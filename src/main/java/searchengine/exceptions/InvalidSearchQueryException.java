package searchengine.exceptions;

public class InvalidSearchQueryException extends RuntimeException {
    public InvalidSearchQueryException() {
    }

    public InvalidSearchQueryException(String query) {
        super("Unable to extract lemmas from current search query : " + query);
    }
}
