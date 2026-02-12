package com.example.mydata.client.card;

import com.example.mydata.client.core.GenericHttpClient;
import com.example.mydata.client.core.MessageClient;
import com.example.mydata.client.core.SystemProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CardMessageClient extends MessageClient {

    public CardMessageClient(GenericHttpClient httpClient, SystemProperties properties, ObjectMapper objectMapper) {
        super(httpClient, properties, objectMapper);
    }
}
