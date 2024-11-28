package searchengine.exceptions;

import searchengine.services.actions.GenerateIndexingResponseAction;

public class IndexingStoppedByUserException extends RuntimeException {
    public IndexingStoppedByUserException() {
        super(GenerateIndexingResponseAction.INDEXING_STOPPED_BY_USER_ERROR);
    }
}
