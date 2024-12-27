package searchengine.services.entity;

import jakarta.annotation.Nullable;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.List;
import java.util.Set;

public interface PageService {

    PageEntity getByRelativePathAndSite(String relativePath, SiteEntity site);

    PageEntity getByAbsPathAndSite(String absPath, SiteEntity site);

    Integer countBySite(SiteEntity siteEntity);

    Long countAll();

    PageEntity save(PageEntity page);

    void delete(PageEntity page);

    void deleteAll();

    PageEntity createPageByAbsPathAndSitePath(String path, SiteEntity site);

    void updateParseInfo(@Nullable Integer statusCode,
                         @Nullable String content,
                         @Nullable Set<String> pageChildLinks,
                         PageEntity page);

    boolean isSiteHomePageAccessible(SiteEntity site);
}
