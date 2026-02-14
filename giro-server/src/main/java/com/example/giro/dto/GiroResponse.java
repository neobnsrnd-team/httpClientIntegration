package com.example.giro.dto;

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
public class GiroResponse<T> {

    @JsonProperty("rsp_cd")
    private String rspCd;

    @JsonProperty("rsp_msg")
    private String rspMsg;

    @JsonProperty("rsp_data")
    private T rspData;

    public static <T> GiroResponse<T> success(T data) {
        return GiroResponse.<T>builder()
                .rspCd("000")
                .rspMsg("정상처리")
                .rspData(data)
                .build();
    }

    public static <T> GiroResponse<T> error(String code, String msg) {
        return GiroResponse.<T>builder()
                .rspCd(code)
                .rspMsg(msg)
                .build();
    }
}
