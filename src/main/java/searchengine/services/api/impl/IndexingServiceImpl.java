package searchengine.services.api.impl;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.SiteIndexingStatus;
import searchengine.services.actions.IndexingThreadAction;
import searchengine.services.actions.ParseAction;
import searchengine.services.actions.PrepareDatabaseBeforeIndexingAction;
import searchengine.services.api.IndexingService;
import searchengine.services.entity.SiteService;
import searchengine.util.IndexingResponseGenerator;
import searchengine.util.LockGenerator;
import searchengine.util.UrlFormatter;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Log4j2
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    @Setter(AccessLevel.PRIVATE)
    @Getter
    private static volatile boolean isIndexingInProcess = false;

    private ExecutorService executorThreadPool;

    private final SitesList sites;
    private final SiteService siteService;
    private final IndexingThreadAction indexingThreadAction;
    private final PrepareDatabaseBeforeIndexingAction prepareDatabaseAction;

    @Override
    public IndexingResponse initiateFullIndexing() {
        if (isIndexingInProcess) {
            return IndexingResponseGenerator.getIndexingAlreadyStartedResponse();
        }
        log.info("Full indexing initiated by user");
        setIndexingInProcessStatus();
        ParseAction.isReadyForFullParsing();
        Thread indexingTask = new Thread(this::indexSiteListFromConfig, "site-indexing-thread");
        indexingTask.start();
        return IndexingResponseGenerator.getAllGoodResponse();
    }

    private void indexSiteListFromConfig() {
        prepareDatabaseAction.prepareDatabaseBeforeFullIndexingStart();
        log.info("Full indexing started");
        List<Site> siteList = sites.getSites();
        startConcurrentFullIndexing(siteList);
        if (!isIndexingFinishedCorrectly()) {
            log.error("Some site statuses are incorrect after completing full indexing");
        }
        setIndexingCompletedStatus();
        log.info("Indexing complete flag was returned to original state");
        log.info("Full indexing is finished");
    }

    private void startConcurrentFullIndexing(List<Site> siteList) {
        CountDownLatch countDownLatch = new CountDownLatch(siteList.size());
        executorThreadPool = Executors.newFixedThreadPool(siteList.size());
        try {
            for (Site site : siteList) {
                executorThreadPool.submit(() ->
                        indexingThreadAction.indexingOneSite(site.getUrl(), site.getName(), countDownLatch));
            }
            countDownLatch.await();
        } catch (InterruptedException ex) {
            log.error("Unexpected exception while executing one or several indexing tasks", ex);
        } finally {
            executorThreadPool.shutdown();
        }
    }

    private boolean isIndexingFinishedCorrectly() {
        boolean isIndexingFinished = false;
        try {
            LockGenerator.lockSiteParseReadLock();
            isIndexingFinished = siteService.getAll().stream()
                    .allMatch(site -> Objects.equals(site.getStatus(), SiteIndexingStatus.INDEXED) ||
                            Objects.equals(site.getStatus(), SiteIndexingStatus.FAILED));
        } catch (Exception ex) {
            log.error("Exception while checking all sites statuses", ex);
        } finally {
            LockGenerator.unlockSiteParseReadLock();
        }
        return isIndexingFinished;
    }

    private Site getSiteForRequestedPageInConfig(String url) {
        List<Site> siteList = sites.getSites();
        return siteList.stream()
                .filter(site -> UrlFormatter.isPagePartOfSite(site.getUrl(), url))
                .findAny()
                .orElse(null);
    }

    @Override
    public IndexingResponse stopIndexing() {
        if (!isIndexingInProcess) {
            return IndexingResponseGenerator.getIndexingNotStartedResponse();
        }
        if (executorThreadPool != null) {
            executorThreadPool.shutdownNow();
        }
        setIndexingStoppedStatus();
        ParseAction.stopParsing();
        return IndexingResponseGenerator.getIndexingStoppedByUserResponse();
    }

    @Override
    public IndexingResponse initiatePartialIndexing(String url) {
        log.info("Indexing page initiated by user, url - {}", url);
        Site siteInConfig = getSiteForRequestedPageInConfig(url);
        if (siteInConfig == null) {
            return IndexingResponseGenerator.getPageNotListedInConfigResponse(url);
        }
        setIndexingInProcessStatus();
        ParseAction.isReadyForLimitedParsing();
        Thread indexingTask = new Thread(() ->
                indexingThreadAction.indexingAddedPage(url, siteInConfig), "page-indexing-thread");
        indexingTask.start();
        return IndexingResponseGenerator.getAllGoodResponse();
    }

    private void setIndexingInProcessStatus() {
        setIndexingFlags(false, true);
    }

    private void setIndexingStoppedStatus() {
        setIndexingFlags(true, true);
    }

    private void setIndexingCompletedStatus() {
        setIndexingFlags(false, false);
    }

    private void setIndexingFlags(boolean isCancelledStopIndexing, boolean isIndexingInProcess) {
        IndexingThreadAction.setCancelledStopIndexing(isCancelledStopIndexing);
        setIndexingInProcess(isIndexingInProcess);
    }

}
