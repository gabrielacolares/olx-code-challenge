package br.com.olx.anuncios.migration;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.olx.anuncios.model.Ads;
import br.com.olx.anuncios.model.AdsStatus;
import br.com.olx.anuncios.repository.AdsRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.datasource.url=jdbc:h2:mem:ads_seed_migration;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@ActiveProfiles("test")
class AdsSeedMigrationTest {
    @Autowired
    private AdsRepository adsRepository;

    @Autowired
    private Flyway flyway;

    @Test
    void importsTheSeedOnceAndPreservesItsData() {
        assertThat(adsRepository.count()).isEqualTo(57);

        UUID firstSeedId = UUID.nameUUIDFromBytes("olx-code-challenge-seed-ad:1".getBytes(StandardCharsets.UTF_8));
        Ads firstAd = adsRepository.findById(firstSeedId).orElseThrow();

        assertThat(firstAd.getTitle()).isEqualTo("iPhone 13 128GB");
        assertThat(firstAd.getPrice()).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(firstAd.getCategory()).isEqualTo("eletronicos");
        assertThat(firstAd.getStatus()).isEqualTo(AdsStatus.APPROVED);
        assertThat(firstAd.getCreatedAt()).isEqualTo(Instant.parse("2026-06-03T12:00:00Z"));
        assertThat(firstAd.getImages()).isEmpty();

        flyway.migrate();

        assertThat(adsRepository.count()).isEqualTo(57);
    }
}
