package com.jhome.api.service;

import com.jhome.api.jwt.JwtProperty;
import com.jhome.api.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisTokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtUtil jwtUtil;
    private final JwtProperty jwtProperty;

    public void saveRefresh(String token) {
        String username = jwtUtil.getUsername(token);
        String key = "username:" + username;
        redisTemplate.opsForHash().put(key, jwtProperty.getRefreshKey(), token);
        redisTemplate.expire(key, Duration.ofMillis(jwtProperty.getRefreshAgeMS()));
    }

    public String getRefreshByToken(String token) {
        String username = jwtUtil.getUsername(token);
        String key = "username:" + username;
        return (String) redisTemplate.opsForHash().get(key, jwtProperty.getRefreshKey());
    }

};
