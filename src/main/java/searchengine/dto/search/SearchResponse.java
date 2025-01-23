package searchengine.dto.search;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

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

    public static SearchResponse buildSearchResponseWithData(List<SearchData> searchDataList) {
        return SearchResponse.builder()
                .result(true)
                .error("")
                .count(searchDataList.size())
                .data(searchDataList)
                .build();
    }

    public static SearchResponse buildSearchResponseWithoutData(String query) {
        return SearchResponse.builder()
                .result(false)
                .error("Nothing was found for query " + query)
                .build();
    }

}