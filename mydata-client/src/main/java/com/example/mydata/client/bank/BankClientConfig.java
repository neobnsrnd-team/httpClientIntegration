package com.example.mydata.client.bank;

import com.example.mydata.client.config.ExternalSystemsProperties;
import com.example.mydata.client.core.GenericHttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BankClientConfig {

    @Bean
    public BankMessageClient bankMessageClient(GenericHttpClient httpClient,
                                                ExternalSystemsProperties properties,
                                                ObjectMapper objectMapper) {
        return new BankMessageClient(httpClient, properties.getBank(), objectMapper);
    }
}
