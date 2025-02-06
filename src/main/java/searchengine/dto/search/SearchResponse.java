package searchengine.dto.search;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.Collections;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder(access = AccessLevel.PRIVATE)
public class SearchResponse {

    private boolean result;
    private String error;
    private Integer count;
    private List<SearchData> data;

    public static SearchResponse buildSearchResponseWithData(List<SearchData> searchDataList, Integer totalDataCount) {
        return SearchResponse.builder()
                .result(true)
                .error("")
                .count(totalDataCount)
                .data(searchDataList)
                .build();
    }

    public static SearchResponse buildSearchResponseWithoutData(String query) {
        return SearchResponse.builder()
                .result(true)
                .error("Nothing was found for query " + query)
                .count(0)
                .data(Collections.emptyList())
                .build();
    }

    public static SearchResponse buildErrorSearchResponse(String error) {
        return SearchResponse.builder()
                .result(false)
                .error(error)
                .build();
    }

}