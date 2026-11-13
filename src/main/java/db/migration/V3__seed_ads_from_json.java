package db.migration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class V3__seed_ads_from_json extends BaseJavaMigration {
    private static final String SEED_RESOURCE = "seed/ads.json";
    private static final String INSERT_AD = "INSERT INTO ads "
            + "(id, title, description, price, category, status, rejection_reason, created_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    @Override
    public void migrate(Context context) throws Exception {
        JsonNode ads = readAds();
        Connection connection = context.getConnection();

        try (PreparedStatement insertAd = connection.prepareStatement(INSERT_AD)) {
            for (JsonNode ad : ads) {
                long sourceId = ad.path("id").longValue();
                if (sourceId <= 0) {
                    throw new IllegalStateException("Seed ad id must be a positive integer");
                }

                UUID id = UUID.nameUUIDFromBytes(
                        ("olx-code-challenge-seed-ad:" + sourceId).getBytes(StandardCharsets.UTF_8));

                insertAd.setObject(1, id);
                insertAd.setString(2, requiredText(ad, "title"));
                insertAd.setString(3, requiredText(ad, "description"));
                insertAd.setBigDecimal(4, ad.path("price").decimalValue());
                insertAd.setString(5, requiredText(ad, "category"));
                insertAd.setString(6, "APPROVED");
                insertAd.setNull(7, Types.VARCHAR);
                insertAd.setObject(8, OffsetDateTime.parse(requiredText(ad, "created_at")));
                insertAd.executeUpdate();
            }
        }
    }

    private JsonNode readAds() throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(SEED_RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Seed resource not found: " + SEED_RESOURCE);
            }

            JsonNode ads = new ObjectMapper().readTree(input).path("ads");
            if (!ads.isArray()) {
                throw new IllegalStateException("Seed resource must contain an ads array");
            }

            return ads;
        }
    }

    private static String requiredText(JsonNode ad, String field) {
        JsonNode value = ad.path(field);
        if (value.isMissingNode() || value.isNull() || value.asText().isBlank()) {
            throw new IllegalStateException("Seed ad is missing required field: " + field);
        }

        return value.asText();
    }
}
