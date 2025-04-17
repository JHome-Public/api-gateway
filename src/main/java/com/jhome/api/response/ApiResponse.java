package com.jhome.api.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
@RequiredArgsConstructor
public class ApiResponse<D> {

    private final int code;
    private final String message;
    private D data;

    public static ApiResponse<?> success() {
        return new ApiResponse<>(ApiResponseCode.SUCCESS.getCode(), ApiResponseCode.SUCCESS.getMessage());
    }

    public static <D> ApiResponse<?> success(D data) {
        return new ApiResponse<>(ApiResponseCode.SUCCESS.getCode(), ApiResponseCode.SUCCESS.getMessage(), data);
    }

    public static ApiResponse<?> fail(ApiResponseCode apiResponseCode) {
        return new ApiResponse<>(apiResponseCode.getCode(), apiResponseCode.getMessage());
    }

    public static <D> ApiResponse<?> fail(ApiResponseCode apiResponseCode, D data) {
        return new ApiResponse<>(apiResponseCode.getCode(), apiResponseCode.getMessage(), data);
    }

}
