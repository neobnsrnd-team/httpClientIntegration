package com.example.insurance.dto;

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
public class InsuranceResponse<T> {

    private String code;

    private String msg;

    private T result;

    public static <T> InsuranceResponse<T> success(T result) {
        return InsuranceResponse.<T>builder()
                .code("00")
                .msg("정상처리")
                .result(result)
                .build();
    }

    public static <T> InsuranceResponse<T> error(String code, String msg) {
        return InsuranceResponse.<T>builder()
                .code(code)
                .msg(msg)
                .build();
    }
}
