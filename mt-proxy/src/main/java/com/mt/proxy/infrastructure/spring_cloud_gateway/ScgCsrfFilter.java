package com.mt.proxy.infrastructure.spring_cloud_gateway;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * todo reserve for future usage:
 * instead of use spring security implementation,
 * write our own implementation with https.
 */
@Slf4j
@Component
public class ScgCsrfFilter implements GlobalFilter, Ordered {
    @Value("${manytree.domain-name}")
    String domain;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.trace("inside ScgCsrfFilter - order "+getOrder());
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            log.trace("inside [then]");
            if (exchange.getRequest().getCookies().get("XSRF-TOKEN") == null
                &&
                exchange.getRequest().getHeaders().get("x-xsrf-token") == null) {
                String var0 = UUID.randomUUID().toString();
                exchange.getResponse().getHeaders().add(HttpHeaders.SET_COOKIE,
                    "XSRF-TOKEN=" + var0 + "; SameSite=None; Path=/; Secure; Domain=" + domain);
            }
        }));
    }

    @Override
    public int getOrder() {
        return 4;
    }
}
