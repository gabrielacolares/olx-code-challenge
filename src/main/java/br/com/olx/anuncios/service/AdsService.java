package br.com.olx.anuncios.service;

import br.com.olx.anuncios.model.Ads;
import br.com.olx.anuncios.integration.ModerationClient;
import br.com.olx.anuncios.model.ModerationDecision;
import br.com.olx.anuncios.repository.AdsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class AdsService {
    @Autowired
    private AdsRepository adsRepository;

    @Autowired
    private ModerationClient moderationClient;

    public Ads create(String title, String description, BigDecimal price, String category, List<String> images) {
        Ads ads = new Ads(UUID.randomUUID(), title, description, price, category, images);
        ads.validate();

        ModerationDecision decision = moderationClient.moderate(
                ads.getTitle(),
                ads.getDescription(),
                ads.getPrice(),
                ads.getCategory());

        ads.applyModerationDecision(decision.getStatus(), decision.getRejectionReason());
        return adsRepository.save(ads);
    }
}
