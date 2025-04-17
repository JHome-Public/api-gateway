package com.jhome.api.filter;

import com.jhome.api.exception.CustomException;
import com.jhome.api.jwt.JwtProperty;
import com.jhome.api.service.RedisTokenService;
import com.jhome.api.response.ApiResponseCode;
import com.jhome.api.jwt.JwtUtil;
import com.jhome.api.response.ResponseUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class GlobalAuthFilter extends AbstractGatewayFilterFactory<GlobalAuthFilter.Config> {

    private final RedisTokenService tokenService;
    private final ResponseUtil responseUtil;
    private final JwtUtil jwtUtil;
    private final JwtProperty jwtProperty;

    public GlobalAuthFilter(RedisTokenService redisTokenService, ResponseUtil responseUtil, JwtUtil jwtUtil, JwtProperty jwtProperty) {
        super(Config.class);
        this.tokenService = redisTokenService;
        this.responseUtil = responseUtil;
        this.jwtUtil = jwtUtil;
        this.jwtProperty = jwtProperty;
    }

    @Getter
    @Setter
    public final static class Config {
        final private List<String> excludeUris = List.of(
                "/api/users/register"
        );
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            log.info("[GLOBAL_AUTH_FILTER] Start Filter");

            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            if (isExcludePath(config, request)){
                log.info("[GLOBAL_AUTH_FILTER] Excluded Filter");
                return chain.filter(exchange);
            }

            String accessToken = jwtUtil.getAccessToken(request);
            if (isAccessTokenInvalid(accessToken)) {
                log.info("[GLOBAL_AUTH_FILTER] Invalid Access Token");

                String refreshToken = jwtUtil.getRefreshToken(request);
                String storedToken = tokenService.getRefreshByToken(refreshToken);
                if(isRefreshTokenInvalid(refreshToken, storedToken)){
                    log.info("[GLOBAL_AUTH_FILTER] Invalid Refresh Token");
                    renewTokens(refreshToken, response);
                    log.info("[GLOBAL_AUTH_FILTER] Token Renewed");
                }
            } else if(isAccessTokenUnknown(accessToken)) {
                log.info("[GLOBAL_AUTH_FILTER] Username Not Found");
                throw new CustomException(ApiResponseCode.TOKEN_NOT_FOUND);
            }

            log.info("[GLOBAL_AUTH_FILTER] End Filter");
            return chain.filter(exchange);
        };
    }

    private boolean isExcludePath(Config config, ServerHttpRequest request) {
        String path = request.getURI().getPath();
        String method = request.getMethod().toString();
        log.info("[GLOBAL_AUTH_FILTER] Path = {}, Method = {}", path, method);

        return config.getExcludeUris().stream().anyMatch(path::startsWith);
    }

    private boolean isAccessTokenInvalid(String accessToken) {
        jwtUtil.validateAccessToken(accessToken);
        return jwtUtil.isExpired(accessToken);
    }

    private boolean isAccessTokenUnknown(String accessToken) {
        return tokenService.getRefreshByToken(accessToken) == null;
    }

    private boolean isRefreshTokenInvalid(String refreshToken, String storedToken) {
        jwtUtil.validateRefreshToken(refreshToken);
        jwtUtil.validateRefreshTokenFromRedis(storedToken, refreshToken);
        return jwtUtil.isExpired(refreshToken);
    }

    private void renewTokens(String refreshToken, ServerHttpResponse response) {
        String newAccess = jwtUtil.renewAccessToken(refreshToken);
        responseUtil.addHeader(response, jwtProperty.getAccessKey(), jwtProperty.getPrefix() + newAccess);
        log.info("[GLOBAL_AUTH_FILTER] Added new Access Token to Response Header");

        String newRefresh = jwtUtil.renewRefreshToken(refreshToken);
        responseUtil.addCookie(response, jwtProperty.getRefreshKey(), newRefresh, jwtProperty.getRefreshAgeMS());
        tokenService.saveRefresh(newRefresh);
        log.info("[GLOBAL_AUTH_FILTER] Added new Refresh Token to Response Cookie");
    }

}