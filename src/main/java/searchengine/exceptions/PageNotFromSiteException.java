package searchengine.exceptions;

public class PageNotFromSiteException extends RuntimeException {
    public PageNotFromSiteException(String pageUrl, String siteUrl) {
        super("Page " + pageUrl + " is not part of site " + siteUrl);
    }
}
