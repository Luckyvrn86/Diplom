package searchengine.dto.search;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class SearchData {
    private final String site;
    private final String siteName;
    private final String uri;
    private final String title;
    private final String snippet;
    private final Float relevance;

}
