package searchengine.services.entity.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.LemmaRepository;
import searchengine.services.entity.LemmaService;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
@RequiredArgsConstructor
public class LemmaServiceImpl implements LemmaService {

    private final LemmaRepository lemmaRepository;

    @Override
    public Integer countBySite(SiteEntity siteEntity) {
        return lemmaRepository.countBySite(siteEntity);
    }

    @Override
    public Long countAll() {
        return lemmaRepository.count();
    }

    @Override
    public LemmaEntity getBySiteAndLemma(SiteEntity site, String lemma) {
        return lemmaRepository.findBySiteAndLemma(site, lemma).orElse(null);
    }

    @Override
    public LemmaEntity save(LemmaEntity lemma) {
        return lemmaRepository.saveAndFlush(lemma);
    }

    @Override
    public List<LemmaEntity> saveAll(Collection<LemmaEntity> lemmas) {
        return lemmaRepository.saveAllAndFlush(lemmas);
    }

    @Override
    public void delete(LemmaEntity lemmaEntity) {
        lemmaRepository.delete(lemmaEntity);
    }

    @Override
    public void deleteAll() {
        lemmaRepository.deleteAllInBatch();
    }

    @Override
    public LemmaEntity correctLemmaFrequencyBySite(String lemma, SiteEntity site) {
        Optional<LemmaEntity> lemmaBySiteOptional = lemmaRepository.findBySiteAndLemma(site, lemma);
        if (lemmaBySiteOptional.isPresent()) {
            LemmaEntity lemmaEntity = lemmaBySiteOptional.get();
            Integer oldFrequency = lemmaEntity.getFrequency();
            lemmaEntity.setFrequency(oldFrequency + 1);
            log.debug("Lemma '{}' is present in DB, frequency was {} now {}",
                    lemma, oldFrequency, lemmaEntity.getFrequency());
            return lemmaEntity;
        }
        return createLemmaEntity(lemma, site);
    }

    private LemmaEntity createLemmaEntity(String lemma, SiteEntity site) {
        log.debug("Creating new lemma '{}' for site {}", lemma, site.getUrl());
        LemmaEntity lemmaEntity = new LemmaEntity();
        lemmaEntity.setLemma(lemma);
        lemmaEntity.setSite(site);
        lemmaEntity.setFrequency(1);
        return lemmaEntity;
    }

    @Override
    public void decreaseLemmaFrequencyInDatabase(LemmaEntity lemmaEntity) {
        Integer lemmaFrequency = lemmaEntity.getFrequency();
        if (lemmaFrequency == 1) {
            delete(lemmaEntity);
            log.info("Lemma {} deleted", lemmaEntity.getLemma());
        } else {
            lemmaEntity.setFrequency(lemmaFrequency - 1);
            save(lemmaEntity);
            log.info("Lemma {} frequency was {} now {}",
                    lemmaEntity.getLemma(), lemmaFrequency, lemmaEntity.getFrequency());
        }
    }

}
