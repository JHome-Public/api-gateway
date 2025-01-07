package com.jhome.api.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jhome.api.common.response.ApiResponse;
import com.jhome.api.common.response.ApiResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if(response.isCommitted()) {
            return Mono.error(ex);
        }

        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ApiResponse<?> apiResponse;
        if (ex instanceof CustomException) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            apiResponse = ApiResponse.fail(ApiResponseCode.INVALID_TOKEN);
            log.error("[GlobalExceptionHandler] Invalid Token, {}", ex.getMessage());
        } else if(ex instanceof ConnectException){
            response.setStatusCode(HttpStatus.BAD_GATEWAY);
            apiResponse = ApiResponse.fail(ApiResponseCode.CONNECTION_REFUSED);
            log.error("[GlobalExceptionHandler] Connection Error, {}", ex.getMessage());
        } else {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            apiResponse = ApiResponse.fail(ApiResponseCode.FAIL);
            log.error("[GlobalExceptionHandler] Unhandled Exception, {}", ex.getMessage());
            ex.printStackTrace();
        }

        return writeResponseBody(response, apiResponse);
    }

    private Mono<Void> writeResponseBody(ServerHttpResponse response, ApiResponse<?> apiResponse) {
        try {
            String jsonBody = objectMapper.writeValueAsString(apiResponse);
            DataBuffer buffer = response.bufferFactory().wrap(jsonBody.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer))
                    .doOnError(error -> DataBufferUtils.release(buffer));
        } catch (Exception e) {
            log.error("[GlobalExceptionHandler] Error writing response body", e);
            return response.setComplete();
        }
    }

}
