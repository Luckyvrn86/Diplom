package searchengine.services.search;

import searchengine.dto.search.SearchResponse;

import java.io.IOException;

public interface SearchService {
    SearchResponse search(String query, String urlSite, int offset, int limit);
}
