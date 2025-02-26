package searchengine.services.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import searchengine.exceptions.PageNotFromSiteException;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.PageRepository;
import searchengine.util.UrlFormatter;
import searchengine.services.entity.impl.PageServiceImpl;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class PageServiceTest {

    private final PageRepository pageRepository = Mockito.mock(PageRepository.class);
    private final PageService pageService = new PageServiceImpl(pageRepository);

    @Test
    @DisplayName("Get page by site and relative path with escape end")
    public void testGetByRelativePathWithEscapeEndAndSite() {
        SiteEntity site = new SiteEntity();
        site.setUrl("https://test-site-url");
        PageEntity page = new PageEntity();
        int id = 110;
        page.setId(id);
        String relativePathWithoutEscapeEnd = "/path";
        String relativePathWithEscapeEnd = relativePathWithoutEscapeEnd + "/";
        page.setRelativePath(relativePathWithoutEscapeEnd);
        Mockito.when(pageRepository.findByRelativePathAndSite(relativePathWithoutEscapeEnd, site)).thenReturn(Optional.of(page));
        PageEntity pageFromService = pageService.getByRelativePathAndSite(relativePathWithEscapeEnd, site);
        assertEquals(id, pageFromService.getId());
        assertEquals(relativePathWithoutEscapeEnd, pageFromService.getRelativePath());
        Mockito.verify(pageRepository, Mockito.times(1)).findByRelativePathAndSite(relativePathWithoutEscapeEnd, site);
    }

    @Test
    @DisplayName("Get page by site and relative path without escape end")
    public void testGetByRelativePathWithoutEscapeEndAndSite() {
        SiteEntity site = new SiteEntity();
        site.setUrl("https://test-site-url");
        int id = 110;
        PageEntity page = new PageEntity();
        page.setId(id);
        String relativePathWithoutEscapeEnd = "/path";
        String relativePathWithEscapeEnd = relativePathWithoutEscapeEnd + "/";
        page.setRelativePath(relativePathWithEscapeEnd);
        Mockito.when(pageRepository.findByRelativePathAndSite(relativePathWithoutEscapeEnd, site)).thenReturn(Optional.of(page));
        PageEntity pageFromService = pageService.getByRelativePathAndSite(relativePathWithoutEscapeEnd, site);
        assertEquals(id, pageFromService.getId());
        Mockito.verify(pageRepository, Mockito.times(1)).findByRelativePathAndSite(relativePathWithoutEscapeEnd, site);
    }

    @Test
    @DisplayName("Get page by site and absolute path with escape end")
    public void testGetByAbsPathWithEscapeEndAndSite() {
        SiteEntity site = new SiteEntity();
        site.setUrl("https://test-site-url");
        String absPathWithEscapeEnd = "https://test-site-url/path/";
        PageEntity page = new PageEntity();
        String relativePath = "/path";
        page.setRelativePath(relativePath);
        Mockito.when(pageRepository.findByRelativePathAndSite(relativePath, site)).thenReturn(Optional.of(page));
        PageEntity pageFromServiceWithEscapeEnd = pageService.getByAbsPathAndSite(absPathWithEscapeEnd, site);
        assertEquals(relativePath, pageFromServiceWithEscapeEnd.getRelativePath());
    }

    @Test
    @DisplayName("Get page by site and absolute path without escape end")
    public void testGetByAbsPathWithoutEscapeEndAndSite() {
        SiteEntity site = new SiteEntity();
        site.setUrl("https://test-site-url");
        String absPathWithoutEscapeEnd = "https://test-site-url/path";
        PageEntity page = new PageEntity();
        String relativePath = "/path";
        page.setRelativePath(relativePath);
        Mockito.when(pageRepository.findByRelativePathAndSite(relativePath, site)).thenReturn(Optional.of(page));
        PageEntity pageFromServiceWithoutEscapeEnd = pageService.getByAbsPathAndSite(absPathWithoutEscapeEnd, site);
        assertEquals(relativePath, pageFromServiceWithoutEscapeEnd.getRelativePath());
    }

    @Test
    @DisplayName("Get page by site and absolute path when page is site home page")
    public void testGetByAbsPathAndSiteWhenHomePage() {
        SiteEntity site = new SiteEntity();
        site.setUrl("https://test-site-url");
        int id = 110;
        String absPathWithEscapeEnd = "https://test-site-url/";
        String absPathWithoutEscapeEnd = "https://test-site-url";
        PageEntity page = new PageEntity();
        page.setId(id);
        page.setRelativePath(UrlFormatter.SITE_HOME_PAGE_RELATIVE_PATH);
        Mockito.when(pageRepository.findByRelativePathAndSite(UrlFormatter.SITE_HOME_PAGE_RELATIVE_PATH, site))
                .thenReturn(Optional.of(page));
        PageEntity pageFromServiceWithEscapeEnd = pageService.getByAbsPathAndSite(absPathWithEscapeEnd, site);
        assertEquals(id, pageFromServiceWithEscapeEnd.getId());
        PageEntity pageFromServiceWithoutEscapeEnd = pageService.getByAbsPathAndSite(absPathWithoutEscapeEnd, site);
        assertEquals(id, pageFromServiceWithoutEscapeEnd.getId());
    }

    @Test
    @DisplayName("Get page by site and absolute path when page is not from site")
    public void testGetByAbsPathAndSiteWhenNotFromSite() {
        SiteEntity site = new SiteEntity();
        site.setUrl("https://test-site-url");
        String absPath = "https://another-test-site-url/path/";
        PageEntity page = pageService.getByAbsPathAndSite(absPath, site);
        assertNull(page);
    }

    @Test
    @DisplayName("Save page - page should be saved with no escape end relative path")
    public void testSave() {
        PageEntity page = new PageEntity();
        String relativePath = "/path";
        String relativePathWithEscapeEnd = relativePath + "/";
        page.setRelativePath(relativePathWithEscapeEnd);
        Mockito.when(pageRepository.saveAndFlush(page)).thenReturn(page);
        PageEntity pageFromService = pageService.save(page);
        assertEquals(relativePath, pageFromService.getRelativePath());
        Mockito.verify(pageRepository, Mockito.times(1)).saveAndFlush(page);
    }

    @Test
    @DisplayName("Save site home page")
    public void testSaveHomePage() {
        PageEntity page = new PageEntity();
        String relativePath = UrlFormatter.SITE_HOME_PAGE_RELATIVE_PATH;
        page.setRelativePath(relativePath);
        Mockito.when(pageRepository.saveAndFlush(page)).thenReturn(page);
        PageEntity pageFromService = pageService.save(page);
        assertEquals(relativePath, pageFromService.getRelativePath());
    }

    @Test
    @DisplayName("Create page by site path and page absolute path with escape end")
    public void testCreatePageByAbsPathWithEscapeEndAndSitePath() {
        String siteUrl = "https://test-site-url";
        String absPath = "https://test-site-url/path/";
        testCreatePageByAbsPathAndSitePath(absPath, siteUrl, true);
    }

    @Test
    @DisplayName("Create page by site path and page absolute path without escape end")
    public void testCreatePageByAbsPathWithoutEscapeEndAndSitePath() {
        String siteUrl = "https://test-site-url";
        String absPath = "https://test-site-url/path";
        testCreatePageByAbsPathAndSitePath(absPath, siteUrl, false);
    }

    private void testCreatePageByAbsPathAndSitePath(String absPath, String siteUrl, boolean withEscapeEnd) {
        SiteEntity site = new SiteEntity();
        site.setUrl(siteUrl);
        String relativePath = "/path";
        String relativePathWithEscapeEnd = relativePath + "/";
        String pageContent = "";
        PageEntity pageFromService = pageService.createPageByAbsPathAndSitePath(absPath, site);
        assertEquals(site.getUrl(), pageFromService.getSite().getUrl());
        if (withEscapeEnd) {
            assertEquals(relativePathWithEscapeEnd, pageFromService.getRelativePath());
        } else {
            assertEquals(relativePath, pageFromService.getRelativePath());
        }
        assertEquals(pageContent, pageFromService.getContent());
        assertEquals(Collections.emptySet(), pageFromService.getChildLinks());
        assertNull(pageFromService.getCode());
    }

    @Test
    @DisplayName("Create page by site path and page absolute path when page is not from site")
    public void testCreatePageByAbsPathAndSitePathWhenNotFromSite() {
        String siteUrl = "https://test-site-url";
        SiteEntity site = new SiteEntity();
        site.setUrl(siteUrl);
        String absPath = "https://another-test-site-url/path";
        assertThrows(PageNotFromSiteException.class,
                () -> pageService.createPageByAbsPathAndSitePath(absPath, site));
    }

    @Test
    @DisplayName("Update page parse info when all info is valid")
    public void testUpdateParseInfo() {
        PageEntity page = new PageEntity();
        page.setChildLinks(Collections.emptySet());
        page.setContent("");
        Set<String> childLinks = Set.of("link1", "link2");
        pageService.updateParseInfo(200,  "content", childLinks, page);
        assertEquals(200, page.getCode());
        assertEquals("content", page.getContent());
        assertIterableEquals(childLinks, page.getChildLinks());
    }

    @Test
    @DisplayName("Update page parse info when info is null")
    public void testUpdateParseInfoWithNulls() {
        PageEntity page = new PageEntity();
        page.setCode(200);
        Set<String> childLinks = Set.of("link1", "link2");
        page.setChildLinks(childLinks);
        page.setContent("content");
        pageService.updateParseInfo(null,  "", null, page);
        assertEquals(200, page.getCode());
        assertEquals("content", page.getContent());
        assertIterableEquals(childLinks, page.getChildLinks());
        pageService.updateParseInfo(null,  null, null, page);
        assertEquals("content", page.getContent());
    }

    @Test
    @DisplayName("Determine if site home page is accessible when page code is SUCCESS")
    public void testIsSiteHomePageAccessibleWhenPageCodeIsOK() {
        String siteUrl = "https://test-site-url";
        SiteEntity site = new SiteEntity();
        site.setUrl(siteUrl);
        PageEntity page = new PageEntity();
        page.setCode(200);
        Mockito.when(pageRepository.findByRelativePathAndSite(UrlFormatter.SITE_HOME_PAGE_RELATIVE_PATH, site))
                .thenReturn(Optional.of(page));
        assertTrue(pageService.isSiteHomePageAccessible(site));
    }

    @Test
    @DisplayName("Determine if site home page is accessible when page code is not SUCCESS")
    public void testIsSiteHomePageAccessibleWhenPageCodeIsBad() {
        String siteUrl = "https://test-site-url";
        SiteEntity site = new SiteEntity();
        site.setUrl(siteUrl);
        PageEntity page = new PageEntity();
        page.setCode(404);
        Mockito.when(pageRepository.findByRelativePathAndSite(UrlFormatter.SITE_HOME_PAGE_RELATIVE_PATH, site))
                .thenReturn(Optional.of(page));
        assertFalse(pageService.isSiteHomePageAccessible(site));
    }

    @Test
    @DisplayName("Determine if site home page is accessible when home page was not saved to DB")
    public void testIsSiteHomePageAccessibleWhenNoHomePage() {
        String siteUrl = "https://test-site-url";
        SiteEntity site = new SiteEntity();
        site.setUrl(siteUrl);
        Mockito.when(pageRepository.findByRelativePathAndSite(UrlFormatter.SITE_HOME_PAGE_RELATIVE_PATH, site))
                .thenReturn(Optional.empty());
        assertFalse(pageService.isSiteHomePageAccessible(site));
    }

}
