package searchengine.services.entity.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.LemmaRepository;
import searchengine.services.entity.LemmaService;

import java.util.*;

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

}
