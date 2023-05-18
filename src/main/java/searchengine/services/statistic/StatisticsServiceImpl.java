package searchengine.services.statistic;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.List;


@Service
@Getter
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Override
    public StatisticsResponse getStatistics() {
        List<Site> sites = siteRepository.findAll();
        TotalStatistics totalStatistics = new TotalStatistics();
        StatisticsData statisticsData = new StatisticsData();
        StatisticsResponse response = new StatisticsResponse();
        if (sites.isEmpty()){
            statisticsData.setTotal(totalStatistics);
            response.setStatistics(statisticsData);
            return response;
        }
        for (Site site : sites){
            DetailedStatisticsItem detailedStatisticsItem = new DetailedStatisticsItem(site, this);
            statisticsData.addDetailedStatistic(detailedStatisticsItem);
            totalStatistics.setPages(totalStatistics.getPages() + detailedStatisticsItem.getPages());
            totalStatistics.setLemmas(totalStatistics.getLemmas() + detailedStatisticsItem.getLemmas());
            if (site.getStatus().equals(Status.INDEXING)) totalStatistics.setIndexing(true);
        }
        totalStatistics.setSites(sites.size());
        statisticsData.setTotal(totalStatistics);
        response.setStatistics(statisticsData);
        return response;
    }
}














