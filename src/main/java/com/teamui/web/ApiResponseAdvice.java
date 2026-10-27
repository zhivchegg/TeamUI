package com.teamui.web;

import com.teamui.dto.ApiResponse;
import com.teamui.dto.PageResponse;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Wraps successful controller responses in the common envelope
 * {@code { "status": "0", "message": "", "data": ... }}.
 *
 * <p>A {@link Page} becomes a {@link PageResponse}. A body that is already an
 * {@link ApiResponse} (auth endpoints, error handlers) is left as is. A null body
 * (for example 204) is also left as is.</p>
 *
 * @author TeamUI
 * @since 0.0.1
 */
@ControllerAdvice(basePackages = "com.teamui.web")
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (body == null || body instanceof ApiResponse<?>) {
            return body;
        }
        if (body instanceof Page<?> page) {
            return ApiResponse.success(PageResponse.from(page));
        }
        return ApiResponse.success(body);
    }
}
