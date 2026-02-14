package com.example.mydata.client.giro;

import com.example.mydata.client.config.ExternalSystemsProperties;
import com.example.mydata.client.core.GenericHttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GiroClientConfig {

    @Bean
    public GiroMessageClient giroMessageClient(GenericHttpClient httpClient,
                                                ExternalSystemsProperties properties,
                                                ObjectMapper objectMapper) {
        return new GiroMessageClient(httpClient, properties.getGiro(), objectMapper);
    }
}
