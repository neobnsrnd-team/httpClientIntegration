package com.example.mydata.client.bank;

import com.example.mydata.client.core.GenericHttpClient;
import com.example.mydata.client.core.MessageClient;
import com.example.mydata.client.core.SystemProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BankMessageClient extends MessageClient {

    public BankMessageClient(GenericHttpClient httpClient, SystemProperties properties, ObjectMapper objectMapper) {
        super(httpClient, properties, objectMapper);
    }
}
