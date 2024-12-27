package searchengine.services.entity;

import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;

import java.util.Collection;
import java.util.List;

public interface LemmaService {

    Integer countBySite(SiteEntity siteEntity);

    Long countAll();

    LemmaEntity save(LemmaEntity lemma);

    List<LemmaEntity> saveAll(Collection<LemmaEntity> lemmas);

    void delete(LemmaEntity lemmaEntity);

    void deleteAll();

    LemmaEntity correctLemmaFrequencyBySite(String lemma, SiteEntity site);

    void decreaseLemmaFrequencyInDatabase(LemmaEntity lemmaEntity);
}
