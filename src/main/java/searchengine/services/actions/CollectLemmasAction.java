package searchengine.services.actions;

import lombok.extern.log4j.Log4j2;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
@Service
public class CollectLemmasAction {

    private static final String[] PARTICLES_NAMES = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ", "МС"};

    private static LuceneMorphology luceneMorph;

    static {
        try {
            luceneMorph = new RussianLuceneMorphology();
        } catch (IOException e) {
            log.error(e);
        }
    }

    public static String cleanText (PageEntity page) {
        return Jsoup.clean(page.getContent(), Safelist.simpleText());
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
                .replaceAll("([^а-я\\s])", " ")
                .split("\\s+");
    }

    private static boolean isWord(String word) {
        if (!word.isEmpty() && word.matches("[а-яё]+")) {
            return !isAnyWordBaseParticle(word);
        }
        return false;
    }

    private static String getNormalFormOfWord(String word) {
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
