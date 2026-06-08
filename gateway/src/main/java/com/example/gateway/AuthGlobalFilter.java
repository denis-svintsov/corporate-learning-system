package com.example.gateway;

import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {
    private final AuthValidationClient authValidationClient;

    public AuthGlobalFilter(AuthValidationClient authValidationClient) {
        this.authValidationClient = authValidationClient;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }
        if (path.startsWith("/auth/") || path.startsWith("/actuator/")) {
            return chain.filter(exchange);
        }
        if (exchange.getRequest().getMethod() == HttpMethod.GET && path.startsWith("/courses/covers/")) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        Mono<ServerHttpRequest> authenticatedRequest = authValidationClient.validate(authHeader)
                .onErrorResume(ex -> Mono.empty())
                .map(result -> exchange.getRequest().mutate()
                            .headers(headers -> {
                                headers.remove("X-User-Id");
                                headers.remove("X-User-Name");
                                headers.remove("X-User-Roles");
                                headers.set("X-User-Id", result.userId() == null ? "" : result.userId());
                                headers.set("X-User-Name", result.username() == null ? "" : result.username());
                                headers.set("X-User-Roles", result.roles() == null ? "" : String.join(",", result.roles()));
                            })
                            .build());

        return authenticatedRequest
                .switchIfEmpty(Mono.defer(() -> unauthorized(exchange)))
                .flatMap(mutatedRequest -> chain.filter(exchange.mutate().request(mutatedRequest).build()));
    }

    @Override
    public int getOrder() {
        return -50;
    }

    private Mono<ServerHttpRequest> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete().then(Mono.empty());
    }
}
