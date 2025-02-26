package searchengine.exceptions;

import searchengine.util.IndexingResponseGenerator;

public class IndexingStoppedByUserException extends RuntimeException {
    public IndexingStoppedByUserException() {
        super(IndexingResponseGenerator.INDEXING_STOPPED_BY_USER_ERROR);
    }
}
