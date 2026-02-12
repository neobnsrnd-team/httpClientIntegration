package com.example.card.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CardResponse<T> {

    private String status;

    private String message;

    @JsonProperty("error_code")
    private String errorCode;

    private T payload;

    public static <T> CardResponse<T> success(T payload) {
        return CardResponse.<T>builder()
                .status("SUCCESS")
                .message("처리완료")
                .payload(payload)
                .build();
    }

    public static <T> CardResponse<T> error(String errorCode, String message) {
        return CardResponse.<T>builder()
                .status("FAIL")
                .message(message)
                .errorCode(errorCode)
                .build();
    }
}
