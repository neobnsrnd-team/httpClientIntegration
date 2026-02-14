package com.example.mydata.controller;

import com.example.mydata.client.core.ExternalSystemException;
import com.example.mydata.dto.MydataResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ExternalSystemException.class)
    public ResponseEntity<MydataResponse<Void>> handleExternalSystemException(ExternalSystemException e) {
        log.warn("외부 시스템 오류: [{}] {}", e.getErrorCode(), e.getErrorMessage());
        return ResponseEntity.badRequest().body(
                MydataResponse.externalError(e.getErrorCode(), e.getErrorMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MydataResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("내부 요청 오류: {}", e.getMessage());
        return ResponseEntity.badRequest().body(
                MydataResponse.internalError(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MydataResponse<Void>> handleException(Exception e) {
        log.error("시스템 오류 발생", e);
        return ResponseEntity.internalServerError().body(
                MydataResponse.systemError("시스템 오류가 발생했습니다"));
    }
}
