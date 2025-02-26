package searchengine.services.actions;

import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import searchengine.config.JsoupConfig;
import searchengine.dto.indexing.PageIndexingData;
import searchengine.exceptions.PageAlreadyPresentException;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.SiteIndexingStatus;
import searchengine.services.entity.IndexService;
import searchengine.services.entity.LemmaService;
import searchengine.services.entity.PageService;
import searchengine.services.entity.SiteService;
import searchengine.util.ConnectionInfoExtractor;
import searchengine.util.ExceptionsHandler;
import searchengine.util.LockGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RecursiveAction;

@Log4j2
public class ParseAction extends RecursiveAction {

    private final JsoupConfig jsoupConfig;

    @Setter(AccessLevel.PRIVATE)
    private static boolean isCancelled;
    @Setter(AccessLevel.PRIVATE)
    private static boolean isLimited;

    private final SiteService siteService;
    private final PageService pageService;
    private final LemmaService lemmaService;
    private final IndexService indexService;
    private final ComputeIndexingInfoAction computeIndexingInfoAction;

    private final SiteEntity site;
    private final String pageUrl;

    public ParseAction(String pageUrl,
                       SiteEntity site,
                       JsoupConfig jsoupConfig,
                       SiteService siteService,
                       PageService pageService,
                       LemmaService lemmaService,
                       IndexService indexService,
                       ComputeIndexingInfoAction computeIndexingInfoAction) {
        this.pageUrl = pageUrl;
        this.site = site;
        this.jsoupConfig = jsoupConfig;
        this.siteService = siteService;
        this.pageService = pageService;
        this.lemmaService = lemmaService;
        this.indexService = indexService;
        this.computeIndexingInfoAction = computeIndexingInfoAction;
    }

    @Override
    protected void compute() {
        if (isCancelled) {
            return;
        }
        log.info("Starting parsing for page {}", pageUrl);
        PageEntity page = extractPageFromUrl();
        if (isPageValidToProcess(page)) {
            processExtractedPage(page);
            Set<String> childLinks = page.getChildLinks();
            if (!isCancelled && !isLimited && childLinksAreValidToProcess(childLinks)) {
                forkAndJoinParseTasks(childLinks);
            }
        }
    }

    private PageEntity extractPageFromUrl() {
        if (isUrlPresentInDatabase()) {
            ExceptionsHandler.handlePageAlreadyPresentExceptions(pageUrl, site);
            return null;
        }
        PageEntity page = pageService.createPageByAbsPathAndSitePath(pageUrl, site);
        SiteIndexingStatus status = SiteIndexingStatus.INDEXING;
        String siteLastError = "";
        try {
            extractPageParseInfoFromResponse(page);
        } catch (HttpStatusException ex) {
            log.warn("HttpStatusException for page {}", pageUrl);
            pageService.updateParseInfo(ex.getStatusCode(), "", null, page);
        } catch (Exception ex) {
            log.error("Exception while parsing page {} -> {}", pageUrl, ex.getMessage(), ex);
            status = SiteIndexingStatus.FAILED;
            siteLastError = "Ошибка при парсинге страницы " + pageUrl + " -> " + ex.getMessage();
        } finally {
            updateSiteStatusInfoAfterParsingPage(status, siteLastError, page);
        }
        return page;
    }

    private boolean isPageValidToProcess(PageEntity page) {
        return page != null && page.getCode() != null;
    }

    private void processExtractedPage(PageEntity page) {
        debugCheckLock();
        try {
            LockGenerator.lockPageParseWriteLock();
            computeAndSaveExtractedPageInfoToDatabase(page);
        } catch (PageAlreadyPresentException ex) {
            ExceptionsHandler.handlePageAlreadyPresentExceptions(pageUrl, site);
        } catch (Exception ex) {
            log.error("Exception while saving to database page with indexing info, url - {}", pageUrl, ex);
        } finally {
            LockGenerator.unlockPageParseWriteLock();
        }
    }

    private void debugCheckLock() {
        if (LockGenerator.PAGE_PARSE_LOCK.isWriteLocked()) {
            log.debug("Page write lock is NOT free for page {} \n\t {}", pageUrl, LockGenerator.PAGE_PARSE_LOCK);
            if (LockGenerator.PAGE_PARSE_LOCK.isWriteLockedByCurrentThread()) {
                log.debug("Page write lock is being held by this thread while parsing page {} \n\t {}",
                        pageUrl,
                        LockGenerator.PAGE_PARSE_LOCK);
            }
        }
    }

    private void computeAndSaveExtractedPageInfoToDatabase(PageEntity page) {
        boolean isPageSaved;
        isPageSaved = saveExtractedPageToDatabase(page);
        if (!isPageSaved) {
            throw new PageAlreadyPresentException();
        }
        if (ConnectionInfoExtractor.isPageCodeSuccessful(page.getCode())) {
            PageIndexingData pageIndexingData =
                    computeIndexingInfoAction.computeIndexingInfoForPage(lemmaService, indexService, page);
            saveExtractedPageIndexingDataToDatabase(pageIndexingData);
        } else {
            log.warn("Page {} was parsed with code {} - computing indexing info is not possible",
                    page.getSite().getUrl() + page.getRelativePath(), page.getCode());
        }
    }

    private boolean saveExtractedPageToDatabase(PageEntity page) {
        if (!isVisitedPage()) {
            pageService.save(page);
            log.info("Page {} is saved to database", pageUrl);
            return true;
        }
        return false;
    }

    private void saveExtractedPageIndexingDataToDatabase(PageIndexingData pageIndexingData) {
        if (pageIndexingData == null) {
            throw new IllegalArgumentException("Indexing data is null");
        }
        if (pageIndexingData.getLemmasByPage() == null || pageIndexingData.getLemmasByPage().isEmpty() ||
                pageIndexingData.getIndexesByPage() == null || pageIndexingData.getIndexesByPage().isEmpty()) {
            throw new IllegalArgumentException("Indexing data lemmas list and/or indexes list is empty");
        }
        lemmaService.saveAll(pageIndexingData.getLemmasByPage());
        indexService.saveAll(pageIndexingData.getIndexesByPage());
        log.info("Indexing info for page {} is saved to database\n\tLemmas count : {}, indexes count : {} ",
                pageIndexingData.getPage().getSite().getUrl() + pageIndexingData.getPage().getRelativePath(),
                pageIndexingData.getLemmasByPage().size(), pageIndexingData.getIndexesByPage().size());
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
            ParseAction parser = new ParseAction(childLink, site,
                    jsoupConfig, siteService, pageService, lemmaService, indexService, computeIndexingInfoAction);
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
            LockGenerator.lockPageParseReadLock();
            isVisitedPage = isVisitedPage();
        } catch (Exception ex) {
            log.error("Exception while trying to determine if page {} is already present in database", pageUrl, ex);
        } finally {
            LockGenerator.unlockPageParseReadLock();
        }
        return isVisitedPage;
    }

    private void extractPageParseInfoFromResponse(PageEntity page) throws Exception {
        log.info("Extracting page {}, site -> {}", pageUrl, site.getUrl());
        Connection.Response response = ConnectionInfoExtractor.getResponseFromUrl(pageUrl, jsoupConfig);
        log.debug("Response for {} -> response.statusCode() {}, response.body() {}",
                pageUrl,
                response.statusCode(),
                (response.body() == null || response.body().isEmpty()) ? "null or empty" : "OK");
        Set<String> pageChildLinks = ConnectionInfoExtractor.getChildLinksFromResponse(response, pageUrl);
        pageService.updateParseInfo(response.statusCode(), response.body(), pageChildLinks, page);
    }

    private void updateSiteStatusInfoAfterParsingPage(SiteIndexingStatus status, String siteLastError, PageEntity page) {
        log.info("Parsing complete with code {} for page {}",
                page.getCode() != null ? page.getCode() : "unknown",
                pageUrl);
        if (!isCancelled && (site.getStatus() != SiteIndexingStatus.FAILED)) {
            updateSiteStatusInfo(status, siteLastError);
        }
    }

    private boolean isVisitedPage() {
        return pageService.getByAbsPathAndSite(pageUrl, site) != null;
    }

    private void updateSiteStatusInfo(SiteIndexingStatus status, String siteLastError) {
        try {
            LockGenerator.lockSiteParseWriteLock();
            siteService.updateSiteStatusInfo(status, siteLastError, site);
            siteService.save(site);
            log.debug("Site status info was updated after parsing page {}", pageUrl);
        } catch (Exception ex) {
            log.error("Exception while updating info for site {} after parsing page {}",
                    site.getUrl(),
                    pageUrl,
                    ex);
        } finally {
            LockGenerator.unlockSiteParseWriteLock();
        }
    }

    public static void stopParsing() {
        setCancelled(true);
    }

    public static void isReadyForFullParsing() {
        setCancelled(false);
        setLimited(false);
    }

    public static void isReadyForLimitedParsing() {
        setCancelled(false);
        setLimited(true);
    }

}
