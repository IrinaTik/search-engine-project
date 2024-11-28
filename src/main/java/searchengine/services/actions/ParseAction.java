package searchengine.services.actions;

import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import searchengine.config.JsoupConfig;
import searchengine.exceptions.PageAlreadyPresentException;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.SiteIndexingStatus;
import searchengine.services.entity.PageService;
import searchengine.services.entity.SiteService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RecursiveAction;

import static searchengine.model.SiteIndexingStatus.FAILED;
import static searchengine.model.SiteIndexingStatus.INDEXING;
import static searchengine.services.actions.ExtractConnectionInfoAction.getChildLinksFromResponse;
import static searchengine.services.actions.ExtractConnectionInfoAction.getResponseFromUrl;
import static searchengine.services.actions.GenerateLockAction.*;

@Log4j2
public class ParseAction extends RecursiveAction {

    private final JsoupConfig jsoupConfig;

    @Setter(AccessLevel.PRIVATE)
    private static boolean isCancelled;

    private final SiteService siteService;
    private final PageService pageService;

    private final SiteEntity site;
    private final String pageUrl;

    public ParseAction(String pageUrl,
                       SiteEntity site,
                       JsoupConfig jsoupConfig,
                       SiteService siteService,
                       PageService pageService) {
        this.pageUrl = pageUrl;
        this.site = site;
        this.jsoupConfig = jsoupConfig;
        this.siteService = siteService;
        this.pageService = pageService;
    }

    @Override
    protected void compute() {
        if (isCancelled) {
            return;
        }
        log.info("Starting parsing for page {}", pageUrl);
        PageEntity page = extractPageFromUrl();
        if (isPageValidToProcess(page)) {
            saveExtractedPage(page);
            Set<String> childLinks = page.getChildLinks();
            if (!isCancelled && childLinksAreValidToProcess(childLinks)) {
                forkAndJoinParseTasks(childLinks);
            }
        }
    }

    private PageEntity extractPageFromUrl() {
        if (isUrlPresentInDatabase()) {
            handlePageAlreadyPresentExceptions();
            return null;
        }
        PageEntity page = pageService.createPageByAbsPathAndSitePath(pageUrl, site);
        SiteIndexingStatus status = INDEXING;
        String siteLastError = "";
        try {
            extractPageParseInfoFromResponse(page);
        } catch (HttpStatusException ex) {
            log.warn("HttpStatusException for page {}", pageUrl);
            pageService.updateParseInfo(ex.getStatusCode(), "", null, page);
        } catch (Exception ex) {
            log.error("Exception while parsing page {} -> {}", pageUrl, ex.getMessage(), ex);
            status = FAILED;
            siteLastError = "Ошибка при парсинге страницы " + pageUrl + " -> " + ex.getMessage();
        } finally {
            updateSiteStatusInfoAfterParsingPage(status, siteLastError, page);
        }
        return page;
    }

    private boolean isPageValidToProcess(PageEntity page) {
        return page != null && page.getCode() != null;
    }

    private void saveExtractedPage(PageEntity page) {
        boolean isPageSaved = false;
        debugCheckLock();
        try {
            lockPageParseWriteLock();
            isPageSaved = saveExtractedPageToDatabase(page);
        } catch (Exception ex) {
            log.error("Exception while saving to DB page {}", pageUrl, ex);
        } finally {
            unlockPageParseWriteLock();
            if (!isPageSaved) {
                handlePageAlreadyPresentExceptions();
            }
        }
    }

    private void debugCheckLock() {
        if (PAGE_PARSE_LOCK.isWriteLocked()) {
            log.debug("Page write lock is NOT free for page {} \n\t {}", pageUrl, PAGE_PARSE_LOCK);
            if (PAGE_PARSE_LOCK.isWriteLockedByCurrentThread()) {
                log.error("Page write lock is being held by this thread while parsing page {} \n\t {}",
                        pageUrl,
                        PAGE_PARSE_LOCK);
            }
        }
    }

    private boolean saveExtractedPageToDatabase(PageEntity page) {
        if (!isVisitedPage()) {
            pageService.save(page);
            log.info("Page {} is saved to DB", pageUrl);
            return true;
        }
        return false;
    }

    private boolean childLinksAreValidToProcess(Set<String> childLinks) {
        return childLinks != null && !childLinks.isEmpty();
    }

    private void forkAndJoinParseTasks(Set<String> childLinks) {
        List<ParseAction> parsers = new ArrayList<>();
        for (String childLink : childLinks) {
            if (isCancelled) {
                return;
            }
            ParseAction parser = new ParseAction(childLink, site, jsoupConfig, siteService, pageService);
            parser.fork();
            parsers.add(parser);
        }
        for (ParseAction parser : parsers) {
            parser.join();
        }
    }

    private boolean isUrlPresentInDatabase() {
        boolean isVisitedPage = false;
        try {
            lockPageParseReadLock();
            isVisitedPage = isVisitedPage();
        } catch (Exception ex) {
            log.error("Exception while trying to determine if page {} is already present in DB", pageUrl, ex);
        } finally {
            unlockPageParseReadLock();
        }
        return isVisitedPage;
    }

    private void extractPageParseInfoFromResponse(PageEntity page) throws Exception {
        log.info("Extracting page {}, site -> {}", pageUrl, site.getUrl());
        Connection.Response response = getResponseFromUrl(pageUrl, jsoupConfig);
        log.debug("Response for {} -> response.statusCode() {}, response.body() {}",
                pageUrl,
                response.statusCode(),
                (response.body() == null || response.body().isEmpty()) ? "null or empty" : "OK");
        Set<String> pageChildLinks = getChildLinksFromResponse(response, pageUrl);
        pageService.updateParseInfo(response.statusCode(), response.body(), pageChildLinks, page);
    }

    private void updateSiteStatusInfoAfterParsingPage(SiteIndexingStatus status, String siteLastError, PageEntity page) {
        log.info("Parsing complete with code {} for page {}",
                page.getCode() != null ? page.getCode() : "unknown",
                pageUrl);
        if (!isCancelled && (site.getStatus() != FAILED)) {
            updateSiteStatusInfo(status, siteLastError);
        }
    }

    private void handlePageAlreadyPresentExceptions() {
        PageAlreadyPresentException exception = new PageAlreadyPresentException(pageUrl, site.getUrl());
        log.warn(exception.getMessage());
    }

    private boolean isVisitedPage() {
        return pageService.getByAbsPathAndSite(pageUrl, site) != null;
    }

    private void updateSiteStatusInfo(SiteIndexingStatus status, String siteLastError) {
        try {
            lockSiteParseWriteLock();
            siteService.updateSiteStatusInfo(status, siteLastError, site);
            siteService.save(site);
            log.debug("Site status info was updated after parsing page {}", pageUrl);
        } catch (Exception ex) {
            log.error("Exception while updating info for site {} after parsing page {}",
                    site.getUrl(),
                    pageUrl,
                    ex);
        } finally {
            unlockSiteParseWriteLock();
        }
    }

    public static void stopParsing() {
        setCancelled(true);
    }

}
