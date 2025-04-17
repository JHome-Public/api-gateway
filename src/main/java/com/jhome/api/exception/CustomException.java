package com.jhome.api.exception;

import com.jhome.api.response.ApiResponseCode;
import lombok.Getter;

@Getter
public class CustomException extends RuntimeException{

    private final ApiResponseCode apiResponseCode;

    public CustomException(ApiResponseCode apiResponseCode) {
        super(apiResponseCode.getMessage());
        this.apiResponseCode = apiResponseCode;
    }
}
