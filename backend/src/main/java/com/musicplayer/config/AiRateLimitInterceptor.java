package com.musicplayer.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 付费模型接口的限流拦截器。
 *
 * <p>背景：{@code /api/ai-dj/**} 与 {@code /api/ai/analysis/**} 会真实调用 DeepSeek /
 * SiliconFlow 的付费接口。收紧为需要登录之后，任何持有一个普通账号的人仍然可以循环调用，
 * 把调用方的余额刷光——鉴权解决"谁能用"，限流才解决"能用多少"。
 *
 * <p>实现为按主体（已登录用户）或来源地址（未登录）的固定窗口计数器。计数器保存在进程内存中，
 * 因此只对单实例部署有效；多实例部署需要换成 Redis 之类的共享存储。这一点是刻意的取舍，
 * 而不是遗漏。
 */
@Component
public class AiRateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AiRateLimitInterceptor.class);

    /** 窗口长度默认为 1 分钟；可配置以便测试与不同部署调整。 */
    private final long windowMillis;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final AtomicLong lastSweep = new AtomicLong(System.currentTimeMillis());

    private final int limitPerMinute;

    public AiRateLimitInterceptor(
            @Value("${app.ai.rate-limit.per-minute:10}") int limitPerMinute,
            @Value("${app.ai.rate-limit.window-seconds:60}") long windowSeconds) {
        this.limitPerMinute = limitPerMinute;
        this.windowMillis = Math.max(1L, windowSeconds) * 1000L;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        long now = System.currentTimeMillis();
        sweepIfDue(now);

        String key = resolveKey(request);
        Window window = windows.compute(key, (ignored, existing) -> {
            if (existing == null || now - existing.startedAt >= windowMillis) {
                return new Window(now, 1);
            }
            existing.count++;
            return existing;
        });

        if (window.count > limitPerMinute) {
            long retryAfterSeconds = Math.max(1, (windowMillis - (now - window.startedAt)) / 1000);
            log.warn("AI 接口触发限流: key={} count={} limit={}/min", key, window.count, limitPerMinute);
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.getWriter().write(
                    "{\"ok\":false,\"error\":{\"code\":\"RATE_LIMITED\",\"message\":\"请求过于频繁，请稍后再试\"}}");
            return false;
        }
        return true;
    }

    /**
     * 已登录用户按其 userId 计数（同一人换 IP 也绕不过），未登录按来源地址计数。
     */
    private String resolveKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null
                && !"anonymousUser".equals(auth.getName())) {
            return "user:" + auth.getName();
        }
        return "addr:" + request.getRemoteAddr();
    }

    /** 防止长期运行后 map 无界增长。 */
    private void sweepIfDue(long now) {
        long last = lastSweep.get();
        if (now - last < windowMillis || !lastSweep.compareAndSet(last, now)) return;
        windows.entrySet().removeIf(entry -> now - entry.getValue().startsWith() >= windowMillis);
    }

    /** 供测试读取当前计数。 */
    int countFor(String key) {
        Window window = windows.get(key);
        return window == null ? 0 : window.count;
    }

    /** 供测试清空状态。 */
    void reset() {
        windows.clear();
    }

    int limitPerMinute() {
        return limitPerMinute;
    }

    private static final class Window {
        final long startedAt;
        int count;

        Window(long startedAt, int count) {
            this.startedAt = startedAt;
            this.count = count;
        }

        long startsWith() {
            return startedAt;
        }
    }
}
