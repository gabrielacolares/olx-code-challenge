package br.com.olx.anuncios.repository;

import br.com.olx.anuncios.model.Ads;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdsRepository extends JpaRepository<Ads, UUID> {
    @Override
    @EntityGraph(attributePaths = "images")
    java.util.Optional<Ads> findById(UUID id);

    Page<Ads> findByCategoryIgnoreCase(String category, Pageable pageable);

    @Query("select distinct ads from Ads ads left join fetch ads.images where ads.id in :ids")
    List<Ads> findWithImagesByIds(@Param("ids") Collection<UUID> ids);
}
