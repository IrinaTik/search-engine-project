package searchengine.services.actions;

import lombok.extern.log4j.Log4j2;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@Service
public class CollectLemmasAction {

    private static final String[] PARTICLES_NAMES = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ", "МС"};
    private static final String NOT_LOWCASE_RUSSIAN_LETTERS_OR_SPACE = "([^а-яъ\\p{Z}])";
    private static final String TWO_OR_MORE_LOWCASE_RUSSIAN_LETTERS = "[а-яъ]{2,}";
    private static final String ONE_OR_MORE_SPACE_SYMBOLS = "\\p{Z}+";
    private static final String DOUBLE_STANDALONE_CONSONANTS = "([б-джзк-н-п-т-х-ь])\\1+[\\p{Z}\\p{Punct}]";

    private static LuceneMorphology luceneMorph;

    static {
        try {
            luceneMorph = new RussianLuceneMorphology();
        } catch (IOException e) {
            log.error(e);
        }
    }

    public static String cleanText (String text) {
        return Jsoup.parse(text).text().replaceAll("ё", "е");
    }

    public static Map<String, Integer> collectLemmasFromCleanedTextWithCount(String text) {
        String[] words = getRussianWordsFromCleanedText(text);
        return Arrays.stream(words)
                .filter(CollectLemmasAction::isWord)
                .collect(Collectors.toMap(
                        word -> getNormalFormOfWord(word),
                        count -> 1,
                        Integer::sum));
    }

    public static String[] getRussianWordsFromCleanedText(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll(NOT_LOWCASE_RUSSIAN_LETTERS_OR_SPACE, " ")
                .replaceAll(DOUBLE_STANDALONE_CONSONANTS, " ")
                .split(ONE_OR_MORE_SPACE_SYMBOLS);
    }

    public static boolean isWord(String word) {
        if (!word.isEmpty() && word.matches(TWO_OR_MORE_LOWCASE_RUSSIAN_LETTERS)) {
            return !isAnyWordBaseParticle(word);
        }
        return false;
    }

    public static String getNormalFormOfWord(String word) {
        return luceneMorph.getNormalForms(word).get(0);
    }

    private static boolean isAnyWordBaseParticle(String word) {
        return luceneMorph.getMorphInfo(word).stream().anyMatch(CollectLemmasAction::isParticle);
    }

    private static boolean isParticle(String form) {
        for (String particle : PARTICLES_NAMES) {
            if (form.contains(particle)) {
                return true;
            }
        }
        return false;
    }

}
