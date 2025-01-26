package searchengine.services.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.repository.IndexRepository;
import searchengine.services.entity.impl.IndexServiceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IndexServiceTest {

    private final IndexRepository indexRepository = Mockito.mock(IndexRepository.class);
    private final IndexService indexService = new IndexServiceImpl(indexRepository);

    @Test
    @DisplayName("Create index for page")
    public void testCreateIndexForPage() {
        Float rank = 3.0f;
        String path = "/path";
        PageEntity page = new PageEntity();
        page.setRelativePath(path);
        String lemma = "lemma";
        LemmaEntity lemmaEntity = new LemmaEntity();
        lemmaEntity.setLemma(lemma);
        IndexEntity indexEntity = indexService.createIndexForPage(lemmaEntity, rank, page);
        assertEquals(lemma, indexEntity.getLemma().getLemma());
        assertEquals(path, indexEntity.getPage().getRelativePath());
        assertEquals(rank, indexEntity.getRank());
    }
}
