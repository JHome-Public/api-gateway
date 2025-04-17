package com.jhome.api.response;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResponseUtil {

    public void addHeader(ServerHttpResponse response, String key, String value) {
        response.getHeaders().add(key, value);
    }

    public void addCookie(ServerHttpResponse response, String key, String value, Long age) {
        ResponseCookie cookie = ResponseCookie.from(key, value) // 쿠키 이름과 값 설정
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(age)
                .build();

        response.addCookie(cookie);
    }

}
