package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {

    List<IndexEntity> findByPage(PageEntity page);

    List<IndexEntity> findByLemma(LemmaEntity lemma);

    Optional<IndexEntity> findByLemmaAndPage(LemmaEntity lemma, PageEntity page);
}
