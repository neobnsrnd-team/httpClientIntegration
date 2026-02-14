package com.example.mydata.client.insurance;

import com.example.mydata.client.core.GenericHttpClient;
import com.example.mydata.client.core.MessageClient;
import com.example.mydata.client.core.SystemProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

public class InsuranceMessageClient extends MessageClient {

    public InsuranceMessageClient(GenericHttpClient httpClient, SystemProperties properties, ObjectMapper objectMapper) {
        super(httpClient, properties, objectMapper);
    }
}
