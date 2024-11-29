package searchengine.services.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import searchengine.model.SiteEntity;
import searchengine.model.SiteIndexingStatus;
import searchengine.repository.SiteRepository;
import searchengine.services.entity.impl.SiteServiceImpl;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static searchengine.model.SiteIndexingStatus.FAILED;
import static searchengine.model.SiteIndexingStatus.INDEXING;

public class SiteServiceTest {

    private final SiteRepository siteRepository = Mockito.mock(SiteRepository.class);
    private final SiteService siteService = new SiteServiceImpl(siteRepository);

    @Test
    @DisplayName("Save site")
    public void testSave() {
        SiteEntity site = new SiteEntity();
        String url = "https://test-site-url";
        site.setUrl(url);
        when(siteRepository.saveAndFlush(site)).thenReturn(site);
        SiteEntity siteFromService = siteService.save(site);
        assertEquals(url, siteFromService.getUrl());
        verify(siteRepository, times(1)).saveAndFlush(site);
    }

    @Test
    @DisplayName("Create site by name and url")
    public void testCreateSiteByNameAndUrl() {
        String url = "https://test-site-url";
        String name = "Test site";
        SiteIndexingStatus status = INDEXING;
        LocalDateTime statusTime = LocalDateTime.now();
        String lastError = "";
        SiteEntity siteFromService = siteService.createSiteByNameAndUrl(name, url);
        assertEquals(name, siteFromService.getName());
        assertEquals(url, siteFromService.getUrl());
        assertEquals(status, siteFromService.getStatus());
        assertEquals(statusTime, siteFromService.getStatusTime());
        assertEquals(lastError, siteFromService.getLastError());
    }

    @Test
    @DisplayName("Update site status info when info is valid and none is null")
    public void testUpdateSiteStatusInfoWithoutNullValue() {
        LocalDateTime midnightOfFirstJanuary1999 = LocalDateTime.of(1999, 1, 1, 0, 0, 0);
        String oldError = "some error";
        String siteUrl = "https://test-site-url";
        SiteIndexingStatus oldStatus = INDEXING;
        SiteEntity site = new SiteEntity();
        site.setUrl(siteUrl);
        site.setStatus(oldStatus);
        site.setStatusTime(midnightOfFirstJanuary1999);
        site.setLastError(oldError);
        String newError = "very serious error";
        SiteIndexingStatus newStatus = FAILED;
        siteService.updateSiteStatusInfo(newStatus, newError, site);
        assertEquals(newStatus, site.getStatus());
        assertTrue(isStatusTimeAroundNow(site.getStatusTime()));
        assertEquals(newError, site.getLastError());
    }

    @Test
    @DisplayName("Update site status info when last_error is null")
    public void testUpdateSiteStatusInfoWithNullValue() {
        String newNullError = null;
        testUpdateSiteStatusInfo(newNullError);
    }

    @Test
    @DisplayName("Update site status info when last_error is empty")
    public void testUpdateSiteStatusInfoWithEmptyValue() {
        String newEmptyError = "";
        testUpdateSiteStatusInfo(newEmptyError);
    }

    private void testUpdateSiteStatusInfo(String newError) {
        LocalDateTime midnightOfFirstJanuary1999 = LocalDateTime.of(1999, 1, 1, 0, 0, 0);
        String siteUrl = "https://test-site-url";
        SiteIndexingStatus oldStatus = INDEXING;
        String oldError = "some error";
        SiteEntity site = new SiteEntity();
        site.setUrl(siteUrl);
        site.setStatus(oldStatus);
        site.setStatusTime(midnightOfFirstJanuary1999);
        site.setLastError(oldError);
        SiteIndexingStatus newStatus = FAILED;
        siteService.updateSiteStatusInfo(newStatus, newError, site);
        assertEquals(newStatus, site.getStatus());
        assertTrue(isStatusTimeAroundNow(site.getStatusTime()));
        assertEquals(oldError, site.getLastError());
    }

    private static boolean isStatusTimeAroundNow(LocalDateTime siteStatusTime) {
        LocalDateTime now = LocalDateTime.now();
        return siteStatusTime.isAfter(now.minusSeconds(5)) || siteStatusTime.isEqual(now);
    }

}
