package searchengine.services.actions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GenerateSnippetActionTest {

    private final CollectLemmasAction collectLemmasAction = new CollectLemmasAction();
    private final GenerateSnippetAction snippetAction = new GenerateSnippetAction(collectLemmasAction);

    @Test
    @DisplayName("Create snippet")
    public void testCreateSnippet() {
        String expected = "Домашняя <b>лошадь</b> - одомашненный потомок <b>дикой</b> <b>лошади</b>. Используется " +
                "человеком... времени. Наука о <b>лошадях</b> — иппология. В Европе <b>дикие</b> или одичавшие " +
                "<b>лошади</b> — тарпаны — водились... табун одичавших <b>лошадей</b> обитает в Ростовском... ";
        String pageContent = """
                Домашняя лошадь - одомашненный потомок дикой лошади.
                Используется человеком вплоть до настоящего времени. Наука о лошадях — иппология.
                В Европе дикие или одичавшие лошади — тарпаны — водились ещё в первой половине
                прошлого столетия. В России табун одичавших лошадей обитает в Ростовском заповеднике.""";
        Set<String> queryLemmas = new HashSet<>();
        String lemma1 = "лошадь";
        String lemma2 = "дикий";
        queryLemmas.add(lemma1);
        queryLemmas.add(lemma2);
        String snippet = getSnippet(pageContent, queryLemmas);
        assertEquals(expected, snippet);
    }

    private String getSnippet(String pageContent, Set<String> queryLemmas) {
        SiteEntity site = new SiteEntity();
        site.setUrl("https://test-site-url");
        PageEntity page = new PageEntity();
        page.setContent(pageContent);
        page.setRelativePath("/path");
        page.setSite(site);
        return snippetAction.createSnippet(page, queryLemmas);
    }

    @Test
    @DisplayName("Cut one big snippet (by space)")
    public void testCutBigSnippetBySpace() {
        String expected = "<b>Расписание</b> занятий - <b>Расписание</b> занятий - <b>Расписание</b> занятий - " +
                "<b>Расписание</b> занятий <b>Расписание</b> занятий - <b>Расписание</b> занятий - <b>Расписание</b> " +
                "занятий - <b>Расписание</b> занятий памc <b>Расписание</b> занятий - <b>Расписание</b> занятий - " +
                "<b>Расписание</b> занятий - <b>Расписание</b>...";
        String pageContent = """
                Расписание занятий - Расписание занятий - Расписание занятий - Расписание занятий
                Расписание занятий - Расписание занятий - Расписание занятий - Расписание занятий памc
                Расписание занятий - Расписание занятий - Расписание занятий - Расписание занятий
                Расписание занятий - Расписание занятий - Расписание занятий - Расписание занятий""";
        Set<String> queryLemmas = new HashSet<>();
        String lemma1 = "расписание";
        queryLemmas.add(lemma1);
        String snippet = getSnippet(pageContent, queryLemmas);
        assertEquals(expected, snippet);
    }

    @Test
    @DisplayName("Cut several snippets (by delimiter)")
    public void testCutBigSnippetByDelimiter() {
        String expected = "<b>Расписание</b> занятий - <b>Расписание</b> занятий - <b>Расписание</b> занятий - " +
                "<b>Расписание</b> занятий <b>Расписание</b> занятий - <b>Расписание</b> занятий - <b>Расписание</b> " +
                "занятий - <b>Расписание</b> занятий...";
        String pageContent = """
                Расписание занятий - Расписание занятий - Расписание занятий - Расписание занятий
                Расписание занятий - Расписание занятий - Расписание занятий - Расписание занятий...
                Расписание занятий - Расписание занятий - Расписание занятий - Расписание занятий
                Расписание занятий - Расписание занятий - Расписание занятий - Расписание занятий""";
        Set<String> queryLemmas = new HashSet<>();
        String lemma1 = "расписание";
        queryLemmas.add(lemma1);
        String snippet = getSnippet(pageContent, queryLemmas);
        assertEquals(expected, snippet);
    }
}
