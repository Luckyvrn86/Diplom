package searchengine.dto.statistics;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class TotalStatistics {
    private int sites;
    private long pages;
    private long lemmas;
    private boolean indexing = false;

    public TotalStatistics(int sites, long pages, long lemmas, boolean indexing) {
        this.sites = sites;
        this.pages = pages;
        this.lemmas = lemmas;
        this.indexing = indexing;
    }
}
