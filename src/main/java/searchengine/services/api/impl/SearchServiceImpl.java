package searchengine.services.api.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.exceptions.InvalidSearchQueryException;
import searchengine.model.*;
import searchengine.services.actions.CollectLemmasAction;
import searchengine.services.actions.GenerateSnippetAction;
import searchengine.services.api.SearchService;
import searchengine.services.entity.IndexService;
import searchengine.services.entity.LemmaService;
import searchengine.services.entity.PageService;
import searchengine.services.entity.SiteService;
import searchengine.util.ExceptionsHandler;
import searchengine.util.SearchResponseGenerator;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private static final Double LEMMA_FREQUENCY_PERCENT = 0.9;
    public static final Float DEFAULT_MAX_PAGE_RELEVANCE = 1f;

    private final SiteService siteService;
    private final PageService pageService;
    private final LemmaService lemmaService;
    private final IndexService indexService;
    private final GenerateSnippetAction snippetAction;
    private final CollectLemmasAction collectLemmasAction;

    private String currentQuery = "";
    private List<SiteEntity> currentSiteListForSearch = new ArrayList<>();
    private Map<PageEntity, Float> sortedPagesWithRelativeRelevance = new HashMap<>();
    private Integer totalPagesCoveringQueryCount = 0;

    @Override
    public SearchResponse getSearchResults(String query, String siteUrl, Integer offset, Integer limit) {
        log.info("Search for query '{}' started", query);
        if (query.isBlank()) {
            return SearchResponseGenerator.getEmptyQueryResponse();
        }
        List<SiteEntity> siteList = getSitesForSearch(siteUrl);
        if (siteList.stream().anyMatch(site -> !site.getStatus().equals(SiteIndexingStatus.INDEXED))) {
            return SearchResponseGenerator.getSiteIsNotIndexedResponse(siteUrl);
        }
        List<SearchData> searchData = computeSearchDataForQuery(query, siteList, offset, limit);
        log.info("Total results count for query '{}' in {} : {}",
                query, siteUrl != null ? "site " + siteUrl : "all sites", totalPagesCoveringQueryCount);
        return SearchResponseGenerator.getAllGoodResponse(searchData, totalPagesCoveringQueryCount, query);
    }

    private List<SearchData> computeSearchDataForQuery(String query,
                                                       List<SiteEntity> siteList,
                                                       Integer offset,
                                                       Integer limit) {
        Instant start = Instant.now();
        query = query.trim();
        String cleanQueryText = collectLemmasAction.cleanText(query);
        Set<String> queryLemmas = collectLemmasAction.collectLemmasFromCleanedTextWithCount(cleanQueryText).keySet();
        if (!isSameQuery(query, siteList)) {
            setCurrentValues(query, siteList);
            sortedPagesWithRelativeRelevance = getPagesSortedByRelativeRelevanceCoveringQuery(queryLemmas);
            totalPagesCoveringQueryCount = sortedPagesWithRelativeRelevance.size();
        }
        Map<PageEntity, Float> pagesForShow = getPagesToShowInResponse(offset, limit);
        List<SearchData> searchData = getSearchDataForQuery(pagesForShow, queryLemmas);
        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);
        log.info("Searching for query '{}' with offset {} complete in {} min {} sec {} ms",
                query, offset, duration.toMinutes(), duration.toSecondsPart(), duration.toMillisPart());
        return searchData;
    }

    private boolean isSameQuery(String query, List<SiteEntity> siteList) {
        return StringUtils.equalsIgnoreCase(query, currentQuery) && isSiteListsEqual(siteList, currentSiteListForSearch);
    }

    private boolean isSiteListsEqual(List<SiteEntity> siteList, List<SiteEntity> currentSiteListForSearch) {
        if (siteList.size() != currentSiteListForSearch.size()) {
            return false;
        }
        for (SiteEntity siteEntity : currentSiteListForSearch) {
            if (siteList.stream().noneMatch(site -> Objects.equals(site.getId(), siteEntity.getId()))) {
                return false;
            }
        }
        return true;
    }

    private void setCurrentValues(String query, List<SiteEntity> siteList) {
        currentQuery = query;
        currentSiteListForSearch = siteList;
    }

    private List<SiteEntity> getSitesForSearch(String siteUrl) {
        return siteUrl != null
                ? Collections.singletonList(siteService.getByUrl(siteUrl))
                : siteService.getAll();
    }

    private Map<PageEntity, Float> getPagesSortedByRelativeRelevanceCoveringQuery(Set<String> queryLemmas) {
        try {
            Map<PageEntity, Float> pagesRelevantToQueryWithAbsRelevance =
                    getPagesRelevantToQueryWithAbsRelevance(currentSiteListForSearch, queryLemmas);
            if (pagesRelevantToQueryWithAbsRelevance.isEmpty()) {
                return Collections.emptyMap();
            }
            Float maxAbsRelevance = findMaxPageRelevance(pagesRelevantToQueryWithAbsRelevance);
            return getSortedPagesWithRelativeRelevance(pagesRelevantToQueryWithAbsRelevance, maxAbsRelevance);
        } catch (InvalidSearchQueryException e) {
            ExceptionsHandler.handleInvalidSearchQueryExceptions(currentQuery);
            return Collections.emptyMap();
        }
    }

    private Map<PageEntity, Float> getPagesRelevantToQueryWithAbsRelevance(List<SiteEntity> siteList, Set<String> queryLemmas) {
        Map<PageEntity, Float> pagesRelevantToQueryWithAbsRelevance = new HashMap<>();
        for (SiteEntity site : siteList) {
            List<LemmaEntity> siteQueryLemmaEntities = getQueryLemmaEntitiesPresentInSite(queryLemmas, site);
            Integer frequencyLimit = getLemmaFrequencyLimit(site);
            List<LemmaEntity> lemmaEntitiesReadyForSearch =
                    getLemmaEntitiesFilteredAndSortedByFrequency(siteQueryLemmaEntities, frequencyLimit);
            List<PageEntity> pagesRelevantToQueryBySite =
                    getPagesRelevantToSearchQuery(lemmaEntitiesReadyForSearch, site);
            Map<PageEntity, Float> pagesWithAbsRelevanceBySite =
                    computePagesAbsRelevance(pagesRelevantToQueryBySite, lemmaEntitiesReadyForSearch);
            pagesRelevantToQueryWithAbsRelevance.putAll(pagesWithAbsRelevanceBySite);
        }
        return pagesRelevantToQueryWithAbsRelevance;
    }

    private List<LemmaEntity> getQueryLemmaEntitiesPresentInSite(Set<String> queryLemmas,
                                                                 SiteEntity site) {
        return queryLemmas.stream()
                .map(lemma -> lemmaService.getBySiteAndLemma(site, lemma))
                .filter(Objects::nonNull)
                .toList();
    }

    private int getLemmaFrequencyLimit(SiteEntity site) {
        Integer responsivePagesCount = pageService.countResponsivePagesBySite(site);
        return (int) (responsivePagesCount * LEMMA_FREQUENCY_PERCENT);
    }

    private List<LemmaEntity> getLemmaEntitiesFilteredAndSortedByFrequency(List<LemmaEntity> siteQueryLemmaEntities,
                                                                           Integer frequencyLimit) {
        return siteQueryLemmaEntities.stream()
                .filter(lemmaEntity -> lemmaEntity.getFrequency() <= frequencyLimit)
                .sorted(Comparator.comparingInt(LemmaEntity::getFrequency))
                .toList();
    }

    private List<PageEntity> getPagesRelevantToSearchQuery(List<LemmaEntity> lemmaEntitiesReadyForSearch,
                                                           SiteEntity site) {
        if (lemmaEntitiesReadyForSearch.isEmpty()) {
            return Collections.emptyList();
        }
        List<PageEntity> pagesWithRarestLemma = getPageWithRarestLemma(lemmaEntitiesReadyForSearch, site);
        List<LemmaEntity> searchLemmasWithoutRarestLemma = lemmaEntitiesReadyForSearch.stream().skip(1).toList();
        List<PageEntity> searchRelevantPages = new ArrayList<>(pagesWithRarestLemma);
        for (LemmaEntity queryLemmaEntity : searchLemmasWithoutRarestLemma) {
            List<PageEntity> searchIterationPages =
                    getRemainingSearchRelevantPages(queryLemmaEntity, searchRelevantPages);
            searchRelevantPages.retainAll(searchIterationPages);
        }
        return searchRelevantPages;
    }

    private List<PageEntity> getPageWithRarestLemma(List<LemmaEntity> lemmaEntitiesReadyForSearch, SiteEntity site) {
        LemmaEntity rarestLemmaEntity = lemmaEntitiesReadyForSearch.stream()
                .findFirst()
                .orElseThrow(InvalidSearchQueryException::new);
        log.info("Rarest lemma on site {} for current search query is '{}'",
                site.getUrl(), rarestLemmaEntity.getLemma());
        List<IndexEntity> indexesForRarestLemma = indexService.getByLemma(rarestLemmaEntity);
        return indexesForRarestLemma.stream()
                .filter(Objects::nonNull)
                .map(IndexEntity::getPage)
                .toList();
    }

    private List<PageEntity> getRemainingSearchRelevantPages(LemmaEntity queryLemmaEntity,
                                                             List<PageEntity> searchRelevantPages) {
        List<IndexEntity> indexesByLemma = indexService.getByLemma(queryLemmaEntity);
        return searchRelevantPages.stream()
                .filter(page -> indexesByLemma.stream()
                        .anyMatch(index -> Objects.equals(index.getPage().getId(), page.getId())))
                .toList();
    }

    private Map<PageEntity, Float> computePagesAbsRelevance(List<PageEntity> pagesRelevantToQuery,
                                                            List<LemmaEntity> lemmaEntitiesReadyForSearch) {
        return pagesRelevantToQuery.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        pageEntity -> computeSinglePageAbsRelevance(lemmaEntitiesReadyForSearch, pageEntity)));
    }

    private Float computeSinglePageAbsRelevance(List<LemmaEntity> lemmaEntitiesReadyForSearch,
                                                PageEntity pageEntity) {
        float pageAbsRelevance = (float) 0;
        for (LemmaEntity lemmaEntity : lemmaEntitiesReadyForSearch) {
            IndexEntity index = indexService.getByLemmaAndPage(lemmaEntity, pageEntity);
            if (index != null) {
                pageAbsRelevance += index.getRank();
            }
        }
        return pageAbsRelevance;
    }

    private Float findMaxPageRelevance(Map<PageEntity, Float> pagesWithAbsRelevance) {
        Optional<Float> maxAbsRelevance = pagesWithAbsRelevance.values().stream().max(Float::compare);
        if (maxAbsRelevance.isEmpty()) {
            log.error("Unable to calculate max page relevance for query {}", currentQuery);
            return DEFAULT_MAX_PAGE_RELEVANCE;
        }
        return maxAbsRelevance.get();
    }

    private Map<PageEntity, Float> getSortedPagesWithRelativeRelevance(Map<PageEntity, Float> pagesWithAbsRelevance,
                                                                       Float maxAbsRelevance) {
        Map<PageEntity, Float> pagesWithRelativeRelevance = pagesWithAbsRelevance.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue() / maxAbsRelevance,
                        (oldValue, newValue) -> oldValue));
        return pagesWithRelativeRelevance.entrySet().stream()
                .sorted(Map.Entry.<PageEntity, Float>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new));
    }

    private Map<PageEntity, Float> getPagesToShowInResponse(Integer offset, Integer limit) {
        if (sortedPagesWithRelativeRelevance.size() <= limit) {
            return sortedPagesWithRelativeRelevance;
        }
        return sortedPagesWithRelativeRelevance.entrySet().stream()
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new));
    }

    private List<SearchData> getSearchDataForQuery(Map<PageEntity, Float> pagesForShow, Set<String> queryLemmas) {
        if (pagesForShow == null || pagesForShow.isEmpty()) {
            return Collections.emptyList();
        }
        return getSearchDataFromRelevantPages(pagesForShow, queryLemmas);
    }

    private List<SearchData> getSearchDataFromRelevantPages(Map<PageEntity, Float> pagesWithRelativeRelevance,
                                                            Set<String> queryLemmas) {
        List<SearchData> data = new ArrayList<>();
        for (PageEntity pageEntity : pagesWithRelativeRelevance.keySet()) {
            SearchData pageSearchData = getSearchDataForPage(
                    pageEntity,
                    pagesWithRelativeRelevance.get(pageEntity),
                    queryLemmas);
            data.add(pageSearchData);
        }
        return data;
    }

    public SearchData getSearchDataForPage(PageEntity page, Float relevance, Set<String> queryLemmas) {
        return SearchData.builder()
                .site(page.getSite().getUrl())
                .siteName(page.getSite().getName())
                .uri(page.getRelativePath())
                .title(pageService.getPageTitle(page))
                .snippet(snippetAction.createSnippet(page, queryLemmas))
                .relevance(relevance)
                .build();
    }

}
