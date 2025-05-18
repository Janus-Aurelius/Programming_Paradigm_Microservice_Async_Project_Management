package com.pm.userservice.config;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.Optional;
import java.util.UUID;

@Component
public class MdcLoggingFilter implements WebFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_CONTEXT_KEY = "correlationId"; // Key for Reactor Context & MDC

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String correlationId = Optional.ofNullable(exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER))
                .orElse("N/A-" + UUID.randomUUID().toString().substring(0, 8));

        // Chain the filter, adding the correlationId to the Reactor Context
        return chain.filter(exchange)
                .contextWrite(Context.of(CORRELATION_ID_CONTEXT_KEY, correlationId))
                // Set MDC for synchronous logging within reactive operators
                .doOnEach(signal -> {
                    if (signal.getContextView().hasKey(CORRELATION_ID_CONTEXT_KEY)) {
                        MDC.put(CORRELATION_ID_CONTEXT_KEY, signal.getContextView().get(CORRELATION_ID_CONTEXT_KEY));
                    }
                })
                .doFinally(signalType -> MDC.remove(CORRELATION_ID_CONTEXT_KEY)); // Cleanup MDC
    }
}