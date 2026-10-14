package com.lecture.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private static final ZoneId SERVICE_ZONE_ID = ZoneId.of("Asia/Seoul");

    private T data;
    private String code;
    private String message;
    private OffsetDateTime timestamp;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, null, null, OffsetDateTime.now(SERVICE_ZONE_ID));
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(null, code, message, OffsetDateTime.now(SERVICE_ZONE_ID));
    }
}
