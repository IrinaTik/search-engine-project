package searchengine.services.entity.impl;


import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.exceptions.PageNotFromSiteException;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.PageRepository;
import searchengine.services.actions.ExtractConnectionInfoAction;
import searchengine.services.actions.FormatUrlAction;
import searchengine.services.entity.PageService;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@Log4j2
@Service
@RequiredArgsConstructor
public class PageServiceImpl implements PageService {

    private final PageRepository pageRepository;

    @Override
    public PageEntity getByRelativePathAndSite(String relativePath, SiteEntity site) {
        if (!FormatUrlAction.isHomePageRelativePath(relativePath)) {
            relativePath = FormatUrlAction.removeEscapeEnd(relativePath);
        }
        return pageRepository.findByRelativePathAndSite(relativePath, site).orElse(null);
    }

    @Override
    public PageEntity getByAbsPathAndSite(String absPath, SiteEntity site) {
        try {
            String relativePath = FormatUrlAction.convertAbsPathToRelativePath(absPath, site);
            return getByRelativePathAndSite(relativePath, site);
        } catch (PageNotFromSiteException ex) {
            log.error("Cannot get page by absolute path", ex);
            return null;
        }
    }

    @Override
    public Integer countBySite(SiteEntity siteEntity) {
        return pageRepository.countBySite(siteEntity);
    }

    @Override
    public Integer countResponsivePagesBySite(SiteEntity site) {
        return pageRepository.countBySiteAndCode(site, ExtractConnectionInfoAction.PAGE_CODE_SUCCESS);
    }

    @Override
    public Long countAll() {
        return pageRepository.count();
    }

    @Override
    public PageEntity save(PageEntity page) {
        if (!FormatUrlAction.isHomePageRelativePath(page.getRelativePath())) {
            String relativePathWithoutEscapeEnd = FormatUrlAction.removeEscapeEnd(page.getRelativePath());
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
        String relativePath = FormatUrlAction.convertAbsPathToRelativePath(path, site);
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
                pageRepository.findByRelativePathAndSite(FormatUrlAction.SITE_HOME_PAGE_RELATIVE_PATH, site);
        return homePageOptional
                .filter(page -> ExtractConnectionInfoAction.isPageCodeSuccessful(page.getCode()))
                .isPresent();
    }

    @Override
    public String getPageTitle(PageEntity page) {
        return Jsoup.parse(page.getContent()).title();
    }

}