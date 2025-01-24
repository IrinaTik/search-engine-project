package searchengine.dto.search;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnalteredWord {

    private String initialWord;
    private String lemma;
    private Integer firstLetterIndexInText;
    private Integer ordinalNumberInText;

    public Integer getLastLetterIndexInText() {
        return this.firstLetterIndexInText + this.initialWord.length() - 1;
    }

}