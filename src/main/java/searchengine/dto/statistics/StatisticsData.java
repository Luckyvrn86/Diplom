package searchengine.dto.statistics;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class StatisticsData {
    private TotalStatistics total;
    private List<DetailedStatisticsItem> detailed = new ArrayList<>();

    public void addDetailedStatistic (DetailedStatisticsItem item) {
        detailed.add(item);
    }
}
