package searchengine.services.api.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteEntity;
import searchengine.model.SiteIndexingStatus;
import searchengine.services.api.StatisticsService;
import searchengine.services.entity.LemmaService;
import searchengine.services.entity.PageService;
import searchengine.services.entity.SiteService;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SiteService siteService;
    private final PageService pageService;
    private final LemmaService lemmaService;

    @Override
    public StatisticsResponse getStatistics() {
        log.info("Statistics request is initiated");
        StatisticsResponse response = new StatisticsResponse();
        try {
            response.setStatistics(getStatisticsData());
            response.setResult(true);
        } catch (Exception ex) {
            response.setResult(false);
            log.error("Error while getting statistics");
        }
        log.info("Statistics request is completed");
        return response;
    }

    private StatisticsData getStatisticsData() {
        StatisticsData statisticsData = new StatisticsData();
        List<SiteEntity> sites = siteService.getAll();
        statisticsData.setTotal(getStatisticsTotalInfo(sites));
        statisticsData.setDetailed(getStatisticsDetailedInfo(sites));
        return statisticsData;
    }

    private TotalStatistics getStatisticsTotalInfo(List<SiteEntity> sites) {
        TotalStatistics totalInfo = new TotalStatistics();
        totalInfo.setSites(sites.size());
        totalInfo.setPages(pageService.countAll().intValue());
        totalInfo.setLemmas(lemmaService.countAll().intValue());
        totalInfo.setIndexing(sites.stream().anyMatch(site -> site.getStatus().equals(SiteIndexingStatus.INDEXING)));
        return totalInfo;
    }

    private List<DetailedStatisticsItem> getStatisticsDetailedInfo(List<SiteEntity> sites) {
        List<DetailedStatisticsItem> detailedInfoList = new ArrayList<>();
        for (SiteEntity site : sites) {
            DetailedStatisticsItem detailedInfo = DetailedStatisticsItem.builder()
                    .url(site.getUrl())
                    .name(site.getName())
                    .status(site.getStatus().name())
                    .statusTime(site.getStatusTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                    .error(site.getLastError())
                    .pages(pageService.countBySite(site))
                    .lemmas(lemmaService.countBySite(site))
                    .build();
            detailedInfoList.add(detailedInfo);
        }
        return detailedInfoList;
    }

}
