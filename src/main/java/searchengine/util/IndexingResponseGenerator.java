package searchengine.util;

import lombok.extern.log4j.Log4j2;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.SiteIndexingStatus;

@Log4j2
public class IndexingResponseGenerator {

    public static final String INDEXING_STOPPED_BY_USER_ERROR = "Индексация прервана пользователем";
    public static final String INDEXING_ALREADY_STARTED_ERROR = "Индексация уже запущена";
    public static final String INDEXING_NOT_STARTED_ERROR = "Индексация не запущена";
    public static final String SITE_HOME_PAGE_NOT_ACCESSIBLE = "Главная страница сайта недоступна";
    public static final String PAGE_NOT_LISTED_IN_CONFIG = "Данная страница находится за пределами сайтов, указанных в конфигурационном файле";

    private static final IndexingResponse INDEXING_ALREADY_STARTED_RESPONSE =
            IndexingResponse.buildErrorIndexingResponse(INDEXING_ALREADY_STARTED_ERROR);
    private static final IndexingResponse INDEXING_NOT_STARTED_RESPONSE =
            IndexingResponse.buildErrorIndexingResponse(INDEXING_NOT_STARTED_ERROR);
    private static final IndexingResponse SITE_HOME_PAGE_NOT_ACCESSIBLE_RESPONSE =
            IndexingResponse.buildErrorIndexingResponse(SITE_HOME_PAGE_NOT_ACCESSIBLE);
    private static final IndexingResponse PAGE_NOT_LISTED_IN_CONFIG_RESPONSE =
            IndexingResponse.buildErrorIndexingResponse(PAGE_NOT_LISTED_IN_CONFIG);
    private static final IndexingResponse GOOD_INDEXING_RESPONSE = IndexingResponse.buildGoodIndexingResponse();

    public static IndexingResponse getIndexingAlreadyStartedResponse() {
        log.error("Attempt to start indexing was made while there was already indexing in process");
        return INDEXING_ALREADY_STARTED_RESPONSE;
    }

    public static IndexingResponse getIndexingStoppedByUserResponse() {
        log.error("Indexing was stopped by user");
        return GOOD_INDEXING_RESPONSE;
    }

    public static IndexingResponse getIndexingNotStartedResponse() {
        log.error("Attempt to stop indexing was made while indexing was not in process");
        return INDEXING_NOT_STARTED_RESPONSE;
    }

    public static IndexingResponse getSiteHomePageNotAccessibleResponse(String siteUrl) {
        log.error("Home page for site {} is not accessible", siteUrl);
        return SITE_HOME_PAGE_NOT_ACCESSIBLE_RESPONSE;
    }

    public static IndexingResponse getPageNotListedInConfigResponse(String pageUrl) {
        log.error("Page with url {} is not part of site list in configuration file", pageUrl);
        return PAGE_NOT_LISTED_IN_CONFIG_RESPONSE;
    }

    public static IndexingResponse getAllGoodResponse() {
        return GOOD_INDEXING_RESPONSE;
    }

    public static IndexingResponse getIndexingFailedErrorResponse(String siteUrl, String indexingError) {
        String errorMessage = "Ошибка индексации: сайт - " + siteUrl + "\n" + indexingError;
        IndexingResponse result = IndexingResponse.buildErrorIndexingResponse(errorMessage);
        log.error("Indexing completed with status {} for site {} with error '{}'",
                SiteIndexingStatus.FAILED,
                siteUrl,
                indexingError);
        return result;
    }

}
