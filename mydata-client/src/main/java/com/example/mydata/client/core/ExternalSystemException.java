package com.example.mydata.client.core;

import lombok.Getter;

@Getter
public class ExternalSystemException extends RuntimeException {

    private final String errorCode;
    private final String errorMessage;

    public ExternalSystemException(String errorCode, String errorMessage) {
        super("[" + errorCode + "] " + errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public ExternalSystemException(String errorCode, String errorMessage, Throwable cause) {
        super("[" + errorCode + "] " + errorMessage, cause);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
