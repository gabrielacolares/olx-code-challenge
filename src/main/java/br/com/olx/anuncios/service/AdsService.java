package br.com.olx.anuncios.service;

import br.com.olx.anuncios.integration.ModerationClient;
import br.com.olx.anuncios.model.Ads;
import br.com.olx.anuncios.model.AdsPage;
import br.com.olx.anuncios.model.ModerationDecision;
import br.com.olx.anuncios.repository.AdsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdsService {
    private static final Logger log = LoggerFactory.getLogger(AdsService.class);
    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private AdsRepository adsRepository;

    @Autowired
    private ModerationClient moderationClient;

    public Optional<Ads> findById(UUID id) {
        return adsRepository.findById(id);
    }

    public AdsPage list(int page, int size, String category) {

        if (page < 0) {
            throw new IllegalArgumentException("pagina deve ser maior ou igual a zero");
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("tamanho da pagina deve estar entre 1 e " + MAX_PAGE_SIZE);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));

        Page<Ads> result = category == null || category.isBlank()
                ? adsRepository.findAll(pageable)
                : adsRepository.findByCategoryIgnoreCase(category, pageable);

        List<UUID> ids = result.getContent().stream().map(Ads::getId).toList();

        List<Ads> adsWithImages = ids.isEmpty() ? List.of() : adsRepository.findWithImagesByIds(ids);

        Map<UUID, Ads> adsById = adsWithImages.stream().collect(Collectors.toMap(Ads::getId, Function.identity()));

        List<Ads> orderedAds = ids.stream().map(adsById::get).toList();

        return new AdsPage(orderedAds, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    public Ads create(String title, String description, BigDecimal price, String category, List<String> images) {

        Ads ads = new Ads(UUID.randomUUID(), title, description, price, category, images);
        ads.validate();

        ModerationDecision decision = moderationClient.moderate(
                ads.getId(), ads.getTitle(), ads.getDescription(), ads.getPrice(), ads.getCategory());

        ads.applyModerationDecision(decision.getStatus(), decision.getRejectionReason());
        try {
            return adsRepository.save(ads);

        } catch (RuntimeException exception) {

            log.error("operation=persist_after_moderation adId={} moderationStatus={} outcome=failure exceptionType={}",
                    ads.getId(), decision.getStatus(), exception.getClass().getSimpleName());

            throw exception;
        }
    }
}