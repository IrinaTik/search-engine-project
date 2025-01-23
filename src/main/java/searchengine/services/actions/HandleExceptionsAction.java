package searchengine.services.actions;

import lombok.extern.log4j.Log4j2;
import searchengine.exceptions.InvalidSearchQueryException;
import searchengine.exceptions.PageAlreadyPresentException;
import searchengine.model.SiteEntity;
import searchengine.model.SiteIndexingStatus;
import searchengine.services.entity.SiteService;

@Log4j2
public class HandleExceptionsAction {

    public static void handlePageAlreadyPresentExceptions(String pageUrl, SiteEntity site) {
        PageAlreadyPresentException exception = new PageAlreadyPresentException(pageUrl, site.getUrl());
        log.warn(exception.getMessage());
    }

    public static void handleUnexpectedIndexingException(SiteService siteService, SiteEntity site, Exception ex) {
        try {
            GenerateLockAction.lockSiteParseWriteLock();
            siteService.updateSiteStatusInfo(
                    SiteIndexingStatus.FAILED,
                    "Unexpected exception : " + ex.getMessage() + ". See logs for more information.",
                    site);
            siteService.save(site);
            log.error("Site {} was saved as FAILED because of unexpected exception", site.getUrl(), ex);
        } catch (Exception exception) {
            log.error("Exception inside of exception while saving info for site {}", site.getUrl(), ex);
        } finally {
            GenerateLockAction.unlockSiteParseWriteLock();
        }
    }

    public static void handleInvalidSearchQueryExceptions(String query) {
        InvalidSearchQueryException exception = new InvalidSearchQueryException(query);
        log.error(exception.getMessage());
    }
}
