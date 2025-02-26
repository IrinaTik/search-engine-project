package searchengine.services.actions;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import searchengine.dto.search.Snippet;
import searchengine.dto.search.UnalteredWord;
import searchengine.model.PageEntity;

import java.util.*;

@Log4j2
@Service
@RequiredArgsConstructor
public class GenerateSnippetAction {

    private static final Integer MAX_SNIPPET_LENGTH_IN_SYMBOLS = 250;

    private final CollectLemmasAction collectLemmasAction;

    public String createSnippet(PageEntity page, Set<String> queryLemmas) {
        String cleanedPageContent = collectLemmasAction.cleanText(page.getContent());
        List<String> wordsFromText = getMeaningfulWordsFromCleanedText(cleanedPageContent);
        List<UnalteredWord> unalteredWordList = cutUnalteredWordsFromText(page, wordsFromText, cleanedPageContent);
        List<UnalteredWord> wordsContainingQuery =
                getQueryWordsSortedByOrdinalNumberInText(queryLemmas, unalteredWordList);
        Map<Integer, String> snippetsWithQueryWordsCount =
                getSnippetsByQueryWordsCountDownward(wordsContainingQuery, cleanedPageContent, unalteredWordList);
        String finalSnippet = constructFinalSnippet(snippetsWithQueryWordsCount);
        return getSnippetWithQueryWordsInBold(finalSnippet, wordsContainingQuery);
    }

    private List<String> getMeaningfulWordsFromCleanedText(String cleanedPageContent) {
        String[] wordsFromText = collectLemmasAction.getRussianWordsFromCleanedText(cleanedPageContent);
        return Arrays.stream(wordsFromText).filter(collectLemmasAction::isWord).toList();
    }

    private List<UnalteredWord> cutUnalteredWordsFromText(PageEntity page,
                                                          List<String> wordsFromText,
                                                          String cleanedPageContent) {
        List<UnalteredWord> unalteredWordList = new LinkedList<>();
        int currentPosition = 0;
        int positionForWordEnd;
        for (int i = 0; i < wordsFromText.size(); i++) {
            String initialWordInLowerCase = wordsFromText.get(i);
            int indexInText = StringUtils.indexOfIgnoreCase(cleanedPageContent, initialWordInLowerCase, currentPosition);
            try {
                positionForWordEnd = indexInText + initialWordInLowerCase.length();
                String initialWord = cleanedPageContent.substring(indexInText, positionForWordEnd);
                UnalteredWord word = UnalteredWord.builder()
                        .initialWord(initialWord)
                        .lemma(getLemmaFromWord(initialWordInLowerCase))
                        .firstLetterIndexInText(indexInText)
                        .ordinalNumberInText(i)
                        .build();
                currentPosition = positionForWordEnd;
                unalteredWordList.add(word);
            } catch (StringIndexOutOfBoundsException e) {
                log.error("Unable to cut word '{}' on page {} after cleaning while creating snippet",
                        initialWordInLowerCase, page.getSite().getUrl() + page.getRelativePath());
            }
        }
        return unalteredWordList;
    }

    private List<UnalteredWord> getQueryWordsSortedByOrdinalNumberInText(Set<String> queryLemmas,
                                                                         List<UnalteredWord> unalteredWordList) {
        List<UnalteredWord> wordsContainingQuery = new LinkedList<>();
        for (String lemma : queryLemmas) {
            for (UnalteredWord wordFromText : unalteredWordList) {
                if (StringUtils.equalsIgnoreCase(lemma, wordFromText.getLemma())) {
                    wordsContainingQuery.add(wordFromText);
                }
            }
        }
        wordsContainingQuery.sort(Comparator.comparingInt(UnalteredWord::getOrdinalNumberInText));
        return wordsContainingQuery;
    }

    private Map<Integer, String> getSnippetsByQueryWordsCountDownward(List<UnalteredWord> wordsContainingQuery,
                                                                      String cleanedPageContent,
                                                                      List<UnalteredWord> unalteredWordList) {
        Map<Integer, String> snippetsWithQueryWordsCount = new TreeMap<>(Comparator.reverseOrder());
        Snippet snippet = new Snippet();
        for (UnalteredWord word : wordsContainingQuery) {
            snippet.updateSnippetInfo(cleanedPageContent, word, unalteredWordList);
            if (snippet.isCompleted()) {
                addSnippetTextToMap(snippet, snippetsWithQueryWordsCount);
                snippet = new Snippet();
                snippet.updateSnippetInfo(cleanedPageContent, word, unalteredWordList);
            }
        }
        if (!snippet.isCompleted()) {
            snippet.completeSnippet(unalteredWordList, cleanedPageContent);
            addSnippetTextToMap(snippet, snippetsWithQueryWordsCount);
        }
        return snippetsWithQueryWordsCount;
    }

    private String constructFinalSnippet(Map<Integer, String> snippetsWithQueryWordsCount) {
        StringBuilder finalSnippet = new StringBuilder();
        for (Integer queryWordsCount : snippetsWithQueryWordsCount.keySet()) {
            String snippetToAppend = snippetsWithQueryWordsCount.get(queryWordsCount);
            finalSnippet.append(snippetToAppend);
            if (finalSnippet.length() > MAX_SNIPPET_LENGTH_IN_SYMBOLS) {
                int firstDelimiterNearMaxLength = getFirstDelimiterNearMaxLength(finalSnippet.toString());
                finalSnippet.replace(
                        Math.min(firstDelimiterNearMaxLength + Snippet.SNIPPET_DELIMITER.length(), finalSnippet.length()),
                        finalSnippet.length(),
                        "");
                break;
            }
        }
        return finalSnippet.toString();
    }

    private int getFirstDelimiterNearMaxLength(String snippet) {
        int lastDelimiterIndex = snippet
                .substring(0, MAX_SNIPPET_LENGTH_IN_SYMBOLS)
                .lastIndexOf(Snippet.SNIPPET_DELIMITER);
        if (lastDelimiterIndex == -1) {
            return Math.max(0, snippet.indexOf(Snippet.SNIPPET_DELIMITER, MAX_SNIPPET_LENGTH_IN_SYMBOLS));
        }
        return lastDelimiterIndex;
    }

    private String getSnippetWithQueryWordsInBold(String snippetText, List<UnalteredWord> wordsContainingQuery) {
        for (UnalteredWord word : wordsContainingQuery) {
            String wordInBold = "<b>" + word.getInitialWord() + "</b>";
            snippetText = snippetText.replaceAll("(?<!<b>)(" + word.getInitialWord() + ")(?!\\p{L})", wordInBold);
        }
        return snippetText;
    }

    private String getLemmaFromWord(String word) {
        if (collectLemmasAction.isWord(word)) {
            return collectLemmasAction.getNormalFormOfWord(word);
        }
        return word;
    }

    private void addSnippetTextToMap(Snippet snippet, Map<Integer, String> snippetsWithQueryWordsCount) {
        String newSnippetText = snippet.getSnippet();
        Integer queryWordsInSnippetCount = snippet.getQueryWordsInSnippetCount();
        snippetsWithQueryWordsCount.merge(queryWordsInSnippetCount, newSnippetText, String::concat);
    }

}