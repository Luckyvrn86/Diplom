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

import java.time.OffsetDateTime;
import java.util.ArrayList;
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
        TotalStatistics totalStatistics = new TotalStatistics(sites.size(), pageRepository.count(),
                lemmaRepository.count(), true);
        List<DetailedStatisticsItem> detailedStatistic = new ArrayList<>();
        for (Site site : siteRepository.findAll()) {
            DetailedStatisticsItem detailedStatisticsItem = new DetailedStatisticsItem(site, this);
            detailedStatistic.add(detailedStatisticsItem);
        }

        return new StatisticsResponse(true, new StatisticsData(totalStatistics, detailedStatistic));
    }
}














