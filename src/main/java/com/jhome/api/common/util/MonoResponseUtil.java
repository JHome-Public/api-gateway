package com.jhome.api.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jhome.api.common.exception.CustomException;
import com.jhome.api.common.response.ApiResponse;
import com.jhome.api.common.response.ApiResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class MonoResponseUtil {

    private final ObjectMapper objectMapper;

    public Mono<Void> sendResponse(ServerWebExchange exchange, ApiResponseCode responseCode) {
        try {
            String responseBody = objectMapper.writeValueAsString(ApiResponse.fail(responseCode));

            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                    .bufferFactory().wrap(responseBody.getBytes())));
        } catch (Exception e) {
            throw new CustomException(ApiResponseCode.FAIL);
        }
    }

}
