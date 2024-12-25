package searchengine.services.actions;

import lombok.extern.log4j.Log4j2;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.services.entity.IndexService;
import searchengine.services.entity.LemmaService;
import searchengine.services.entity.PageService;
import searchengine.services.entity.SiteService;

import java.util.List;

@Log4j2
public class PrepareDatabaseBeforeIndexingAction {

    public static void prepareDatabaseBeforeFullIndexingStart(SiteService siteService,
                                                              PageService pageService,
                                                              LemmaService lemmaService,
                                                              IndexService indexService) {
        log.info("Deleting all info from database started");
        indexService.deleteAll();
        lemmaService.deleteAll();
        pageService.deleteAll();
        siteService.deleteAll();
        log.info("Deleting all info from database completed");
    }

    public static void prepareDatabaseBeforePartialIndexingStart(PageService pageService,
                                                                 LemmaService lemmaService,
                                                                 IndexService indexService,
                                                                 PageEntity page) {
        log.info("Deleting info related to {} from database started",
                page.getSite().getUrl() + page.getRelativePath());
        deleteIndexingInfoByPage(page, lemmaService, indexService);
        pageService.delete(page);
        log.info("Deleting info related to {} from database completed",
                page.getSite().getUrl() + page.getRelativePath());
    }

    private static void deleteIndexingInfoByPage(PageEntity page,
                                                 LemmaService lemmaService,
                                                 IndexService indexService) {
        List<IndexEntity> indexesByPage = indexService.getByPage(page);
        if (indexesByPage == null || indexesByPage.isEmpty()) {
            log.error("Index list by page {} is empty", page.getSite().getUrl() + page.getRelativePath());
            return;
        }
        List<LemmaEntity> lemmasByPage = indexesByPage.stream().map(IndexEntity::getLemma).toList();
        log.info("Page {} - indexes count : {}, lemmas count {}",
                page.getSite().getUrl() + page.getRelativePath(), indexesByPage.size(), lemmasByPage.size());
        indexService.deleteAll(indexesByPage);
        lemmasByPage.forEach(lemmaService::decreaseLemmaFrequencyInDatabase);
    }

}
