package com.jhome.api.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "spring.jwt")
public class JwtProperty {

    private String accessKey;
    private String refreshKey;
    private Long accessAgeMS;
    private Long refreshAgeMS;
    private String prefix;
    private String secret;

}