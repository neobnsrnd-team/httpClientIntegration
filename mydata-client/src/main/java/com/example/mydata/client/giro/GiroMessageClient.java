package com.example.mydata.client.giro;

import com.example.mydata.client.core.GenericHttpClient;
import com.example.mydata.client.core.MessageClient;
import com.example.mydata.client.core.SystemProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GiroMessageClient extends MessageClient {

    public GiroMessageClient(GenericHttpClient httpClient, SystemProperties properties, ObjectMapper objectMapper) {
        super(httpClient, properties, objectMapper);
    }
}
