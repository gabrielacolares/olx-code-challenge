package br.com.olx.anuncios.repository;

import br.com.olx.anuncios.model.Ads;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.UUID;

public interface AdsRepository extends JpaRepository<Ads, UUID> {
    @Override
    @EntityGraph(attributePaths = "images")
    java.util.Optional<Ads> findById(UUID id);
}

