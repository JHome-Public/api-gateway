package com.jhome.api.common.exception;

import com.jhome.api.common.response.ApiResponseCode;
import lombok.Getter;

@Getter
public class CustomException extends RuntimeException{

    private final ApiResponseCode apiResponseCode;

    public CustomException(ApiResponseCode apiResponseCode) {
        super(apiResponseCode.getMessage());
        this.apiResponseCode = apiResponseCode;
    }
}
