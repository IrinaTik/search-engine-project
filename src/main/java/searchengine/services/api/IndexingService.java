package searchengine.services.api;

import searchengine.dto.indexing.IndexingResponse;

public interface IndexingService {
    IndexingResponse initiateFullIndexing();

    IndexingResponse initiatePartialIndexing(String url);

    IndexingResponse stopIndexing();
}
