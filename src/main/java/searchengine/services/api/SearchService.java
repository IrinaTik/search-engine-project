package searchengine.services.api;

import searchengine.dto.search.SearchResponse;

public interface SearchService {
    SearchResponse getSearchResults(String query, String siteUrl, Integer offset, Integer limit);
}
