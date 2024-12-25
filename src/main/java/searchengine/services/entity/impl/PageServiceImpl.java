package searchengine.services.entity.impl;


import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import searchengine.exceptions.PageNotFromSiteException;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.PageRepository;
import searchengine.services.entity.PageService;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static searchengine.services.actions.ExtractConnectionInfoAction.isPageCodeSuccessful;
import static searchengine.services.actions.FormatUrlAction.*;

@Log4j2
@Service
@RequiredArgsConstructor
public class PageServiceImpl implements PageService {

    private final PageRepository pageRepository;

    @Override
    public PageEntity getByRelativePathAndSite(String relativePath, SiteEntity site) {
        if (!isHomePageRelativePath(relativePath)) {
            relativePath = removeEscapeEnd(relativePath);
        }
        return pageRepository.findByRelativePathAndSite(relativePath, site).orElse(null);
    }

    @Override
    public PageEntity getByAbsPathAndSite(String absPath, SiteEntity site) {
        try {
            String relativePath = convertAbsPathToRelativePath(absPath, site);
            return getByRelativePathAndSite(relativePath, site);
        } catch (PageNotFromSiteException ex) {
            log.error("Cannot get page by absolute path", ex);
            return null;
        }
    }

    @Override
    public PageEntity save(PageEntity page) {
        if (!isHomePageRelativePath(page.getRelativePath())) {
            String relativePathWithoutEscapeEnd = removeEscapeEnd(page.getRelativePath());
            page.setRelativePath(relativePathWithoutEscapeEnd);
        }
        return pageRepository.saveAndFlush(page);
    }

    @Override
    public void delete(PageEntity page) {
        pageRepository.delete(page);
    }

    @Override
    public void deleteAll() {
        pageRepository.deleteAllInBatch();
    }

    @Override
    public PageEntity createPageByAbsPathAndSitePath(String path, SiteEntity site) {
        String relativePath = convertAbsPathToRelativePath(path, site);
        PageEntity page = createPageByRelativePath(relativePath);
        page.setSite(site);
        return page;
    }

    private PageEntity createPageByRelativePath(String path) {
        PageEntity page = new PageEntity();
        page.setRelativePath(path);
        page.setContent("");
        page.setChildLinks(Collections.emptySet());
        return page;
    }

    @Override
    public void updateParseInfo(@Nullable Integer statusCode,
                                @Nullable String content,
                                @Nullable Set<String> pageChildLinks,
                                PageEntity page) {
        if (statusCode != null) {
            page.setCode(statusCode);
        }
        if (content != null && !content.isEmpty()) {
            page.setContent(content);
        }
        if (pageChildLinks != null && !pageChildLinks.isEmpty()) {
            page.setChildLinks(pageChildLinks);
        }
    }

    @Override
    public boolean isSiteHomePageAccessible(SiteEntity site) {
        Optional<PageEntity> homePageOptional =
                pageRepository.findByRelativePathAndSite(SITE_HOME_PAGE_RELATIVE_PATH, site);
        return homePageOptional.filter(page -> isPageCodeSuccessful(page.getCode())).isPresent();
    }

}