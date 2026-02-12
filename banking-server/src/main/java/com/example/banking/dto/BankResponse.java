package com.example.banking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankResponse<T> {

    @JsonProperty("result_code")
    private String resultCode;

    @JsonProperty("result_msg")
    private String resultMsg;

    private T data;

    public static <T> BankResponse<T> success(T data) {
        return BankResponse.<T>builder()
                .resultCode("0000")
                .resultMsg("성공")
                .data(data)
                .build();
    }

    public static <T> BankResponse<T> error(String code, String message) {
        return BankResponse.<T>builder()
                .resultCode(code)
                .resultMsg(message)
                .build();
    }
}
