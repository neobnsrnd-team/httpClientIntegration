package com.example.mydata.client.core;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class SystemProperties {

    private String baseUrl;

    private String successCodeField;

    private String successCodeValue;

    private String errorMessageField;

    private String dataField;

    private int connectTimeout = 5000;

    private int readTimeout = 10000;

    private Map<String, MessageSpecProperties> messages = new LinkedHashMap<>();
}
