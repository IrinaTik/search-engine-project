package searchengine.services.actions;

import org.apache.commons.lang3.StringUtils;
import searchengine.exceptions.PageNotFromSiteException;
import searchengine.model.SiteEntity;

public class FormatUrlAction {

    private static final String SCROLLUP_LINK = "#";
    private static final String ESCAPE_SYMBOL = "/";
    public static final String SITE_HOME_PAGE_RELATIVE_PATH = ESCAPE_SYMBOL;

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
                !StringUtils.endsWith(link, SCROLLUP_LINK) &&
                !StringUtils.startsWithIgnoreCase(link, pageUrl + SCROLLUP_LINK);
    }

    private static boolean isSameLink(String link1, String link2) {
        return StringUtils.equalsIgnoreCase(link1, link2);
    }

    public static boolean isHomePageRelativePath(String relativePath) {
        return StringUtils.equals(relativePath, SITE_HOME_PAGE_RELATIVE_PATH);
    }
}
