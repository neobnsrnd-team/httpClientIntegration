package com.example.mydata.client.card;

import com.example.mydata.client.config.ExternalSystemsProperties;
import com.example.mydata.client.core.GenericHttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CardClientConfig {

    @Bean
    public CardMessageClient cardMessageClient(GenericHttpClient httpClient,
                                                ExternalSystemsProperties properties,
                                                ObjectMapper objectMapper) {
        return new CardMessageClient(httpClient, properties.getCard(), objectMapper);
    }
}
