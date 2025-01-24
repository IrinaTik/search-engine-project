package searchengine.dto.search;

import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Log4j2
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Snippet {

    public static final String SNIPPET_DELIMITER = "...";
    private static final Integer WORDS_BEFORE_OR_AFTER_SNIPPET = 2;
    private static final int DEFAULT_LETTER_INDEX = -1;

    @Getter
    private String snippet = "";
    private Integer firstLetterIndexInText = DEFAULT_LETTER_INDEX;
    private Integer lastLetterIndexInText = DEFAULT_LETTER_INDEX;
    private Integer lastQueryWordOrdinalNumber = DEFAULT_LETTER_INDEX;
    @Getter
    private Integer queryWordsInSnippetCount = 0;
    private boolean isComplete = false;

    public void updateSnippetInfo(String text, UnalteredWord word, List<UnalteredWord> unalteredWordList) {
        Integer currentQueryWordOrdinalNumber = word.getOrdinalNumberInText();
        int supposedFirstSnippetWordOrdinalNumber =
                Math.max(0, currentQueryWordOrdinalNumber - WORDS_BEFORE_OR_AFTER_SNIPPET);
        int supposedLastSnippetWordOrdinalNumber = this.lastQueryWordOrdinalNumber + WORDS_BEFORE_OR_AFTER_SNIPPET;
        if (isLastQueryWordNear(supposedLastSnippetWordOrdinalNumber, supposedFirstSnippetWordOrdinalNumber) ||
                isNewlyCreated()) {
            setIndexesAndCount(word, unalteredWordList, supposedFirstSnippetWordOrdinalNumber);
        } else {
            completeSnippet(unalteredWordList, text);
        }
    }

    private static boolean isLastQueryWordNear(int supposedLastSnippetWordOrdinalNumber,
                                               int supposedFirstSnippetWordOrdinalNumber) {
        return supposedLastSnippetWordOrdinalNumber >= supposedFirstSnippetWordOrdinalNumber - 1;
    }

    private boolean isNewlyCreated() {
        return this.firstLetterIndexInText == DEFAULT_LETTER_INDEX;
    }

    private void setIndexesAndCount(UnalteredWord word, List<UnalteredWord> unalteredWordList, int firstSnippetWordOrdinalNumber) {
        if (isNewlyCreated()) {
            this.firstLetterIndexInText =
                    unalteredWordList.get(firstSnippetWordOrdinalNumber).getFirstLetterIndexInText();
        }
        incrementQueryWordsCount();
        this.lastLetterIndexInText = word.getLastLetterIndexInText();
        this.lastQueryWordOrdinalNumber = word.getOrdinalNumberInText();
    }

    private void incrementQueryWordsCount() {
        this.queryWordsInSnippetCount += 1;
    }

    public void completeSnippet(List<UnalteredWord> unalteredWordList, String text) {
        int lastWordToAppendOrdinalNumber = this.lastQueryWordOrdinalNumber + WORDS_BEFORE_OR_AFTER_SNIPPET;
        int maxOrdinalNumber = unalteredWordList.size() - 1;
        if (lastWordToAppendOrdinalNumber > maxOrdinalNumber) {
            lastWordToAppendOrdinalNumber = maxOrdinalNumber;
        }
        this.lastLetterIndexInText =
                unalteredWordList.get(lastWordToAppendOrdinalNumber).getLastLetterIndexInText();
        this.snippet = getSnippetFromText(text);
        this.isComplete = true;
    }

    private String getSnippetFromText(String text) {
        try {
            cutSnippetFromText(text);
            addEndingDelimiter();
            return this.snippet;
        } catch (IndexOutOfBoundsException e) {
            log.error("Unable to cut snippet from text within index range : from {} to {}",
                    firstLetterIndexInText, lastLetterIndexInText);
        }
        return StringUtils.EMPTY;
    }

    private void cutSnippetFromText(String text) {
        this.snippet = text.substring(firstLetterIndexInText, lastLetterIndexInText + 1);
    }

    private void addEndingDelimiter() {
        this.snippet = this.snippet + SNIPPET_DELIMITER + " ";
    }

    public boolean isCompleted() {
        return this.isComplete;
    }

}