package searchengine.services.api.impl;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.JsoupConfig;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.exceptions.IndexingStoppedByUserException;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.services.actions.ParseAction;
import searchengine.services.api.IndexingService;
import searchengine.services.entity.IndexService;
import searchengine.services.entity.LemmaService;
import searchengine.services.entity.PageService;
import searchengine.services.entity.SiteService;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

import static searchengine.model.SiteIndexingStatus.FAILED;
import static searchengine.model.SiteIndexingStatus.INDEXED;
import static searchengine.services.actions.FormatUrlAction.isPagePartOfSite;
import static searchengine.services.actions.GenerateIndexingResponseAction.*;
import static searchengine.services.actions.GenerateLockAction.*;
import static searchengine.services.actions.HandleExceptionsAction.handleUnexpectedIndexingException;
import static searchengine.services.actions.PrepareDatabaseBeforeIndexingAction.prepareDatabaseBeforeFullIndexingStart;
import static searchengine.services.actions.PrepareDatabaseBeforeIndexingAction.prepareDatabaseBeforePartialIndexingStart;

@Log4j2
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    @Setter(AccessLevel.PRIVATE)
    @Getter
    private static volatile boolean isCancelledStopIndexing = false;
    @Setter(AccessLevel.PRIVATE)
    @Getter
    private static volatile boolean isIndexingInProcess = false;

    private ExecutorService executorThreadPool;

    private final SitesList sites;
    private final JsoupConfig jsoupConfig;
    private final SiteService siteService;
    private final PageService pageService;
    private final LemmaService lemmaService;
    private final IndexService indexService;

    @Override
    public IndexingResponse initiateFullIndexing() {
        if (isIndexingInProcess) {
            return getIndexingAlreadyStartedResponse();
        }
        log.info("Full indexing initiated by user");
        setIndexingFlags(false, true);
        prepareDatabaseBeforeFullIndexingStart(siteService, pageService, lemmaService, indexService);
        ParseAction.readyForFullParsing();
        Thread indexingTask = new Thread(this::indexSiteListFromConfig, "site-indexing-thread");
        indexingTask.start();
        return getAllGoodResponse();
    }

    private void indexSiteListFromConfig() {
        log.info("Full indexing started");
        List<Site> siteList = sites.getSites();
        startConcurrentFullIndexing(siteList);
        if (!isIndexingFinishedCorrectly()) {
            log.error("Some site statuses are incorrect after completing full indexing");
        }
        setIndexingInProcess(false);
        log.info("Indexing complete flag was returned to original state");
        log.info("Full indexing is finished");
    }

    private void startConcurrentFullIndexing(List<Site> siteList) {
        CountDownLatch countDownLatch = new CountDownLatch(siteList.size());
        executorThreadPool = Executors.newFixedThreadPool(siteList.size());
        try {
            for (Site site : siteList) {
                executorThreadPool.submit(() ->
                        indexingOneSite(site.getUrl(), site.getName(), countDownLatch));
            }
            countDownLatch.await();
        } catch (InterruptedException ex) {
            log.error("Unexpected exception while executing one or several indexing tasks", ex);
        } finally {
            executorThreadPool.shutdown();
        }
    }

    @Transactional
    public IndexingResponse indexingOneSite(String url, String name, CountDownLatch countDownLatch) {
        log.info("Indexing site {} started", url);
        Instant start = Instant.now();
        SiteEntity site = siteService.createSiteByNameAndUrl(name, url);
        siteService.save(site);
        if (isCancelledStopIndexing()) {
            handleStopByUser(site);
            return getIndexingStoppedByUserResponse();
        }
        IndexingResponse indexingOneSiteResponse = gatherSiteIndexingInfo(site);
        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);
        log.info("Indexing site {} complete in {} minutes {} seconds",
                site.getUrl(),
                duration.toMinutes(),
                duration.toSecondsPart());
        countDownLatch.countDown();
        return indexingOneSiteResponse;
    }

    private void handleStopByUser(SiteEntity site) {
        try {
            lockSiteParseWriteLock();
            siteService.updateSiteStatusInfo(
                    FAILED,
                    INDEXING_STOPPED_BY_USER_ERROR,
                    site);
            siteService.save(site);
            log.info("Site {} was saved as FAILED because of user stop", site.getUrl());
        } catch (Exception ex) {
            log.error("Exception while saving INDEXING STOPPED BY USER info for site {}", site.getUrl(), ex);
        } finally {
            unlockSiteParseWriteLock();
        }
    }

    private IndexingResponse gatherSiteIndexingInfo(SiteEntity site) {
        IndexingResponse indexingResponse = null;
        try {
            parseSite(site);
            indexingResponse = getResponseAccordingToSiteStatus(site);
        } catch (IndexingStoppedByUserException ex) {
            handleStopByUser(site);
            indexingResponse = getIndexingStoppedByUserResponse();
        } catch (Exception ex) {
            handleUnexpectedIndexingException(siteService, site, ex);
        } finally {
            log.info("Indexing for site {} is completed with status {}", site.getUrl(), site.getStatus());
        }
        return indexingResponse;
    }

    private boolean isIndexingFinishedCorrectly() {
        boolean isIndexingFinished = false;
        try {
            lockSiteParseReadLock();
            isIndexingFinished = siteService.getAll().stream()
                    .allMatch(site -> Objects.equals(site.getStatus(), INDEXED) || Objects.equals(site.getStatus(), FAILED));
        } catch (Exception ex) {
            log.error("Exception while checking all sites statuses", ex);
        } finally {
            unlockSiteParseReadLock();
        }
        return isIndexingFinished;
    }

    private void parseSite(SiteEntity site) {
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        ParseAction parser = new ParseAction(site.getUrl(), site, jsoupConfig, siteService, pageService, lemmaService, indexService);
        forkJoinPool.invoke(parser);
        if (isCancelledStopIndexing()) {
            forkJoinPool.shutdownNow();
            throw new IndexingStoppedByUserException();
        }
    }

    private IndexingResponse getResponseAccordingToSiteStatus(SiteEntity site) {
        if (site.getStatus() == FAILED) {
            return getIndexingFailedErrorResponse(site.getUrl(), site.getLastError());
        }
        if (!pageService.isSiteHomePageAccessible(site)) {
            siteService.updateSiteStatusInfo(FAILED, SITE_HOME_PAGE_NOT_ACCESSIBLE, site);
            siteService.save(site);
            return getSiteHomePageNotAccessibleResponse(site.getUrl());
        }
        siteService.updateSiteStatusInfo(INDEXED, "", site);
        siteService.save(site);
        return getAllGoodResponse();
    }

    private Site getSiteForRequestedPageInConfig(String url) {
        List<Site> siteList = sites.getSites();
        return siteList.stream()
                .filter(site -> isPagePartOfSite(site.getUrl(), url))
                .findAny()
                .orElse(null);
    }

    @Override
    public IndexingResponse stopIndexing() {
        if (!isIndexingInProcess) {
            return getIndexingNotStartedResponse();
        }
        executorThreadPool.shutdownNow();
        setIndexingFlags(true, false);
        ParseAction.stopParsing();
        return getIndexingStoppedByUserResponse();
    }

    @Override
    public IndexingResponse initiatePartialIndexing(String url) {
        log.info("Indexing page initiated by user, url - {}", url);
        Site siteInConfig = getSiteForRequestedPageInConfig(url);
        if (siteInConfig == null) {
            return getPageNotListedInConfigResponse(url);
        }
        indexingAddedPage(url, siteInConfig);
        return getAllGoodResponse();
    }

    @Transactional
    public void indexingAddedPage(String url, Site siteInConfig) {
        log.info("Indexing page is started, url - {}", url);
        SiteEntity site = siteService.getByUrl(siteInConfig.getUrl());
        if (site == null) {
            site = siteService.createSiteByNameAndUrl(siteInConfig.getName(), siteInConfig.getUrl());
            siteService.save(site);
        }
        PageEntity page = pageService.getByAbsPathAndSite(url, site);
        if (page != null) {
            prepareDatabaseBeforePartialIndexingStart(pageService, lemmaService, indexService, page);
        }
        parseAddedPage(url, site);
        getResponseAccordingToSiteStatus(site);
        log.info("Indexing page is finished, url - {}", url);
    }

    private void parseAddedPage(String url, SiteEntity site) {
        ParseAction parser = new ParseAction(url, site, jsoupConfig, siteService, pageService, lemmaService, indexService);
        ParseAction.readyForLimitedParsing();
        parser.invoke();
    }

    private void setIndexingFlags(boolean isCancelledStopIndexing, boolean isIndexingInProcess) {
        setCancelledStopIndexing(isCancelledStopIndexing);
        setIndexingInProcess(isIndexingInProcess);
    }

}
