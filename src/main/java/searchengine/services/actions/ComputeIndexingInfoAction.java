package searchengine.services.actions;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import searchengine.dto.indexing.PageIndexingData;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.services.entity.IndexService;
import searchengine.services.entity.LemmaService;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Log4j2
@Service
@RequiredArgsConstructor
public class ComputeIndexingInfoAction {

    private final CollectLemmasAction collectLemmasAction;

    public PageIndexingData computeIndexingInfoForPage(LemmaService lemmaService,
                                                       IndexService indexService,
                                                       PageEntity page) {
        log.info("Computing indexing info for page {} started",
                page.getSite().getUrl() + page.getRelativePath());
        Instant start = Instant.now();
        String cleanedPageContent = collectLemmasAction.cleanText(page.getContent());
        Map<String, Integer> lemmasFromTextWithCount =
                collectLemmasAction.collectLemmasFromCleanedTextWithCount(cleanedPageContent);
        PageIndexingData pageIndexingData = computePageIndexingData(
                lemmaService, indexService, lemmasFromTextWithCount, page);
        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);
        log.info("Computing indexing info for page {} completed in {} min {} sec {} ms",
                page.getSite().getUrl() + page.getRelativePath(),
                duration.toMinutes(), duration.toSecondsPart(), duration.toMillisPart());
        return pageIndexingData;
    }

    private PageIndexingData computePageIndexingData(LemmaService lemmaService,
                                                     IndexService indexService,
                                                     Map<String, Integer> lemmasFromTextWithCount,
                                                     PageEntity page) {
        List<LemmaEntity> lemmasByPage = new ArrayList<>();
        List<IndexEntity> indexesByPage = new ArrayList<>();
        PageIndexingData pageIndexingData = new PageIndexingData(page, lemmasByPage, indexesByPage);
        for (String lemma : lemmasFromTextWithCount.keySet()) {
            LemmaEntity lemmaEntity = lemmaService.increaseLemmaFrequencyBySite(lemma, page.getSite());
            Float rank = Float.valueOf(lemmasFromTextWithCount.get(lemma));
            IndexEntity indexEntity = indexService.createIndexForPage(lemmaEntity, rank, page);
            lemmasByPage.add(lemmaEntity);
            indexesByPage.add(indexEntity);
        }
        return pageIndexingData;
    }

}
