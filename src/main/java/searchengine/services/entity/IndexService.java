package searchengine.services.entity;

import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;

import java.util.Collection;
import java.util.List;

public interface IndexService {
    List<IndexEntity> getByPage(PageEntity page);

    List<IndexEntity> getByLemma(LemmaEntity lemma);

    IndexEntity getByLemmaAndPage(LemmaEntity lemma, PageEntity page);

    IndexEntity save(IndexEntity index);

    List<IndexEntity> saveAll(Collection<IndexEntity> indexes);

    void deleteAll(Iterable<IndexEntity> indexes);

    void deleteAll();

    IndexEntity createIndexForPage(LemmaEntity lemmaEntity, Float rank, PageEntity page);
}
