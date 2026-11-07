package com.jaldishop.backend.payment.infrastructure.client.mercadopago.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Pruebas de serialización Jackson para DTOs de Mercado Pago Orders API y Preferences")
class MpOrderSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MpOrderRequest.MpOrderConfig buildConfig(String success, String failure, String pending) {
        String successUrl = StringUtils.hasText(success) ? success.trim() : null;
        String failureUrl = StringUtils.hasText(failure) ? failure.trim() : null;
        String pendingUrl = StringUtils.hasText(pending) ? pending.trim() : null;

        if (successUrl != null || failureUrl != null || pendingUrl != null) {
            return new MpOrderRequest.MpOrderConfig(
                    new MpOrderRequest.MpOnlineConfig(
                            successUrl,
                            failureUrl,
                            pendingUrl,
                            "approved"
                    )
            );
        }
        return null;
    }

    private MpOrderRequest createBaseRequest(MpOrderRequest.MpOrderConfig config) {
        MpOrderRequest.MpOrderItem item = new MpOrderRequest.MpOrderItem(
                "Café Espresso",
                "Taza 250ml",
                1,
                new BigDecimal("1.00")
        );

        return new MpOrderRequest(
                "online",
                "manual",
                "automatic",
                new BigDecimal("1.00"),
                "REF-TEST-001",
                List.of(item),
                null,
                config
        );
    }

    @Test
    @DisplayName("MpOrderRequest serializa total_amount e items[].unit_price como strings decimales")
    void shouldSerializeMonetaryFieldsAsStringsInOrdersApi() throws Exception {
        BigDecimal unitPrice = new BigDecimal("1.00");
        int quantity = 2;
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity)); // 2.00

        MpOrderRequest.MpOrderItem item = new MpOrderRequest.MpOrderItem(
                "Café Espresso",
                "Taza 250ml",
                quantity,
                unitPrice
        );

        MpOrderRequest request = new MpOrderRequest(
                "online",
                "manual",
                "automatic",
                totalAmount,
                "REF-TEST-001",
                List.of(item),
                null,
                null
        );

        String json = objectMapper.writeValueAsString(request);
        JsonNode root = objectMapper.readTree(json);

        // Validar total_amount como nodo de texto
        JsonNode totalAmountNode = root.get("total_amount");
        assertThat(totalAmountNode).isNotNull();
        assertThat(totalAmountNode.isTextual()).as("total_amount debe ser serializado como String").isTrue();
        assertThat(totalAmountNode.asText()).isEqualTo("2.00");

        // Validar items[0].unit_price como nodo de texto
        JsonNode itemNode = root.get("items").get(0);
        JsonNode unitPriceNode = itemNode.get("unit_price");
        assertThat(unitPriceNode).isNotNull();
        assertThat(unitPriceNode.isTextual()).as("unit_price debe ser serializado como String").isTrue();
        assertThat(unitPriceNode.asText()).isEqualTo("1.00");

        // Validar que la sumatoria coincide matemáticamente con total_amount
        BigDecimal calculatedSum = new BigDecimal(unitPriceNode.asText())
                .multiply(BigDecimal.valueOf(itemNode.get("quantity").asInt()));
        assertThat(calculatedSum).isEqualByComparingTo(new BigDecimal(totalAmountNode.asText()));
    }

    @Test
    @DisplayName("Items en Orders API NO contienen 'id' ni 'currency_id', y conservan solo campos permitidos")
    void shouldNotContainIdNorCurrencyIdInItems() throws Exception {
        MpOrderRequest.MpOrderItem item = new MpOrderRequest.MpOrderItem(
                "Torta de Chocolate Artesanal",
                "Torta Entera (8-10 porciones)",
                1,
                new BigDecimal("45.00")
        );

        MpOrderRequest request = new MpOrderRequest(
                "online",
                "manual",
                "automatic",
                new BigDecimal("45.00"),
                "ORDER-123",
                List.of(item),
                null,
                null
        );

        String json = objectMapper.writeValueAsString(request);
        JsonNode root = objectMapper.readTree(json);
        JsonNode itemNode = root.get("items").get(0);

        assertThat(itemNode.has("id")).as("Items en Orders API no deben contener 'id'").isFalse();
        assertThat(itemNode.has("currency_id")).as("Items en Orders API no deben contener 'currency_id'").isFalse();
        assertThat(itemNode.has("title")).isTrue();
        assertThat(itemNode.get("title").asText()).isEqualTo("Torta de Chocolate Artesanal");
        assertThat(itemNode.has("description")).isTrue();
        assertThat(itemNode.get("description").asText()).isEqualTo("Torta Entera (8-10 porciones)");
        assertThat(itemNode.has("quantity")).isTrue();
        assertThat(itemNode.get("quantity").asInt()).isEqualTo(1);
        assertThat(itemNode.has("unit_price")).isTrue();
        assertThat(itemNode.get("unit_price").asText()).isEqualTo("45.00");
    }

    @Test
    @DisplayName("Payer con last_name null -> se omite last_name del JSON")
    void shouldOmitLastNameWhenNull() throws Exception {
        MpOrderRequest.MpOrderPayer payer = new MpOrderRequest.MpOrderPayer(
                "Valeria",
                null,
                "valeria@example.com",
                null
        );

        MpOrderRequest request = new MpOrderRequest(
                "online", "manual", "automatic", new BigDecimal("45.00"), "ORDER-1",
                List.of(new MpOrderRequest.MpOrderItem("Torta", null, 1, new BigDecimal("45.00"))),
                payer, null
        );

        String json = objectMapper.writeValueAsString(request);
        JsonNode root = objectMapper.readTree(json);
        JsonNode payerNode = root.get("payer");

        assertThat(payerNode).isNotNull();
        assertThat(payerNode.has("last_name")).as("last_name null debe ser omitido").isFalse();
        assertThat(payerNode.get("first_name").asText()).isEqualTo("Valeria");
        assertThat(payerNode.get("email").asText()).isEqualTo("valeria@example.com");
    }

    @Test
    @DisplayName("Payer con last_name vacío normalizado a null -> se omite last_name del JSON")
    void shouldOmitLastNameWhenNormalizedFromEmptyString() throws Exception {
        String rawLastName = "";
        String normalizedLastName = StringUtils.hasText(rawLastName) ? rawLastName.trim() : null;

        MpOrderRequest.MpOrderPayer payer = new MpOrderRequest.MpOrderPayer(
                "Valeria Ramos",
                normalizedLastName,
                "valeria@example.com",
                null
        );

        MpOrderRequest request = new MpOrderRequest(
                "online", "manual", "automatic", new BigDecimal("45.00"), "ORDER-1",
                List.of(new MpOrderRequest.MpOrderItem("Torta", null, 1, new BigDecimal("45.00"))),
                payer, null
        );

        String json = objectMapper.writeValueAsString(request);
        JsonNode root = objectMapper.readTree(json);
        JsonNode payerNode = root.get("payer");

        assertThat(payerNode).isNotNull();
        assertThat(payerNode.has("last_name")).as("last_name vacío normalizado a null debe ser omitido").isFalse();
        assertThat(payerNode.get("first_name").asText()).isEqualTo("Valeria Ramos");
    }

    @Test
    @DisplayName("Payer con last_name compuesto solo por espacios normalizado a null -> se omite del JSON")
    void shouldOmitLastNameWhenNormalizedFromWhitespace() throws Exception {
        String rawLastName = "    ";
        String normalizedLastName = StringUtils.hasText(rawLastName) ? rawLastName.trim() : null;

        MpOrderRequest.MpOrderPayer payer = new MpOrderRequest.MpOrderPayer(
                "Valeria",
                normalizedLastName,
                "valeria@example.com",
                null
        );

        MpOrderRequest request = new MpOrderRequest(
                "online", "manual", "automatic", new BigDecimal("45.00"), "ORDER-1",
                List.of(new MpOrderRequest.MpOrderItem("Torta", null, 1, new BigDecimal("45.00"))),
                payer, null
        );

        String json = objectMapper.writeValueAsString(request);
        JsonNode root = objectMapper.readTree(json);
        JsonNode payerNode = root.get("payer");

        assertThat(payerNode).isNotNull();
        assertThat(payerNode.has("last_name")).as("last_name con solo espacios debe ser omitido").isFalse();
    }

    @Test
    @DisplayName("Payer con last_name válido con longitud >= 1 -> se conserva en el JSON")
    void shouldPreserveValidLastName() throws Exception {
        String rawLastName = "  Ramos  ";
        String normalizedLastName = StringUtils.hasText(rawLastName) ? rawLastName.trim() : null;

        MpOrderRequest.MpOrderPayer payer = new MpOrderRequest.MpOrderPayer(
                "Valeria",
                normalizedLastName,
                "valeria@example.com",
                null
        );

        MpOrderRequest request = new MpOrderRequest(
                "online", "manual", "automatic", new BigDecimal("45.00"), "ORDER-1",
                List.of(new MpOrderRequest.MpOrderItem("Torta", null, 1, new BigDecimal("45.00"))),
                payer, null
        );

        String json = objectMapper.writeValueAsString(request);
        JsonNode root = objectMapper.readTree(json);
        JsonNode payerNode = root.get("payer");

        assertThat(payerNode).isNotNull();
        assertThat(payerNode.has("last_name")).isTrue();
        assertThat(payerNode.get("last_name").asText()).isEqualTo("Ramos");
    }

    @Test
    @DisplayName("Payer phone con areaCode null -> se omite area_code y conserva number")
    void shouldOmitAreaCodeWhenNullInPhone() throws Exception {
        MpOrderRequest.MpOrderPhone phone = new MpOrderRequest.MpOrderPhone(null, "+51987654321");
        MpOrderRequest.MpOrderPayer payer = new MpOrderRequest.MpOrderPayer(
                "Valeria",
                "Ramos",
                "valeria@example.com",
                phone
        );

        MpOrderRequest request = new MpOrderRequest(
                "online", "manual", "automatic", new BigDecimal("45.00"), "ORDER-1",
                List.of(new MpOrderRequest.MpOrderItem("Torta", null, 1, new BigDecimal("45.00"))),
                payer, null
        );

        String json = objectMapper.writeValueAsString(request);
        JsonNode root = objectMapper.readTree(json);
        JsonNode phoneNode = root.get("payer").get("phone");

        assertThat(phoneNode).isNotNull();
        assertThat(phoneNode.has("area_code")).as("area_code null debe ser omitido").isFalse();
        assertThat(phoneNode.get("number").asText()).isEqualTo("+51987654321");
    }

    @Test
    @DisplayName("Tres URLs null -> campo config completamente ausente del JSON")
    void shouldOmitConfigWhenAllUrlsAreNull() throws Exception {
        MpOrderRequest request = createBaseRequest(buildConfig(null, null, null));
        String json = objectMapper.writeValueAsString(request);
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.has("config")).as("El campo config debe omitirse cuando las URLs son null").isFalse();
    }

    @Test
    @DisplayName("Tres URLs vacías (cadenas vacías) -> campo config completamente ausente del JSON")
    void shouldOmitConfigWhenAllUrlsAreEmptyStrings() throws Exception {
        MpOrderRequest request = createBaseRequest(buildConfig("", "", ""));
        String json = objectMapper.writeValueAsString(request);
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.has("config")).as("El campo config debe omitirse cuando las URLs son vacías").isFalse();
    }

    @Test
    @DisplayName("Tres URLs compuestas solo por espacios en blanco -> campo config completamente ausente del JSON")
    void shouldOmitConfigWhenAllUrlsAreWhitespace() throws Exception {
        MpOrderRequest request = createBaseRequest(buildConfig("   ", "\t", "\n  "));
        String json = objectMapper.writeValueAsString(request);
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.has("config")).as("El campo config debe omitirse cuando las URLs son espacios en blanco").isFalse();
    }

    @Test
    @DisplayName("URLs válidas configuradas -> campo config presente con todas las URLs y auto_return")
    void shouldIncludeConfigWhenUrlsAreValid() throws Exception {
        MpOrderRequest request = createBaseRequest(buildConfig(
                "https://example.com/success",
                "https://example.com/failure",
                "https://example.com/pending"
        ));
        String json = objectMapper.writeValueAsString(request);
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.has("config")).isTrue();
        JsonNode onlineNode = root.get("config").get("online");
        assertThat(onlineNode).isNotNull();
        assertThat(onlineNode.get("success_url").asText()).isEqualTo("https://example.com/success");
        assertThat(onlineNode.get("failure_url").asText()).isEqualTo("https://example.com/failure");
        assertThat(onlineNode.get("pending_url").asText()).isEqualTo("https://example.com/pending");
        assertThat(onlineNode.get("auto_return").asText()).isEqualTo("approved");
    }

    @Test
    @DisplayName("Combinación parcial: solo success_url presente -> omite failure_url y pending_url del JSON")
    void shouldIncludeOnlyConfiguredUrlsInPartialCombination() throws Exception {
        MpOrderRequest request = createBaseRequest(buildConfig(
                "https://example.com/success",
                "  ",
                null
        ));
        String json = objectMapper.writeValueAsString(request);
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.has("config")).isTrue();
        JsonNode onlineNode = root.get("config").get("online");
        assertThat(onlineNode).isNotNull();
        assertThat(onlineNode.get("success_url").asText()).isEqualTo("https://example.com/success");
        assertThat(onlineNode.has("failure_url")).as("failure_url debe omitirse").isFalse();
        assertThat(onlineNode.has("pending_url")).as("pending_url debe omitirse").isFalse();
        assertThat(onlineNode.get("auto_return").asText()).isEqualTo("approved");
    }

    @Test
    @DisplayName("MpPreferenceRequest (flujo legacy Preferences) no es alterado y conserva id, currency_id y unit_price numérico")
    void shouldNotAlterLegacyPreferencesRequest() throws Exception {
        BigDecimal unitPrice = new BigDecimal("25.00");
        MpPreferenceRequest.MpItem item = new MpPreferenceRequest.MpItem(
                "pref-1", "Almuerzo", "Menu", 1, unitPrice, "PEN"
        );

        MpPreferenceRequest preferenceRequest = new MpPreferenceRequest(
                List.of(item), null, null, null, null, "REF-PREF", "JaldiShop"
        );

        String json = objectMapper.writeValueAsString(preferenceRequest);
        JsonNode root = objectMapper.readTree(json);

        JsonNode prefItemNode = root.get("items").get(0);
        assertThat(prefItemNode.get("id").asText()).isEqualTo("pref-1");
        assertThat(prefItemNode.get("currency_id").asText()).isEqualTo("PEN");
        JsonNode prefUnitPriceNode = prefItemNode.get("unit_price");
        assertThat(prefUnitPriceNode).isNotNull();
        assertThat(prefUnitPriceNode.isNumber()).as("Preferences unit_price debe conservar serialización numérica").isTrue();
        assertThat(prefUnitPriceNode.asDouble()).isEqualTo(25.0);
    }

    @Test
    @DisplayName("MpOrderRequest serializa expiration_time como string cuando está presente")
    void shouldSerializeExpirationTimeWhenProvided() throws Exception {
        MpOrderRequest.MpOrderItem item = new MpOrderRequest.MpOrderItem(
                "Café Espresso", "Taza 250ml", 1, new BigDecimal("1.00")
        );
        MpOrderRequest request = new MpOrderRequest(
                "online", "manual", "automatic", new BigDecimal("1.00"), "REF-EXP-01",
                List.of(item), null, null, "PT9M30S"
        );

        String json = objectMapper.writeValueAsString(request);
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.has("expiration_time")).isTrue();
        assertThat(root.get("expiration_time").asText()).isEqualTo("PT9M30S");
    }

    @Test
    @DisplayName("MpOrderRequest omite expiration_time cuando es nulo")
    void shouldOmitExpirationTimeWhenNull() throws Exception {
        MpOrderRequest.MpOrderItem item = new MpOrderRequest.MpOrderItem(
                "Café Espresso", "Taza 250ml", 1, new BigDecimal("1.00")
        );
        MpOrderRequest request = new MpOrderRequest(
                "online", "manual", "automatic", new BigDecimal("1.00"), "REF-EXP-02",
                List.of(item), null, null, null
        );

        String json = objectMapper.writeValueAsString(request);
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.has("expiration_time")).isFalse();
    }
}
