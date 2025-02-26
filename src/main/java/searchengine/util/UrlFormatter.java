package searchengine.util;

import org.apache.commons.lang3.StringUtils;
import searchengine.exceptions.PageNotFromSiteException;
import searchengine.model.SiteEntity;

import java.util.List;

public class UrlFormatter {

    private static final String SCROLLUP_LINK = "#";
    private static final String ESCAPE_SYMBOL = "/";
    public static final String SITE_HOME_PAGE_RELATIVE_PATH = ESCAPE_SYMBOL;
    private static final List<String> URL_IS_FILE = List.of(".doc", ".docx", ".pdf", ".png", ".jpg", ".jpeg");

    public static String convertAbsPathToRelativePath(String absPath, SiteEntity site) {
        String siteUrl = removeEscapeEnd(site.getUrl());
        String absPathWithoutEscapeEnd = removeEscapeEnd(absPath);
        if (isSameLink(absPathWithoutEscapeEnd, siteUrl)) {
            return SITE_HOME_PAGE_RELATIVE_PATH;
        }
        if (!StringUtils.startsWithIgnoreCase(absPath, siteUrl)) {
            throw new PageNotFromSiteException(absPath, siteUrl);
        }
        return StringUtils.removeStartIgnoreCase(absPath, siteUrl);
    }

    public static String removeEscapeEnd(String url) {
        return StringUtils.endsWith(url, ESCAPE_SYMBOL) ? StringUtils.removeEnd(url, ESCAPE_SYMBOL) : url;
    }

    public static boolean isGoodLink(String pageUrl, String link) {
        pageUrl = removeEscapeEnd(pageUrl);
        link = removeEscapeEnd(link);
        return StringUtils.startsWithIgnoreCase(link, pageUrl) &&
                !isSameLink(pageUrl, link) &&
                !StringUtils.contains(link, SCROLLUP_LINK) &&
                !StringUtils.startsWithIgnoreCase(link, pageUrl + SCROLLUP_LINK) &&
                !isFileLink(link);
    }

    private static boolean isSameLink(String link1, String link2) {
        return StringUtils.equalsIgnoreCase(link1, link2);
    }

    private static boolean isFileLink(String link) {
        return URL_IS_FILE.stream().anyMatch(fileLinkPart -> StringUtils.containsIgnoreCase(link, fileLinkPart));
    }

    public static boolean isHomePageRelativePath(String relativePath) {
        return StringUtils.equals(relativePath, SITE_HOME_PAGE_RELATIVE_PATH);
    }

    public static boolean isPagePartOfSite(String siteUrl, String pageUrl) {
        String siteUrlWithEscapeEnd = siteUrl;
        if (!StringUtils.endsWith(siteUrl, ESCAPE_SYMBOL)) {
            siteUrlWithEscapeEnd = siteUrl + ESCAPE_SYMBOL;
        }
        return StringUtils.containsIgnoreCase(pageUrl, siteUrlWithEscapeEnd);
    }
}
