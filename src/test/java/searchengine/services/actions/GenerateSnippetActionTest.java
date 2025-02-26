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
        SiteEntity site = new SiteEntity();
        site.setUrl("https://test-site-url");
        PageEntity page = new PageEntity();
        page.setContent(pageContent);
        page.setRelativePath("/path");
        page.setSite(site);
        String snippet = snippetAction.createSnippet(page, queryLemmas);
        assertEquals(expected, snippet);
    }
}
