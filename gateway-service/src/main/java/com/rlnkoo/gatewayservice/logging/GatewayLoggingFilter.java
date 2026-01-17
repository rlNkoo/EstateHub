package com.rlnkoo.gatewayservice.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class GatewayLoggingFilter implements GlobalFilter, Ordered {

    private static final String HEADER_REQUEST_ID = "X-Request-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest req = exchange.getRequest();

        String method = req.getMethod() != null ? req.getMethod().name() : "UNKNOWN";
        String path = req.getURI().getRawPath();

        String requestId = req.getHeaders().getFirst(HEADER_REQUEST_ID);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
            req = req.mutate().header(HEADER_REQUEST_ID, requestId).build();
            exchange = exchange.mutate().request(req).build();
        }

        final ServerWebExchange finalExchange = exchange;
        final String finalMethod = method;
        final String finalPath = path;
        final String finalRequestId = requestId;

        long start = System.currentTimeMillis();

        log.info("Incoming request method=[{}] path=[{}] requestId=[{}]", finalMethod, finalPath, finalRequestId);

        return chain.filter(finalExchange)
                .doOnSuccess(v -> {
                    int status = finalExchange.getResponse().getStatusCode() != null
                            ? finalExchange.getResponse().getStatusCode().value()
                            : 0;

                    long durationMs = System.currentTimeMillis() - start;

                    String routeId = Optional.ofNullable(finalExchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR))
                            .map(attr -> ((Route) attr).getId())
                            .orElse("unknown");

                    log.info("Response sent status=[{}] method=[{}] path=[{}] durationMs=[{}] routeId=[{}] requestId=[{}]",
                            status, finalMethod, finalPath, durationMs, routeId, finalRequestId);
                })
                .doOnError(ex -> {
                    long durationMs = System.currentTimeMillis() - start;

                    String routeId = Optional.ofNullable(finalExchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR))
                            .map(attr -> ((Route) attr).getId())
                            .orElse("unknown");

                    log.warn("Request failed method=[{}] path=[{}] durationMs=[{}] routeId=[{}] requestId=[{}] message=[{}]",
                            finalMethod, finalPath, durationMs, routeId, finalRequestId, ex.getMessage());
                });
    }

    @Override
    public int getOrder() {
        return -1;
    }
}