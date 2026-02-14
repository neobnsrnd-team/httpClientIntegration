package com.example.mydata.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MydataResponse<T> {

    private String resultCode;

    private String resultMessage;

    private T data;

    private String externalErrorCode;

    private String externalErrorMessage;

    public static <T> MydataResponse<T> success(T data) {
        return MydataResponse.<T>builder()
                .resultCode("0000")
                .resultMessage("성공")
                .data(data)
                .build();
    }

    public static MydataResponse<Void> externalError(String externalErrorCode, String externalErrorMessage) {
        return MydataResponse.<Void>builder()
                .resultCode("E001")
                .resultMessage("외부 시스템 오류")
                .externalErrorCode(externalErrorCode)
                .externalErrorMessage(externalErrorMessage)
                .build();
    }

    public static MydataResponse<Void> internalError(String message) {
        return MydataResponse.<Void>builder()
                .resultCode("E002")
                .resultMessage(message)
                .build();
    }

    public static MydataResponse<Void> systemError(String message) {
        return MydataResponse.<Void>builder()
                .resultCode("E999")
                .resultMessage(message)
                .build();
    }
}
