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
            lemmaEntity.setFrequency(lemmaEntity.getFrequency() + 1);
            return lemmaEntity;
        }
        return createLemmaEntity(lemma, site);
    }

    private LemmaEntity createLemmaEntity(String lemma, SiteEntity site) {
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
