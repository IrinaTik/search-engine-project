package searchengine.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import searchengine.model.SiteEntity;
import searchengine.util.UrlFormatter;

import static org.junit.jupiter.api.Assertions.*;

public class UrlFormatterTest {

    @Test
    @DisplayName("Convert page absolute path to relative path")
    public void testConvertAbsPathToRelativePath() {
        SiteEntity site = new SiteEntity();
        String siteUrl = "https://test-site-url";
        site.setUrl(siteUrl);
        String relativePath = "/path";
        String absPath = siteUrl + relativePath;
        String relativePathFromAbsPath = UrlFormatter.convertAbsPathToRelativePath(absPath, site);
        String relativePathFromHomePage = UrlFormatter.convertAbsPathToRelativePath(siteUrl, site);
        String relativePathFromHomePageWithEscapeEnd =
                UrlFormatter.convertAbsPathToRelativePath(siteUrl + "/", site);
        assertEquals(relativePath, relativePathFromAbsPath);
        assertEquals("/", relativePathFromHomePage);
        assertEquals("/", relativePathFromHomePageWithEscapeEnd);
    }

    @Test
    @DisplayName("Remove escape end from page path")
    public void testRemoveEscapeEnd() {
        String absPathWithoutEscapeEnd = "https://test-site-url/path";
        String absPathWithEscapeEnd = absPathWithoutEscapeEnd + "/";
        String urlFromAbsPathWithoutEscapeEnd = UrlFormatter.removeEscapeEnd(absPathWithoutEscapeEnd);
        String urlFromAbsPathWithEscapeEnd = UrlFormatter.removeEscapeEnd(absPathWithEscapeEnd);
        assertEquals(absPathWithoutEscapeEnd, urlFromAbsPathWithoutEscapeEnd);
        assertEquals(absPathWithoutEscapeEnd, urlFromAbsPathWithEscapeEnd);
        String relativePathWithoutEscapeEnd = "/path";
        String relativePathWithEscapeEnd = relativePathWithoutEscapeEnd + "/";
        String urlFromRelativePathWithoutEscapeEnd = UrlFormatter.removeEscapeEnd(relativePathWithoutEscapeEnd);
        String urlFromRelativePathWithEscapeEnd = UrlFormatter.removeEscapeEnd(relativePathWithEscapeEnd);
        assertEquals(relativePathWithoutEscapeEnd, urlFromRelativePathWithoutEscapeEnd);
        assertEquals(relativePathWithoutEscapeEnd, urlFromRelativePathWithEscapeEnd);
    }

    @Test
    @DisplayName("Determine if url is worth parsing")
    public void testIsGoodLink() {
        testStartWithPageUrl();
        testIsSameLink();
        testEndsWithScrollUpSymbol();
        testStartWithPageUrlAndScrollUpSymbol();
    }

    private void testStartWithPageUrl() {
        String pageUrl = "https://test-site-url/path";
        String pageUrlWithEscapeEnd = "https://test-site-url/path/";
        String goodLink = "https://test-site-url/path/smth";
        String badLink = "https://test-site-url/another-path/smth";
        assertTrue(UrlFormatter.isGoodLink(pageUrl, goodLink));
        assertFalse(UrlFormatter.isGoodLink(pageUrl, badLink));
        assertTrue(UrlFormatter.isGoodLink(pageUrlWithEscapeEnd, goodLink));
        assertFalse(UrlFormatter.isGoodLink(pageUrlWithEscapeEnd, badLink));
    }

    private void testIsSameLink() {
        String pageUrl = "https://test-site-url/path";
        String pageUrlWithEscapeEnd = "https://test-site-url/path/";
        assertFalse(UrlFormatter.isGoodLink(pageUrl, pageUrl));
        assertFalse(UrlFormatter.isGoodLink(pageUrl, pageUrlWithEscapeEnd));
        assertFalse(UrlFormatter.isGoodLink(pageUrlWithEscapeEnd, pageUrl));
        assertFalse(UrlFormatter.isGoodLink(pageUrlWithEscapeEnd, pageUrlWithEscapeEnd));
    }

    private void testEndsWithScrollUpSymbol() {
        String pageUrl = "https://test-site-url/path";
        String pageUrlWithEscapeEnd = "https://test-site-url/path/";
        String linkWithEscape = "https://test-site-url/path#";
        String linkWithoutEscape = "https://test-site-url/path/#";
        assertFalse(UrlFormatter.isGoodLink(pageUrl, linkWithEscape));
        assertFalse(UrlFormatter.isGoodLink(pageUrl, linkWithoutEscape));
        assertFalse(UrlFormatter.isGoodLink(pageUrlWithEscapeEnd, linkWithEscape));
        assertFalse(UrlFormatter.isGoodLink(pageUrlWithEscapeEnd, linkWithoutEscape));
    }

    private void testStartWithPageUrlAndScrollUpSymbol() {
        String pageUrl = "https://test-site-url/path";
        String pageUrlWithEscapeEnd = "https://test-site-url/path/";
        String link = "https://test-site-url/path#smth";
        assertFalse(UrlFormatter.isGoodLink(pageUrl, link));
        assertFalse(UrlFormatter.isGoodLink(pageUrlWithEscapeEnd, link));
    }

    @Test
    @DisplayName("Determine if path is site home page relative path")
    public void testIsHomePageRelativePath() {
        String homePageRelativePath = "/";
        assertTrue(UrlFormatter.isHomePageRelativePath(homePageRelativePath));
    }

    @Test
    @DisplayName("Determine if page is part of site when it in fact is")
    public void testIsPagePartOfSiteWhenItIs() {
        String pageUrl = "https://test-site-url/path";
        String siteUrl = "https://test-site-url";
        assertTrue(UrlFormatter.isPagePartOfSite(siteUrl, pageUrl));
    }

    @Test
    @DisplayName("Determine if page is part of site when it in fact isn't")
    public void testIsPagePartOfSiteWhenItIsNot() {
        String pageUrl = "https://test-site-url-not/path";
        String siteUrl = "https://test-site-url";
        assertFalse(UrlFormatter.isPagePartOfSite(siteUrl, pageUrl));
    }
}
