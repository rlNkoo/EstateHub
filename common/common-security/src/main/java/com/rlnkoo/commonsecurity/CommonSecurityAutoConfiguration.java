package com.rlnkoo.commonsecurity;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

@AutoConfiguration
public class CommonSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return JwtAuthenticationConverterFactory.create();
    }
}