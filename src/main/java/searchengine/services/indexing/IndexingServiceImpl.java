package searchengine.services.indexing;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.parsing.SiteParser;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;


@Service
@RequiredArgsConstructor
@Data
public class IndexingServiceImpl implements IndexingService {
    private final SitesList siteList;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final List<ForkJoinPool> poolList = new ArrayList<>();


    @Override
    public IndexingResponse startIndexing() {
        for (SiteConfig site : siteList.getSites()) {
            Site check = siteRepository.findSiteByUrl(site.getUrl());
            if (check != null && check.getStatus().equals(Status.INDEXING))
                return new IndexingResponse(false, "Индексация уже запущена");
            dataDelete();
            Site newSite = siteCreate(site);
            siteRepository.save(newSite);
        }

        for (Site site : siteRepository.findAll()) {
            runParser(site);
        }
        return new IndexingResponse(true, "");
    }

    public void dataDelete() {
        pageRepository.deleteAllInBatch();
        indexRepository.deleteAllInBatch();
        lemmaRepository.deleteAllInBatch();
        siteRepository.deleteAllInBatch();
    }

    @Override
    public IndexingResponse stopIndexing() {
        List<Site> indexingNow = siteRepository.findAllByStatus(Status.INDEXING);
        if (indexingNow.isEmpty()) return new IndexingResponse(false, "Индексация не запущена");
        siteRepository.findAllByStatus(Status.INDEXING).forEach(site -> {
            site.setLastError("Индексация остановлена пользователем");
            site.setStatus(Status.FAILED);
            siteRepository.save(site);
        });

        return new IndexingResponse(true, "");
    }

    @Override
    public IndexingResponse indexPage(String url) {
        SiteConfig siteConfig = new SiteConfig("", "");
        for (SiteConfig site : siteList.getSites()){
            if (url.startsWith(site.getUrl())) {
                siteConfig.setUrl(url);
                siteConfig.setName(url);
            }
        }
        if (siteConfig.getUrl().equals("")) return new IndexingResponse(false, "Данная страница находится за " +
                "пределами сайтов, указанных в конфигурационном файле");
        new Thread(() -> runParser(url)).start();
        return new IndexingResponse(true, "");
    }

    private void runParser(Site site) {
        ForkJoinPool pool = new ForkJoinPool(4);
        poolList.add(pool);
        SiteParser siteParser = new SiteParser(site, this);
        pool.invoke(siteParser);
        site = siteRepository.findSiteByUrl(site.getUrl());
        if (site.getStatus().equals(Status.INDEXING)) {
            site.setStatusTime(LocalDateTime.now());
            site.setStatus(Status.INDEXED);
            siteRepository.save(site);
            poolList.remove(pool);
        }
    }
    private void runParser(String url){
        ForkJoinPool pool = new ForkJoinPool(4);
        poolList.add(pool);
        SiteParser siteParser = new SiteParser(url, this);
        pool.invoke(siteParser);
        poolList.remove(pool);
    }
    private Site siteCreate (SiteConfig siteConfig){
        Site newSite = new Site();
        newSite.setName(siteConfig.getName());
        newSite.setUrl(siteConfig.getUrl());
        newSite.setStatus(Status.INDEXING);
        newSite.setStatusTime(LocalDateTime.now());
        return newSite;
    }
}