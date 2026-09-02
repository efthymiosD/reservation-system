package com.decoupledx.reservation.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

import com.decoupledx.reservation.testinfra.PostgresIntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.decoupledx.reservation.identity.domain.model.CustomerId;
import com.decoupledx.reservation.identity.domain.service.CustomerAccountService;

/**
 * Realm contract: any access token issued by Keycloak must carry a {@code sub}
 * claim, and the application must resolve that {@code sub} to a stable internal
 * {@link CustomerId}. Guards against silently dropping the subject mapper (or
 * client scope changes) that would break identity resolution.
 *
 * <p>Opt-in: requires a running Keycloak (docker-compose) on port 8081 and is
 * excluded from the default test suite. Run with:
 * {@code ./mvnw test -Dgroups=keycloak}
 */
@Tag("keycloak")
class KeycloakTokenContractIntegrationTest extends PostgresIntegrationTest {

    private static final String REALM = "http://localhost:8081/realms/reservation";
    private static final String CLIENT_ID = "reservation-api";
    private static final String USERNAME = "alice";
    private static final String PASSWORD = "alice";

    @Autowired
    CustomerAccountService customerAccounts;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void issuedAccessTokenContainsSubAndResolvesToStableCustomerId() throws Exception {
        String accessToken = obtainAccessToken();

        JsonNode claims = decode(accessToken);
        JsonNode subNode = claims.get("sub");
        assertThat(subNode)
                .as("access token must carry a 'sub' claim (Keycloak subject mapper)")
                .isNotNull();
        String sub = subNode.asText();
        assertThat(sub).isNotBlank();

        CustomerId first = customerAccounts.resolveOrProvision(sub);
        CustomerId second = customerAccounts.resolveOrProvision(sub);
        assertThat(second).isEqualTo(first);
    }

    private String obtainAccessToken() throws Exception {
        String body = "client_id=" + CLIENT_ID
                + "&grant_type=password"
                + "&username=" + USERNAME
                + "&password=" + PASSWORD;
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(REALM + "/protocol/openid-connect/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as("token endpoint status").isEqualTo(200);
        JsonNode json = objectMapper.readTree(response.body());
        return json.get("access_token").asText();
    }

    private JsonNode decode(String token) {
        String payload = token.split("\\.")[1];
        return objectMapper.readTree(base64UrlDecode(payload));
    }

    private String base64UrlDecode(String value) {
        String padded = value.replace('-', '+').replace('_', '/');
        padded = padded + "=".repeat((4 - padded.length() % 4) % 4);
        return new String(Base64.getUrlDecoder().decode(padded));
    }
}
