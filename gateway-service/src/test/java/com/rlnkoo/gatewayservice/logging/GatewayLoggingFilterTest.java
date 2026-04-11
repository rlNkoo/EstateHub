package com.rlnkoo.gatewayservice.logging;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;

class GatewayLoggingFilterTest {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final GatewayLoggingFilter filter = new GatewayLoggingFilter();

    @Test
    void shouldAddRequestIdHeaderWhenMissing() {
        // given
        MockServerHttpRequest request = MockServerHttpRequest.get("/listings/123").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        CapturingGatewayFilterChain chain = new CapturingGatewayFilterChain();

        // when
        filter.filter(exchange, chain).block();

        // then
        assertNotNull(chain.capturedExchange);

        ServerHttpRequest forwardedRequest = chain.capturedExchange.getRequest();
        String requestId = forwardedRequest.getHeaders().getFirst(REQUEST_ID_HEADER);

        assertNotNull(requestId);
        assertFalse(requestId.isBlank());
    }

    @Test
    void shouldKeepExistingRequestIdHeader() {
        // given
        String existingRequestId = "existing-request-id-123";

        MockServerHttpRequest request = MockServerHttpRequest.get("/media/10")
                .header(REQUEST_ID_HEADER, existingRequestId)
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        CapturingGatewayFilterChain chain = new CapturingGatewayFilterChain();

        // when
        filter.filter(exchange, chain).block();

        // then
        assertNotNull(chain.capturedExchange);

        String requestId = chain.capturedExchange.getRequest()
                .getHeaders()
                .getFirst(REQUEST_ID_HEADER);

        assertEquals(existingRequestId, requestId);
    }

    private static class CapturingGatewayFilterChain implements GatewayFilterChain {

        private ServerWebExchange capturedExchange;

        @Override
        public Mono<Void> filter(ServerWebExchange exchange) {
            this.capturedExchange = exchange;
            return Mono.empty();
        }
    }
}