package searchengine.dto.indexing;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IndexingResponse {
    private boolean result;
    private String error;

    public static IndexingResponse buildGoodIndexingResponse() {
        return new IndexingResponse(true, null);
    }

    public static IndexingResponse buildErrorIndexingResponse(String error) {
        return new IndexingResponse(false, error);
    }
}