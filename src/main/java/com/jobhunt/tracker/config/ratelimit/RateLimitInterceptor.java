package com.jobhunt.tracker.config.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final int maxRequests;
    private final long windowMillis;
    private final ObjectMapper objectMapper;

    private record RequestWindow(AtomicInteger count, long windowStart) {
    }

    private final Map<String, RequestWindow> store = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) throws Exception {

        String key = getClientKey(request);
        long now = System.currentTimeMillis();

        store.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStart() > windowMillis) {
                return new RequestWindow(new AtomicInteger(1), now);
            }
            existing.count().incrementAndGet();
            return existing;
        });

        RequestWindow window = store.get(key);
        if (window.count().get() > maxRequests) {
            log.warn("Rate limit exceeded for key={} path={}", key, request.getRequestURI());
            writeRateLimitResponse(response, request);
            return false;
        }

        return true;
    }

    private String getClientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
        return ip + ":" + request.getRequestURI();
    }

    private void writeRateLimitResponse(
            HttpServletResponse response,
            HttpServletRequest request) throws Exception {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many requests. Please try again later."
        );
        problem.setType(URI.create("https://jobhunt.io/errors/too-many-requests"));
        problem.setTitle("Too Many Requests");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("errorCode", "TOO_MANY_REQUESTS");
        problem.setProperty("timestamp", LocalDateTime.now().toString());

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(problem));
    }
}