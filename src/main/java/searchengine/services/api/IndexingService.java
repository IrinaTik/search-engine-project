package searchengine.services.api;

import searchengine.dto.indexing.IndexingResponse;

public interface IndexingService {
    IndexingResponse indexingAllSites();

    IndexingResponse indexingAddedPage();

    IndexingResponse stopIndexing();
}
