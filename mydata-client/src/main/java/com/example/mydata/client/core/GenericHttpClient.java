package com.example.mydata.client.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class GenericHttpClient {

    private final RestClient restClient;

    public GenericHttpClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public String execute(String url, HttpMethod method, Object body) {
        log.debug("HTTP Request: {} {} body={}", method, url, body);

        try {
            RestClient.RequestBodySpec requestSpec = restClient.method(method)
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON);

            if (body != null) {
                requestSpec.body(body);
            }

            String responseBody = requestSpec.retrieve().body(String.class);
            log.debug("HTTP Response: body={}", responseBody);
            return responseBody;
        } catch (ResourceAccessException e) {
            throw new ExternalSystemException("CONNECTION_ERROR",
                    "외부 시스템 연결 실패: " + e.getMessage(), e);
        }
    }
}
