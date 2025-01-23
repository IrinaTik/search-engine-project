package searchengine.dto.search;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class SearchData {

    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private Float relevance;

}
