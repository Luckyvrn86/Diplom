package searchengine.services.indexing;

import lombok.Data;
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
import lombok.RequiredArgsConstructor;


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
            dataDelete(siteRepository.findSiteByUrl(site.getUrl()));
            Site newSite = new Site();
            newSite.setName(site.getName());
            newSite.setUrl(site.getUrl());
            newSite.setStatus(Status.INDEXING);
            newSite.setStatusTime(LocalDateTime.now());
            siteRepository.save(newSite);
        }

        for (Site site : siteRepository.findAll()) {
            runParser(site);
        }
        return new IndexingResponse(true, "");
    }

    public void dataDelete(Site site) {
        if (site != null) {
            indexRepository.deleteAllInBatch();
            lemmaRepository.deleteAllInBatch();
            pageRepository.deleteAllInBatch();
            siteRepository.deleteAllInBatch();
        }
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
    public IndexingResponse indexPage (String url){
        SiteConfig siteConfig = siteList.getSites().stream()
                .filter(site -> site.getUrl().equals(url))
                .findFirst()
                .orElse(null);
        if (siteConfig == null) return new IndexingResponse(false, "Данная страница находится за " +
                "пределами сайтов, указанных в конфигурационном файле");
        new Thread(() -> runParser(siteRepository.findSiteByUrl(siteConfig.getUrl()))).start();
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
}