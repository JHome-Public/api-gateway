package com.jhome.api;

import com.jhome.api.common.redis.RedisTokenService;
import com.jhome.api.common.response.ApiResponseCode;
import com.jhome.api.common.util.MonoResponseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;

@Slf4j
@Component
public class GlobalAuthFilter extends AbstractGatewayFilterFactory<GlobalAuthFilter.Config> {

    private final RedisTokenService tokenService;
    private final MonoResponseUtil monoResponseUtil;

    public GlobalAuthFilter(RedisTokenService redisTokenService, MonoResponseUtil monoResponseUtil) {
        super(Config.class);
        this.tokenService = redisTokenService;
        this.monoResponseUtil = monoResponseUtil;
    }

    public static class Config {
        // 필터의 설정값을 정의할 수 있습니다
    }

    private final List<String> excludeUris = List.of("/api/login", "/api/users/register");

    @Value("${spring.jwt.prefix}")
    private String prefix;

    @Override
    public GatewayFilter apply(Config config) {
        return (ServerWebExchange exchange, GatewayFilterChain chain) -> {
            log.info("[GLOBAL_AUTH_FILTER] Start");

            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            log.info("[GLOBAL_AUTH_FILTER] Path = {}", path);
            for (String excludeUri : excludeUris) {
                if (excludeUri.equals(path)) {
                    log.info("[GLOBAL_AUTH_FILTER] Excluded Filter");
                    return chain.filter(exchange);
                }
            }

            String token = exchange.getRequest().getHeaders().getFirst("Authorization");
            log.info("[GLOBAL_AUTH_FILTER] Token = {}", token);

            if (token == null || !token.startsWith(prefix)) {
                log.info("[GLOBAL_AUTH_FILTER] Invalid Token");
                return monoResponseUtil.sendResponse(exchange, ApiResponseCode.INVALID_TOKEN);
            }

            token = token.replace(prefix, "");
            log.info("[GLOBAL_AUTH_FILTER] Token = {}", token);

            boolean isValid = tokenService.validateToken(token);
            log.info("[GLOBAL_AUTH_FILTER] Token is Valid = {}", isValid);
            if (!isValid) {
                return monoResponseUtil.sendResponse(exchange, ApiResponseCode.INVALID_TOKEN);
            }

            tokenService.extendTokenTTL(token);
            log.info("[GLOBAL_AUTH_FILTER] Extended Token TTL");

            log.info("[GLOBAL_AUTH_FILTER] End");
            return chain.filter(exchange);
        };
    }

}