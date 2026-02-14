package com.example.mydata.client.insurance;

import com.example.mydata.client.config.ExternalSystemsProperties;
import com.example.mydata.client.core.GenericHttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InsuranceClientConfig {

    @Bean
    public InsuranceMessageClient insuranceMessageClient(GenericHttpClient httpClient,
                                                          ExternalSystemsProperties properties,
                                                          ObjectMapper objectMapper) {
        return new InsuranceMessageClient(httpClient, properties.getInsurance(), objectMapper);
    }
}
