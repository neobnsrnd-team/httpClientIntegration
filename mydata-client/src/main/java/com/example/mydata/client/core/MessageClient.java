package com.example.mydata.client.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public abstract class MessageClient {

    protected final GenericHttpClient httpClient;
    protected final SystemProperties properties;
    protected final ObjectMapper objectMapper;
    private final Map<String, MessageSpecProperties> transactionCodeMap;

    protected MessageClient(GenericHttpClient httpClient, SystemProperties properties, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.transactionCodeMap = buildTransactionCodeMap(properties);
    }

    private Map<String, MessageSpecProperties> buildTransactionCodeMap(SystemProperties props) {
        Map<String, MessageSpecProperties> map = new LinkedHashMap<>();
        for (Map.Entry<String, MessageSpecProperties> entry : props.getMessages().entrySet()) {
            String key = entry.getValue().getTransactionCode();
            if (key == null || key.isBlank()) {
                key = entry.getKey();
            }
            map.put(key, entry.getValue());
        }
        return map;
    }

    /**
     * 거래코드와 파라미터로 외부 시스템에 메시지를 전송하고 응답을 반환한다.
     *
     * @param transactionCode 거래코드 (예: "계좌목록조회", "결제예정금액조회")
     * @param params          요청 파라미터
     * @return 응답 데이터 (data/payload 영역)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> request(String transactionCode, Map<String, Object> params) {
        MessageSpecProperties spec = transactionCodeMap.get(transactionCode);
        if (spec == null) {
            throw new IllegalArgumentException("등록되지 않은 거래코드: " + transactionCode);
        }

        // 1. URL 조립
        String url = buildUrl(spec, params);

        // 2. Request Body 조립 (POST/PUT)
        Object body = null;
        HttpMethod method = HttpMethod.valueOf(spec.getMethod().toUpperCase());
        if (method == HttpMethod.POST || method == HttpMethod.PUT) {
            body = buildBody(spec, params);
        }

        // 3. HTTP 실행
        String responseBody = httpClient.execute(url, method, body);

        // 4. 응답 파싱
        return parseResponse(responseBody);
    }

    private String buildUrl(MessageSpecProperties spec, Map<String, Object> params) {
        String url = properties.getBaseUrl() + spec.getPath();

        // Path variable 치환
        for (String pathVar : spec.getPathVariables()) {
            Object value = params.get(pathVar);
            if (value == null) {
                throw new IllegalArgumentException("필수 경로변수 누락: " + pathVar);
            }
            url = url.replace("{" + pathVar + "}", String.valueOf(value));
        }

        // Query parameter 조립
        if (!spec.getQueryParams().isEmpty()) {
            StringBuilder sb = new StringBuilder(url);
            boolean first = true;
            for (Map.Entry<String, String> entry : spec.getQueryParams().entrySet()) {
                Object value = params.get(entry.getValue());
                if (value != null) {
                    sb.append(first ? "?" : "&");
                    sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
                    sb.append("=");
                    sb.append(URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8));
                    first = false;
                }
            }
            url = sb.toString();
        }

        return url;
    }

    private Map<String, Object> buildBody(MessageSpecProperties spec, Map<String, Object> params) {
        if (spec.getBodyFields().isEmpty()) {
            return new LinkedHashMap<>(params);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : spec.getBodyFields().entrySet()) {
            Object value = params.get(entry.getValue());
            if (value != null) {
                body.put(entry.getKey(), value);
            }
        }
        return body;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseResponse(String responseBody) {
        try {
            Map<String, Object> fullResponse = objectMapper.readValue(responseBody,
                    objectMapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class));

            // 응답코드 확인
            Object codeValue = fullResponse.get(properties.getSuccessCodeField());
            if (!properties.getSuccessCodeValue().equals(String.valueOf(codeValue))) {
                String errorMsg = String.valueOf(
                        fullResponse.getOrDefault(properties.getErrorMessageField(), "알 수 없는 오류"));
                throw new ExternalSystemException(String.valueOf(codeValue), errorMsg);
            }

            // 데이터 영역 추출
            Object data = fullResponse.get(properties.getDataField());
            if (data instanceof Map) {
                return (Map<String, Object>) data;
            } else if (data instanceof List) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("items", data);
                return result;
            } else if (data == null) {
                return new LinkedHashMap<>();
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("value", data);
            return result;

        } catch (JsonProcessingException e) {
            throw new ExternalSystemException("PARSE_ERROR", "응답 파싱 실패: " + e.getMessage(), e);
        }
    }
}
