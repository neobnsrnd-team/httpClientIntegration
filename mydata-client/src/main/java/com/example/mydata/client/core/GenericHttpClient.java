package com.example.mydata.client.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
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
        } catch (HttpClientErrorException e) {
            int statusCode = e.getStatusCode().value();
            String errorCode;
            String errorMsg;
            if (statusCode == 404) {
                errorCode = "NOT_FOUND";
                errorMsg = "외부 시스템 리소스를 찾을 수 없습니다";
            } else if (statusCode == 400) {
                errorCode = "BAD_REQUEST";
                errorMsg = "외부 시스템 요청이 잘못되었습니다";
            } else {
                errorCode = "HTTP_" + statusCode;
                errorMsg = "외부 시스템 클라이언트 오류";
            }
            log.error("HTTP Client Error: {} {} - {}", statusCode, e.getStatusText(), e.getResponseBodyAsString());
            throw new ExternalSystemException(errorCode,
                    errorMsg + " (" + statusCode + ")", e);
        } catch (HttpServerErrorException e) {
            int statusCode = e.getStatusCode().value();
            log.error("HTTP Server Error: {} {} - {}", statusCode, e.getStatusText(), e.getResponseBodyAsString());
            throw new ExternalSystemException("SERVER_ERROR",
                    "외부 시스템 서버 오류 (" + statusCode + ")", e);
        } catch (ResourceAccessException e) {
            throw new ExternalSystemException("CONNECTION_ERROR",
                    "외부 시스템 연결 실패: " + e.getMessage(), e);
        }
    }
}
