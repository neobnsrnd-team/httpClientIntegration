package com.example.mydata.client.core;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class MessageSpecProperties {

    private String transactionCode;

    private String method = "GET";

    private String path;

    private List<String> pathVariables = new ArrayList<>();

    private Map<String, String> queryParams = new LinkedHashMap<>();

    private Map<String, String> bodyFields = new LinkedHashMap<>();
}
