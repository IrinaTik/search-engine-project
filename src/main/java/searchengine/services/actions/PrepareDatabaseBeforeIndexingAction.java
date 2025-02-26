package searchengine.services.actions;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.services.entity.IndexService;
import searchengine.services.entity.LemmaService;
import searchengine.services.entity.PageService;
import searchengine.services.entity.SiteService;

import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class PrepareDatabaseBeforeIndexingAction {

    private final SiteService siteService;
    private final PageService pageService;
    private final LemmaService lemmaService;
    private final IndexService indexService;

    public void prepareDatabaseBeforeFullIndexingStart() {
        log.info("Deleting all info from database started");
        log.info("Deleting index table...");
        indexService.deleteAll();
        log.info("Deleting lemma table...");
        lemmaService.deleteAll();
        log.info("Deleting page table...");
        pageService.deleteAll();
        log.info("Deleting site table...");
        siteService.deleteAll();
        log.info("Deleting all info from database completed");
    }

    public void prepareDatabaseBeforePartialIndexingStart(PageEntity page) {
        log.info("Deleting info related to {} from database started",
                page.getSite().getUrl() + page.getRelativePath());
        deleteIndexingInfoByPage(page);
        pageService.delete(page);
        log.info("Deleting info related to {} from database completed",
                page.getSite().getUrl() + page.getRelativePath());
    }

    private void deleteIndexingInfoByPage(PageEntity page) {
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
