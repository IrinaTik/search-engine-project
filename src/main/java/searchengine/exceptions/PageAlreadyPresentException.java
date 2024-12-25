package searchengine.exceptions;

public class PageAlreadyPresentException extends RuntimeException {
    public PageAlreadyPresentException() {
    }

    public PageAlreadyPresentException(String pageUrl, String siteUrl) {
        super("Page " + pageUrl + " from site " + siteUrl + " is already saved to DB");
    }
}
