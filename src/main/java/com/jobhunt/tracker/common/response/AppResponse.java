package com.jobhunt.tracker.common.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppResponse<T> {

    private final boolean success;
    private final int statusCode;
    private final String message;
    private final T data;
    private final PageMetadata metadata;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime timestamp;

    public static <T> AppResponse<T> success(T data) {
        return AppResponse.<T>builder()
                .success(true)
                .statusCode(200)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> AppResponse<T> success(String message, T data) {
        return AppResponse.<T>builder()
                .success(true)
                .statusCode(200)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> AppResponse<T> success(String message) {
        return AppResponse.<T>builder()
                .success(true)
                .statusCode(200)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> AppResponse<T> created(String message, T data) {
        return AppResponse.<T>builder()
                .success(true)
                .statusCode(201)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> AppResponse<List<T>> paginated(Page<T> page) {
        return AppResponse.<List<T>>builder()
                .success(true)
                .statusCode(200)
                .data(page.getContent())
                .metadata(PageMetadata.of(page))
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> AppResponse<List<T>> paginated(String message, Page<T> page) {
        return AppResponse.<List<T>>builder()
                .success(true)
                .statusCode(200)
                .message(message)
                .data(page.getContent())
                .metadata(PageMetadata.of(page))
                .timestamp(LocalDateTime.now())
                .build();
    }
}