package searchengine.services;

import lombok.Data;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.parsing.MainSite;
import searchengine.services.parsing.SiteGetChild;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

@Service
@Data
public class IndexingServiceImpl implements IndexingService {

    private final SitesList siteList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final List<ForkJoinPool> pools = new ArrayList<>();
    @Override
    public IndexingResponse startIndexing() {
        for (SiteConfig site : siteList.getSites()) {
            dataDelete(siteRepository.findSiteByUrl(site.getUrl()));
            Site newSite = new Site();
            newSite.setName(site.getName());
            newSite.setUrl(site.getUrl());
            newSite.setStatus(Status.INDEXING);
            newSite.setStatusTime(LocalDateTime.now());
            siteRepository.save(newSite);
        }
        for (Site site : siteRepository.findAll()) {
            new Thread(() -> parsingSite(site.getUrl())).start();
        }
        return new IndexingResponse();
    }

    @Override
    public IndexingResponse stopIndexing() {
        List<Site> indexingNow = siteRepository.findAllByStatus(Status.INDEXING);
        pools.forEach(ForkJoinPool::shutdown);
        pools.clear();
        for (Site site : indexingNow){
            site.setLastError("Индексация остановлена пользователем");
            site.setStatusTime(LocalDateTime.now());
            site.setStatus(Status.FAILED);
            siteRepository.save(site);
        }
        return new IndexingResponse();
    }

    private void parsingSite(String url) {
        MainSite mainSite = new MainSite(url, this);
        Site site = siteRepository.findSiteByUrl(url);
        ForkJoinPool pool = new ForkJoinPool();
        pools.add(pool);
        pool.invoke(new SiteGetChild(mainSite, mainSite, site));
        if (!site.getStatus().equals(Status.FAILED)) {
            site.setStatus(Status.INDEXED);
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
            pools.remove(pool);
        }
    }


    public void dataDelete(Site site) {
        if (site != null) siteRepository.delete(site);

        List<Page> pages = pageRepository.findAllBySite(site);
        if (!pages.isEmpty()) pageRepository.deleteAll(pages);
    }
}
