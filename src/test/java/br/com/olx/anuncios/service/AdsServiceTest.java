package br.com.olx.anuncios.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import br.com.olx.anuncios.integration.moderation.ModerationClient;
import br.com.olx.anuncios.model.Ads;
import br.com.olx.anuncios.model.AdsStatus;
import br.com.olx.anuncios.model.ModerationDecision;
import br.com.olx.anuncios.repository.AdsRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AdsServiceTest {
    private AdsRepository repository;
    private ModerationClient moderation;
    private AdsService service;

    @BeforeEach
    void setUp() {
        repository = mock(AdsRepository.class);
        moderation = mock(ModerationClient.class);

        service = new AdsService();

        ReflectionTestUtils.setField(service, "adsRepository", repository);
        ReflectionTestUtils.setField(service, "moderationClient", moderation);
    }

    @Test
    void createsAndPersistsApprovedAds() {
        when(moderation.moderate(any(UUID.class), anyString(), anyString(), any(), anyString()))
                .thenReturn(new ModerationDecision(AdsStatus.APPROVED, null));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Ads ads = createValidAds();

        assertEquals(AdsStatus.APPROVED, ads.getStatus());
        assertNull(ads.getRejectionReason());
        verify(moderation)
                .moderate(
                        any(UUID.class),
                        eq("Bicicleta urbana aro 29"),
                        eq("Bicicleta em \u00f3timo estado"),
                        eq(new BigDecimal("850.00")),
                        eq("esportes"));
        verify(repository).save(ads);
    }

    @Test
    void createsAndPersistsRejectedAdsWithReason() {
        when(moderation.moderate(any(UUID.class), anyString(), anyString(), any(), anyString()))
                .thenReturn(new ModerationDecision(AdsStatus.REJECTED, "prohibited_item"));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Ads ads = createValidAds();

        assertEquals(AdsStatus.REJECTED, ads.getStatus());
        assertEquals("prohibited_item", ads.getRejectionReason());
        verify(repository).save(ads);
    }

    @Test
    void rejectsPriceThatIsZeroOrNegativeBeforeModeration() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(
                        "Bicicleta urbana aro 29", "Descri\u00e7\u00e3o", BigDecimal.ZERO, "esportes", validImages()));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(
                        "Bicicleta urbana aro 29",
                        "Descri\u00e7\u00e3o",
                        new BigDecimal("-1"),
                        "esportes",
                        validImages()));
        verifyNoInteractions(moderation, repository);
    }

    @Test
    void rejectsTitleOutsideTenToOneHundredCharactersBeforeModeration() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.create("Curto", "Descri\u00e7\u00e3o", new BigDecimal("10"), "esportes", validImages()));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(
                        "A".repeat(101), "Descri\u00e7\u00e3o", new BigDecimal("10"), "esportes", validImages()));
        verifyNoInteractions(moderation, repository);
    }

    @Test
    void rejectsMissingImagesBeforeModeration() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(
                        "Bicicleta urbana aro 29", "Descri\u00e7\u00e3o", new BigDecimal("10"), "esportes", List.of()));
        verifyNoInteractions(moderation, repository);
    }

    @Test
    void doesNotPersistWhenModerationFails() {
        when(moderation.moderate(any(UUID.class), anyString(), anyString(), any(), anyString()))
                .thenThrow(new IllegalStateException("servi\u00e7o indispon\u00edvel"));

        assertThrows(IllegalStateException.class, this::createValidAds);

        verify(moderation).moderate(any(UUID.class), anyString(), anyString(), any(), anyString());
        verifyNoInteractions(repository);
    }

    @Test
    void findsAdById() {
        UUID id = UUID.fromString("bb885686-0612-4d32-b8db-2d3dd519c2fd");
        Ads expected = new Ads(
                id,
                "Bicicleta urbana aro 29",
                "Em ótimo estado",
                new BigDecimal("850.00"),
                "esportes",
                validImages(),
                AdsStatus.APPROVED,
                null);
        when(repository.findById(id)).thenReturn(Optional.of(expected));

        Optional<Ads> result = service.findById(id);

        assertTrue(result.isPresent());
        assertSame(expected, result.orElseThrow());
        verify(repository).findById(id);
        verifyNoInteractions(moderation);
    }

    @Test
    void returnsEmptyWhenAdIdDoesNotExist() {
        UUID id = UUID.fromString("5a597312-ce13-4f9c-a091-742878ac45a9");
        when(repository.findById(id)).thenReturn(Optional.empty());

        Optional<Ads> result = service.findById(id);

        assertTrue(result.isEmpty());
        verify(repository).findById(id);
        verifyNoInteractions(moderation);
    }

    private Ads createValidAds() {
        return service.create(
                "Bicicleta urbana aro 29",
                "Bicicleta em \u00f3timo estado",
                new BigDecimal("850.00"),
                "esportes",
                validImages());
    }

    private List<String> validImages() {
        return List.of("https://img.test/ad.jpg");
    }
}
