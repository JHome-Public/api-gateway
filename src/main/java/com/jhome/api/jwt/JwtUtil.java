package com.jhome.api.jwt;

import com.jhome.api.exception.CustomException;
import com.jhome.api.response.ApiResponseCode;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Objects;

@Slf4j
@Component
public class JwtUtil {

    private final JwtProperty jwtProperty;
    private final SecretKey secretKey;

    public JwtUtil(JwtProperty jwtProperty) {
        this.jwtProperty = jwtProperty;
        this.secretKey = new SecretKeySpec(
                jwtProperty.getSecret().getBytes(StandardCharsets.UTF_8),
                Jwts.SIG.HS256.key().build().getAlgorithm());
    }

    public String getUsername(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("username", String.class);
    }

    public String getRole(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("role", String.class);
    }

    public String getCategory(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("category", String.class);
    }

    public Boolean isExpired(String token) {
        try {
            return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public String createJwt(String category, String username, String role, Long age) {
        return Jwts.builder()
                .claim("category", category)
                .claim("username", username)
                .claim("role", role)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + age))
                .signWith(secretKey)
                .compact();
    }

    public String renewAccessToken(String token) {
        String username = getUsername(token);
        String role = getRole(token);
        return createJwt(jwtProperty.getAccessKey(), username, role, jwtProperty.getRefreshAgeMS());
    }

    public String renewRefreshToken(String token) {
        String username = getUsername(token);
        String role = getRole(token);
            return createJwt(jwtProperty.getRefreshKey(), username, role, jwtProperty.getRefreshAgeMS());
    }

    public String getAccessToken(ServerHttpRequest request) {
        String accessToken = request.getHeaders().getFirst(jwtProperty.getAccessKey());
        if (accessToken == null || !accessToken.startsWith(jwtProperty.getPrefix())) {
            log.info("[GLOBAL_AUTH_FILTER] Missing Access Token");
            throw new CustomException(ApiResponseCode.INVALID_TOKEN);
        }
        return accessToken.replace(jwtProperty.getPrefix(), "");
    }

    public String getRefreshToken(ServerHttpRequest request) {
        if (request.getCookies().containsKey(jwtProperty.getRefreshKey())) {
            return Objects.requireNonNull(request.getCookies().getFirst(jwtProperty.getRefreshKey())).getValue();
        } else {
            log.info("[GLOBAL_AUTH_FILTER] Missing Refresh Token");
            throw new CustomException(ApiResponseCode.INVALID_TOKEN);
        }
    }

    public void validateRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            log.info("[GLOBAL_AUTH_FILTER] Invalid Refresh Token");
            throw new CustomException(ApiResponseCode.INVALID_TOKEN);
        }

        if (isExpired(refreshToken)) {
            log.info("[GLOBAL_AUTH_FILTER] Expired Refresh Token");
            throw new CustomException(ApiResponseCode.INVALID_TOKEN);
        }
    }

    public void validateRefreshTokenFromRedis(String storedToken, String refreshToken){
        if (storedToken == null){
            log.info("[GLOBAL_AUTH_FILTER] Missing Stored Token");
            throw new CustomException(ApiResponseCode.INVALID_TOKEN);
        }

        if (!storedToken.equals(refreshToken)){
            log.info("[GLOBAL_AUTH_FILTER] Invalid Stored Token");
            throw new CustomException(ApiResponseCode.INVALID_TOKEN);
        }
    }

    public void validateAccessToken(String token) {
        if (token == null || token.isEmpty()) {
            log.info("[GLOBAL_AUTH_FILTER] Invalid Access Token");
            throw new CustomException(ApiResponseCode.INVALID_TOKEN);
        }
    }
}