package searchengine.services.actions;

import lombok.extern.log4j.Log4j2;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.SiteIndexingStatus;

@Log4j2
public class GenerateIndexingResponseAction {

    public static final String INDEXING_STOPPED_BY_USER_ERROR = "Индексация прервана пользователем";
    public static final String INDEXING_ALREADY_STARTED_ERROR = "Индексация уже запущена";
    public static final String INDEXING_NOT_STARTED_ERROR = "Индексация не запущена";
    public static final String SITE_HOME_PAGE_NOT_ACCESSIBLE = "Главная страница сайта недоступна";

    private static final IndexingResponse INDEXING_ALREADY_STARTED_RESPONSE = new IndexingResponse(false, INDEXING_ALREADY_STARTED_ERROR);
    private static final IndexingResponse INDEXING_NOT_STARTED_RESPONSE = new IndexingResponse(false, INDEXING_NOT_STARTED_ERROR);
    private static final IndexingResponse SITE_HOME_PAGE_NOT_ACCESSIBLE_RESPONSE = new IndexingResponse(false, SITE_HOME_PAGE_NOT_ACCESSIBLE);
    private static final IndexingResponse GOOD_INDEXING_RESPONSE = new IndexingResponse(true, "");

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

    public static IndexingResponse getAllGoodResponse() {
        return GOOD_INDEXING_RESPONSE;
    }

    public static IndexingResponse getIndexingFailedErrorResponse(String siteUrl, String error) {
        IndexingResponse result = new IndexingResponse();
        result.setResult(false);
        result.setError("Ошибка индексации: сайт - " + siteUrl + "\n" + error);
        log.error("Indexing completed with status {} for site {} with error '{}'",
                SiteIndexingStatus.FAILED,
                siteUrl,
                error);
        return result;
    }

}
