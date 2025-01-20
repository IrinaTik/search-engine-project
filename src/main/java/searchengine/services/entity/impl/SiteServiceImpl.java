package searchengine.services.entity.impl;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import searchengine.model.SiteEntity;
import searchengine.model.SiteIndexingStatus;
import searchengine.repository.SiteRepository;
import searchengine.services.entity.SiteService;

import java.time.LocalDateTime;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class SiteServiceImpl implements SiteService {

    private final SiteRepository siteRepository;

    @Override
    public SiteEntity getByUrl(String url) {
        return siteRepository.findByUrl(url).orElse(null);
    }

    @Override
    public List<SiteEntity> getAll() {
        return siteRepository.findAll();
    }

    @Override
    public SiteEntity save(SiteEntity site) {
        return siteRepository.saveAndFlush(site);
    }

    @Override
    public void deleteAll() {
        siteRepository.deleteAllInBatch();
    }

    @Override
    public SiteEntity createSiteByNameAndUrl(String name, String url) {
        SiteEntity site = new SiteEntity();
        site.setName(name);
        site.setUrl(url);
        site.setStatus(SiteIndexingStatus.INDEXING);
        site.setStatusTime(LocalDateTime.now());
        site.setLastError("");
        return site;
    }

    @Override
    public void updateSiteStatusInfo(SiteIndexingStatus status,
                                     @Nullable String error,
                                     SiteEntity site) {
        site.setStatus(status);
        site.setStatusTime(LocalDateTime.now());
        if (error != null && !error.isEmpty()) {
            site.setLastError(error);
        }
    }

}
