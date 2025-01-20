package searchengine.services.actions;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.JsoupConfig;
import searchengine.config.Site;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.exceptions.IndexingStoppedByUserException;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.SiteIndexingStatus;
import searchengine.services.entity.IndexService;
import searchengine.services.entity.LemmaService;
import searchengine.services.entity.PageService;
import searchengine.services.entity.SiteService;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;

@Log4j2
@Service
@RequiredArgsConstructor
public class IndexingThreadAction {

    @Setter
    public static volatile boolean isCancelledStopIndexing = false;

    private final JsoupConfig jsoupConfig;
    private final SiteService siteService;
    private final PageService pageService;
    private final LemmaService lemmaService;
    private final IndexService indexService;

    public IndexingResponse indexingOneSite(String url,
                                            String name,
                                            CountDownLatch countDownLatch) {
        log.info("Indexing site {} started", url);
        Instant start = Instant.now();
        SiteEntity site = siteService.createSiteByNameAndUrl(name, url);
        siteService.save(site);
        if (isCancelledStopIndexing) {
            handleStopByUser(site);
            return GenerateIndexingResponseAction.getIndexingStoppedByUserResponse();
        }
        IndexingResponse indexingOneSiteResponse = gatherSiteIndexingInfo(site);
        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);
        log.info("Indexing site {} complete in {} minutes {} seconds",
                site.getUrl(), duration.toMinutes(), duration.toSecondsPart());
        countDownLatch.countDown();
        return indexingOneSiteResponse;
    }

    private void handleStopByUser(SiteEntity site) {
        try {
            GenerateLockAction.lockSiteParseWriteLock();
            siteService.updateSiteStatusInfo(
                    SiteIndexingStatus.FAILED,
                    GenerateIndexingResponseAction.INDEXING_STOPPED_BY_USER_ERROR,
                    site);
            siteService.save(site);
            log.info("Site {} was saved as FAILED because of user stop", site.getUrl());
        } catch (Exception ex) {
            log.error("Exception while saving INDEXING STOPPED BY USER info for site {}", site.getUrl(), ex);
        } finally {
            GenerateLockAction.unlockSiteParseWriteLock();
        }
    }

    @Transactional
    public IndexingResponse gatherSiteIndexingInfo(SiteEntity site) {
        IndexingResponse indexingResponse = null;
        try {
            parseSite(site);
            indexingResponse = getResponseAccordingToSiteStatus(site);
        } catch (IndexingStoppedByUserException ex) {
            handleStopByUser(site);
            indexingResponse = GenerateIndexingResponseAction.getIndexingStoppedByUserResponse();
        } catch (Exception ex) {
            HandleExceptionsAction.handleUnexpectedIndexingException(siteService, site, ex);
        } finally {
            log.info("Indexing for site {} is completed with status {}", site.getUrl(), site.getStatus());
        }
        return indexingResponse;
    }

    private void parseSite(SiteEntity site) {
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        ParseAction parser = new ParseAction(site.getUrl(), site, jsoupConfig, siteService, pageService, lemmaService, indexService);
        forkJoinPool.invoke(parser);
        if (isCancelledStopIndexing) {
            forkJoinPool.shutdownNow();
            throw new IndexingStoppedByUserException();
        }
    }

    private IndexingResponse getResponseAccordingToSiteStatus(SiteEntity site) {
        if (site.getStatus() == SiteIndexingStatus.FAILED) {
            return GenerateIndexingResponseAction.getIndexingFailedErrorResponse(site.getUrl(), site.getLastError());
        }
        if (!pageService.isSiteHomePageAccessible(site)) {
            siteService.updateSiteStatusInfo(
                    SiteIndexingStatus.FAILED,
                    GenerateIndexingResponseAction.SITE_HOME_PAGE_NOT_ACCESSIBLE,
                    site);
            siteService.save(site);
            return GenerateIndexingResponseAction.getSiteHomePageNotAccessibleResponse(site.getUrl());
        }
        siteService.updateSiteStatusInfo(SiteIndexingStatus.INDEXED, "", site);
        siteService.save(site);
        return GenerateIndexingResponseAction.getAllGoodResponse();
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
            PrepareDatabaseBeforeIndexingAction.prepareDatabaseBeforePartialIndexingStart(
                    pageService, lemmaService, indexService, page);
        }
        parseAddedPage(url, site);
        getResponseAccordingToSiteStatus(site);
        log.info("Indexing page is finished, url - {}", url);
    }

    private void parseAddedPage(String url, SiteEntity site) {
        ParseAction parser = new ParseAction(url, site, jsoupConfig, siteService, pageService, lemmaService, indexService);
        parser.invoke();
    }

}
