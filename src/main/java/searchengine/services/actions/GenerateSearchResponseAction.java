package searchengine.services.actions;

import lombok.extern.log4j.Log4j2;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;

import java.util.List;

@Log4j2
public class GenerateSearchResponseAction {

    private static final String EMPTY_QUERY_ERROR = "Задан пустой поисковый запрос";
    private static final String SITE_IS_NOT_INDEXED_ERROR = "Индексация не проведена";

    private static final SearchResponse EMPTY_QUERY_RESPONSE =
            new SearchResponse(false, EMPTY_QUERY_ERROR, 0, null);
    private static final SearchResponse SITE_IS_NOT_INDEXED_RESPONSE =
            new SearchResponse(false, SITE_IS_NOT_INDEXED_ERROR, 0, null);

    public static SearchResponse getEmptyQueryResponse() {
        log.error("Empty search query was fired");
        return EMPTY_QUERY_RESPONSE;
    }

    public static SearchResponse getSiteIsNotIndexedResponse(String siteUrl) {
        log.error("Site {} was not indexed or was indexed with status FAILED", siteUrl);
        return SITE_IS_NOT_INDEXED_RESPONSE;
    }

    public static SearchResponse getAllGoodResponse(List<SearchData> searchDataList, String query) {
        if (searchDataList == null || searchDataList.isEmpty()) {
            return SearchResponse.buildSearchResponseWithoutData(query);
        }
        return SearchResponse.buildSearchResponseWithData(searchDataList);
    }

}
