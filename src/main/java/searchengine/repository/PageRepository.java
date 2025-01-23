package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {

    Optional<PageEntity> findByRelativePathAndSite(String relativePath, SiteEntity site);

    Integer countBySite(SiteEntity siteEntity);

}
