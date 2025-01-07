package com.jhome.api;

import com.jhome.api.common.exception.CustomException;
import com.jhome.api.common.property.JwtProperty;
import com.jhome.api.common.redis.RedisTokenService;
import com.jhome.api.common.response.ApiResponseCode;
import com.jhome.api.common.property.JwtUtil;
import com.jhome.api.common.response.ResponseUtil;
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
    public static class Config {
        private List<String> excludeUris = List.of(
                "/api/users/register"
        );
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();
            String path = request.getURI().getPath();
            String method = request.getMethod().toString();
            log.info("[GLOBAL_AUTH_FILTER] Start Filter, Path = {}, Method = {}", path, method);

            // 토큰 검증 예외 URI 확인
            if (config.getExcludeUris().stream().anyMatch(path::startsWith)) {
                log.info("[GLOBAL_AUTH_FILTER] Excluded Filter");
                return chain.filter(exchange);
            }

            // 액세스 토큰 검증
            String accessToken = getAccessToken(request);
            validateAccessToken(accessToken);

            // 액세스 토큰 만료여부 확인
            if(jwtUtil.isExpired(accessToken)) {
                log.info("[GLOBAL_AUTH_FILTER] Expired Access Token");

                // 리프레시 토큰 검증
                String refreshToken = getRefreshToken(request);
                validateRefreshToken(refreshToken);

                // 액세스 토큰 갱신
                String newAccess = jwtUtil.renewAccessToken(refreshToken);
                responseUtil.addHeader(response, jwtProperty.getAccessKey(), jwtProperty.getPrefix() + newAccess);
                log.info("[GLOBAL_AUTH_FILTER] Added new Access Token to Response Header");

                // 리프레시 토큰 갱신
                String newRefresh = jwtUtil.renewRefreshToken(refreshToken);
                tokenService.saveRefresh(newRefresh);
                responseUtil.addCookie(response, jwtProperty.getRefreshKey(), newRefresh, jwtProperty.getRefreshAgeMS());
                log.info("[GLOBAL_AUTH_FILTER] Added new Refresh Token to Response Cookie");

            } else if(tokenService.getRefreshByToken(accessToken) == null) {
                // 액세스 토큰에서 username 추출하여 Redis 조회 했을 때 데이터가 없다면 에러
                log.info("[GLOBAL_AUTH_FILTER] Username Not Found");
                throw new CustomException(ApiResponseCode.TOKEN_NOT_FOUND);
            }

            log.info("[GLOBAL_AUTH_FILTER] End Filter");
            return chain.filter(exchange);
        };
    }

    private String getAccessToken(ServerHttpRequest request) {
        String accessToken = request.getHeaders().getFirst(jwtProperty.getAccessKey());
        if (accessToken == null || !accessToken.startsWith(jwtProperty.getPrefix())) {
            log.info("[GLOBAL_AUTH_FILTER] Missing Access Token");
            throw new CustomException(ApiResponseCode.INVALID_TOKEN);
        }
        return accessToken.replace(jwtProperty.getPrefix(), "");
    }

    private String getRefreshToken(ServerHttpRequest request) {
        if (request.getCookies().containsKey(jwtProperty.getRefreshKey())) {
            return request.getCookies().getFirst(jwtProperty.getRefreshKey()).getValue();
        } else {
            log.info("[GLOBAL_AUTH_FILTER] Missing Refresh Token");
            throw new CustomException(ApiResponseCode.INVALID_TOKEN);
        }
    }

    private void validateAccessToken(String token) {
        if (token == null || token.isEmpty()) {
            log.info("[GLOBAL_AUTH_FILTER] Invalid Access Token");
            throw new CustomException(ApiResponseCode.INVALID_TOKEN);
        }
    }

    private void validateRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            log.info("[GLOBAL_AUTH_FILTER] Invalid Refresh Token");
            throw new CustomException(ApiResponseCode.INVALID_TOKEN);
        }

        if(jwtUtil.isExpired(refreshToken)) {
            log.info("[GLOBAL_AUTH_FILTER] Expired Refresh Token");
            throw new CustomException(ApiResponseCode.INVALID_TOKEN);
        }

        String storedToken = tokenService.getRefreshByToken(refreshToken);
        if(storedToken == null){
            log.info("[GLOBAL_AUTH_FILTER] Missing Stored Token");
            throw new CustomException(ApiResponseCode.INVALID_TOKEN);
        }

        if(!storedToken.equals(refreshToken)){
            log.info("[GLOBAL_AUTH_FILTER] Invalid Stored Token");
            throw new CustomException(ApiResponseCode.INVALID_TOKEN);
        }
    }

}