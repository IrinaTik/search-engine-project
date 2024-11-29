package searchengine.services.actions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import searchengine.model.SiteEntity;

import static org.junit.jupiter.api.Assertions.*;

public class FormatUrlActionTest {

    @Test
    @DisplayName("Convert page absolute path to relative path")
    public void testConvertAbsPathToRelativePath() {
        SiteEntity site = new SiteEntity();
        String siteUrl = "https://test-site-url";
        site.setUrl(siteUrl);
        String relativePath = "/path";
        String absPath = siteUrl + relativePath;
        String relativePathFromAbsPath = FormatUrlAction.convertAbsPathToRelativePath(absPath, site);
        String relativePathFromHomePage = FormatUrlAction.convertAbsPathToRelativePath(siteUrl, site);
        String relativePathFromHomePageWithEscapeEnd =
                FormatUrlAction.convertAbsPathToRelativePath(siteUrl + "/", site);
        assertEquals(relativePath, relativePathFromAbsPath);
        assertEquals("/", relativePathFromHomePage);
        assertEquals("/", relativePathFromHomePageWithEscapeEnd);
    }

    @Test
    @DisplayName("Remove escape end from page path")
    public void testRemoveEscapeEnd() {
        String absPathWithoutEscapeEnd = "https://test-site-url/path";
        String absPathWithEscapeEnd = absPathWithoutEscapeEnd + "/";
        String urlFromAbsPathWithoutEscapeEnd = FormatUrlAction.removeEscapeEnd(absPathWithoutEscapeEnd);
        String urlFromAbsPathWithEscapeEnd = FormatUrlAction.removeEscapeEnd(absPathWithEscapeEnd);
        assertEquals(absPathWithoutEscapeEnd, urlFromAbsPathWithoutEscapeEnd);
        assertEquals(absPathWithoutEscapeEnd, urlFromAbsPathWithEscapeEnd);
        String relativePathWithoutEscapeEnd = "/path";
        String relativePathWithEscapeEnd = relativePathWithoutEscapeEnd + "/";
        String urlFromRelativePathWithoutEscapeEnd = FormatUrlAction.removeEscapeEnd(relativePathWithoutEscapeEnd);
        String urlFromRelativePathWithEscapeEnd = FormatUrlAction.removeEscapeEnd(relativePathWithEscapeEnd);
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
        assertTrue(FormatUrlAction.isGoodLink(pageUrl, goodLink));
        assertFalse(FormatUrlAction.isGoodLink(pageUrl, badLink));
        assertTrue(FormatUrlAction.isGoodLink(pageUrlWithEscapeEnd, goodLink));
        assertFalse(FormatUrlAction.isGoodLink(pageUrlWithEscapeEnd, badLink));
    }

    private void testIsSameLink() {
        String pageUrl = "https://test-site-url/path";
        String pageUrlWithEscapeEnd = "https://test-site-url/path/";
        assertFalse(FormatUrlAction.isGoodLink(pageUrl, pageUrl));
        assertFalse(FormatUrlAction.isGoodLink(pageUrl, pageUrlWithEscapeEnd));
        assertFalse(FormatUrlAction.isGoodLink(pageUrlWithEscapeEnd, pageUrl));
        assertFalse(FormatUrlAction.isGoodLink(pageUrlWithEscapeEnd, pageUrlWithEscapeEnd));
    }

    private void testEndsWithScrollUpSymbol() {
        String pageUrl = "https://test-site-url/path";
        String pageUrlWithEscapeEnd = "https://test-site-url/path/";
        String linkWithEscape = "https://test-site-url/path#";
        String linkWithoutEscape = "https://test-site-url/path/#";
        assertFalse(FormatUrlAction.isGoodLink(pageUrl, linkWithEscape));
        assertFalse(FormatUrlAction.isGoodLink(pageUrl, linkWithoutEscape));
        assertFalse(FormatUrlAction.isGoodLink(pageUrlWithEscapeEnd, linkWithEscape));
        assertFalse(FormatUrlAction.isGoodLink(pageUrlWithEscapeEnd, linkWithoutEscape));
    }

    private void testStartWithPageUrlAndScrollUpSymbol() {
        String pageUrl = "https://test-site-url/path";
        String pageUrlWithEscapeEnd = "https://test-site-url/path/";
        String link = "https://test-site-url/path#smth";
        assertFalse(FormatUrlAction.isGoodLink(pageUrl, link));
        assertFalse(FormatUrlAction.isGoodLink(pageUrlWithEscapeEnd, link));
    }

    @Test
    @DisplayName("Determine if path is site home page relative path")
    public void testIsHomePageRelativePath() {
        String homePageRelativePath = "/";
        assertTrue(FormatUrlAction.isHomePageRelativePath(homePageRelativePath));
    }
}
