package searchengine.services.entity;

import jakarta.annotation.Nullable;
import searchengine.model.SiteEntity;
import searchengine.model.SiteIndexingStatus;

import java.util.List;

public interface SiteService {

    SiteEntity getByUrl(String url);

    List<SiteEntity> getAll();

    SiteEntity save(SiteEntity site);

    void deleteAll();

    SiteEntity createSiteByNameAndUrl(String name, String url);

    void updateSiteStatusInfo(SiteIndexingStatus status,
                              @Nullable String error,
                              SiteEntity site);
}
