package searchengine.dto.statistics;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class StatisticsData {
    private TotalStatistics total;
    private List<DetailedStatisticsItem> detailed = new ArrayList<>();

    public StatisticsData(TotalStatistics total, List<DetailedStatisticsItem> detailed) {
        this.total = total;
        this.detailed = detailed;
    }

    public void addDetailedStatistic (DetailedStatisticsItem item) {
        detailed.add(item);
    }
}
