package com.rlnkoo.gatewayservice.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@SpringBootTest(
        classes = SecurityConfigTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
class SecurityConfigTest {

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        this.webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void shouldPermitAccessToAuthLoginWithoutAuthentication() {
        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "email": "john@example.com",
                          "password": "secret"
                        }
                        """)
                // when
                .exchange()
                // then
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("ok");
    }

    @Test
    void shouldPermitAccessToAuthRegisterWithoutAuthentication() {
        webTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "email": "john@example.com",
                          "password": "secret"
                        }
                        """)
                // when
                .exchange()
                // then
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("ok");
    }

    @Test
    void shouldPermitGetAccessToListingsWithoutAuthentication() {
        webTestClient.get()
                .uri("/listings/123")
                // when
                .exchange()
                // then
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("ok");
    }

    @Test
    void shouldPermitGetAccessToMediaWithoutAuthentication() {
        webTestClient.get()
                .uri("/media/123")
                // when
                .exchange()
                // then
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("ok");
    }

    @Test
    void shouldPermitGetAccessToSearchWithoutAuthentication() {
        webTestClient.get()
                .uri("/search/query?q=flat")
                // when
                .exchange()
                // then
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("ok");
    }

    @Test
    void shouldRequireAuthenticationForSearchAdminEndpoint() {
        webTestClient.get()
                .uri("/search/admin/reindex")
                // when
                .exchange()
                // then
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldRequireAuthenticationForMeEndpoint() {
        webTestClient.get()
                .uri("/me/profile")
                // when
                .exchange()
                // then
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldRequireAuthenticationForAdminEndpoint() {
        webTestClient.get()
                .uri("/admin/users")
                // when
                .exchange()
                // then
                .expectStatus().isUnauthorized();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({SecurityConfig.class, TestBeansConfig.class, TestController.class})
    static class TestApplication {
    }

    @TestConfiguration
    static class TestBeansConfig {

        @Bean
        Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
            return JwtAuthenticationToken::new;
        }

        @Bean
        ReactiveJwtDecoder reactiveJwtDecoder() {
            return token -> Mono.just(
                    Jwt.withTokenValue(token)
                            .header("alg", "HS256")
                            .claim("sub", "test-user")
                            .build()
            );
        }
    }

    @RestController
    static class TestController {

        @PostMapping("/auth/login")
        String authLogin() {
            return "ok";
        }

        @PostMapping("/auth/register")
        String authRegister() {
            return "ok";
        }

        @GetMapping("/listings/{id}")
        String listing(@PathVariable("id") String id) {
            return "ok";
        }

        @GetMapping("/media/{id}")
        String media(@PathVariable("id") String id) {
            return "ok";
        }

        @GetMapping("/search/query")
        String search() {
            return "ok";
        }

        @GetMapping("/search/admin/reindex")
        String searchAdmin() {
            return "ok";
        }

        @GetMapping("/me/profile")
        String me() {
            return "ok";
        }

        @GetMapping("/admin/users")
        String admin() {
            return "ok";
        }
    }
}