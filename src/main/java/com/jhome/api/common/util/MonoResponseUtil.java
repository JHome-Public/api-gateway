package com.jhome.api.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jhome.api.common.response.ApiResponse;
import com.jhome.api.common.response.ApiResponseCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.IOException;

public class MonoResponseUtil {

    public Mono<Void> sendResponse(ServerWebExchange exchange, ApiResponseCode responseCode) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String responseBody = objectMapper.writeValueAsString(ApiResponse.fail(responseCode));

        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                .bufferFactory().wrap(responseBody.getBytes())));
    }

}
