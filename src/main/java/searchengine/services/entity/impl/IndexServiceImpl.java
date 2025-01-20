package searchengine.services.entity.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.repository.IndexRepository;
import searchengine.services.entity.IndexService;

import java.util.Collection;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class IndexServiceImpl implements IndexService {

    private final IndexRepository indexRepository;

    @Override
    public List<IndexEntity> getByPage(PageEntity page) {
        return indexRepository.findByPage(page);
    }

    @Override
    public List<IndexEntity> getByLemma(LemmaEntity lemma) {
        return indexRepository.findByLemma(lemma);
    }

    @Override
    public IndexEntity getByLemmaAndPage(LemmaEntity lemma, PageEntity page) {
        return indexRepository.findByLemmaAndPage(lemma, page).orElse(null);
    }

    @Override
    public IndexEntity save(IndexEntity index) {
        return indexRepository.saveAndFlush(index);
    }

    @Override
    public List<IndexEntity> saveAll(Collection<IndexEntity> indexes) {
        return indexRepository.saveAllAndFlush(indexes);
    }

    @Override
    public void deleteAll(Iterable<IndexEntity> indexes) {
        indexRepository.deleteAllInBatch(indexes);
    }

    @Override
    public void deleteAll() {
        indexRepository.deleteAllInBatch();
    }

    @Override
    public IndexEntity createIndexForPage(LemmaEntity lemmaEntity, Float rank, PageEntity page) {
        IndexEntity indexEntity = new IndexEntity();
        indexEntity.setPage(page);
        indexEntity.setLemma(lemmaEntity);
        indexEntity.setRank(rank);
        return indexEntity;
    }
}
