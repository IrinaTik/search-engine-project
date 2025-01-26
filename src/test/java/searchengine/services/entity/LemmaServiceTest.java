package searchengine.services.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.LemmaRepository;
import searchengine.services.entity.impl.LemmaServiceImpl;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LemmaServiceTest {

    private final LemmaRepository lemmaRepository = Mockito.mock(LemmaRepository.class);
    private final LemmaService lemmaService = new LemmaServiceImpl(lemmaRepository);

    @Test
    @DisplayName("Increase lemma frequency by 1 when it is present in database")
    public void testIncreaseLemmaFrequencyBySiteWhenLemmaIsPresent() {
        SiteEntity site = new SiteEntity();
        site.setUrl("https://test-site-url");
        String lemma = "lemma";
        int frequency = 5;
        LemmaEntity lemmaEntity = new LemmaEntity();
        lemmaEntity.setLemma(lemma);
        lemmaEntity.setFrequency(frequency);
        lemmaEntity.setSite(site);
        Mockito.when(lemmaRepository.findBySiteAndLemma(site, lemma)).thenReturn(Optional.of(lemmaEntity));
        LemmaEntity correctedLemmaEntity = lemmaService.increaseLemmaFrequencyBySite(lemma, site);
        assertEquals(lemma, correctedLemmaEntity.getLemma());
        assertEquals(site.getUrl(), correctedLemmaEntity.getSite().getUrl());
        assertEquals(frequency + 1, correctedLemmaEntity.getFrequency());
    }

    @Test
    @DisplayName("Create lemma entity with frequency 1 when it is not present in database")
    public void testIncreaseLemmaFrequencyBySiteWhenLemmaIsAbsent() {
        SiteEntity site = new SiteEntity();
        site.setUrl("https://test-site-url");
        String lemma = "lemma";
        int defaultFrequency = 1;
        Mockito.when(lemmaRepository.findBySiteAndLemma(site, lemma)).thenReturn(Optional.empty());
        LemmaEntity correctedLemmaEntity = lemmaService.increaseLemmaFrequencyBySite(lemma, site);
        assertEquals(lemma, correctedLemmaEntity.getLemma());
        assertEquals(site.getUrl(), correctedLemmaEntity.getSite().getUrl());
        assertEquals(defaultFrequency, correctedLemmaEntity.getFrequency());
    }

    @Test
    @DisplayName("Delete lemma entity with frequency 1 from database")
    public void testDecreaseLemmaFrequencyInDatabaseWhenFrequencyIsDefault() {
        SiteEntity site = new SiteEntity();
        site.setUrl("https://test-site-url");
        String lemma = "lemma";
        int defaultFrequency = 1;
        LemmaEntity lemmaEntity = new LemmaEntity();
        lemmaEntity.setLemma(lemma);
        lemmaEntity.setFrequency(defaultFrequency);
        lemmaEntity.setSite(site);
        lemmaService.decreaseLemmaFrequencyInDatabase(lemmaEntity);
        Mockito.verify(lemmaRepository, Mockito.times(1)).delete(lemmaEntity);
    }

    @Test
    @DisplayName("Decrease lemma entity frequency by 1 in database")
    public void testDecreaseLemmaFrequencyInDatabaseWhenFrequencyIsMoreThenDefault() {
        SiteEntity site = new SiteEntity();
        site.setUrl("https://test-site-url");
        String lemma = "lemma";
        int frequency = 5;
        LemmaEntity lemmaEntity = new LemmaEntity();
        lemmaEntity.setLemma(lemma);
        lemmaEntity.setFrequency(frequency);
        lemmaEntity.setSite(site);
        lemmaService.decreaseLemmaFrequencyInDatabase(lemmaEntity);
        Mockito.verify(lemmaRepository, Mockito.times(1)).saveAndFlush(lemmaEntity);
        assertEquals(frequency - 1, lemmaEntity.getFrequency());
    }

}
