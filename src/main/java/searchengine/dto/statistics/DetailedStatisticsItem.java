package searchengine.dto.statistics;

import lombok.Data;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.services.statistic.StatisticsServiceImpl;

import java.time.LocalDateTime;

@Data
public class DetailedStatisticsItem {
    private String url;
    private String name;
    private Status status;
    private LocalDateTime statusTime;
    private String error;
    private int pages;
    private int lemmas;

    public DetailedStatisticsItem(Site site, StatisticsServiceImpl statisticsService) {
        this.url = site.getUrl();
        this.name = site.getName();
        this.status = site.getStatus();
        this.statusTime = site.getStatusTime();
        this.error = site.getLastError() == null ? "" : site.getLastError();
        this.pages = statisticsService.getPageRepository().findAllBySite(site).size();
        this.lemmas = statisticsService.getLemmaRepository().findAllBySite(site).size();
    }
}

