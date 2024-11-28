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
import searchengine.model.SiteEntity;
import searchengine.services.actions.ParseAction;
import searchengine.services.api.IndexingService;
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
import static searchengine.services.actions.GenerateIndexingResponseAction.*;
import static searchengine.services.actions.GenerateLockAction.*;

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

    @Override
    public IndexingResponse indexingAllSites() {
        if (isIndexingInProcess()) {
            return getIndexingAlreadyStartedResponse();
        }
        log.info("Full indexing initiated by user");
        setCancelledStopIndexing(false);
        setIndexingInProcess(true);
        prepareDatabaseBeforeIndexingStart();
        Thread indexingTask = new Thread(this::indexSiteListFromConfig, "indexing-thread");
        indexingTask.start();
        return getAllGoodResponse();
    }

    private void prepareDatabaseBeforeIndexingStart() {
        log.info("Deleting all info from database started");
        pageService.deleteAll();
        siteService.deleteAll();
        log.info("Deleting all info from database completed");
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
                executorThreadPool.submit(() -> indexingOneSite(site.getUrl(), site.getName(), countDownLatch));
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
            log.error("Exception while saving INDEXING STOPPED BY USER info for sile {}", site.getUrl(), ex);
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
            log.error("Unexpected exception while processing site {}", site.getUrl(), ex);
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
        ParseAction parser = new ParseAction(site.getUrl(), site, jsoupConfig, siteService, pageService);
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
        // тут будет всякое леммасобирательство
        siteService.updateSiteStatusInfo(INDEXED, "", site);
        siteService.save(site);
        return getAllGoodResponse();
    }

    @Override
    public IndexingResponse indexingAddedPage() {
        return new IndexingResponse(false, "test indexingAddedPage response");
    }

    @Override
    public IndexingResponse stopIndexing() {
        if (!isIndexingInProcess()) {
            return getIndexingNotStartedResponse();
        }
        executorThreadPool.shutdownNow();
        setCancelledStopIndexing(true);
        setIndexingInProcess(false);
        ParseAction.stopParsing();
        return getIndexingStoppedByUserResponse();
    }
}
