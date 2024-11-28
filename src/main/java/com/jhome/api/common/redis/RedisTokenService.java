package com.jhome.api.common.redis;

import com.jhome.api.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisTokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtUtil jwtUtil;

    public boolean validateToken(String token) {
        // 유효한 토큰인지 검증
        jwtUtil.isExpired(token);
        
        // 토큰에서 username 추출
        String username = jwtUtil.getUsername(token);

        // Redis에서 username을 키로 저장된 token 가져오기
        String key = "username:" + username;
        String storedToken = (String) redisTemplate.opsForHash().get(key, "token");

        // 저장된 토큰이 없거나 일치하지 않으면 false
        return storedToken != null && storedToken.equals(token);
    }

    public void extendTokenTTL(String token) {
        String username = jwtUtil.getUsername(token);
        redisTemplate.expire(username, 3600, TimeUnit.SECONDS);
    }
}
