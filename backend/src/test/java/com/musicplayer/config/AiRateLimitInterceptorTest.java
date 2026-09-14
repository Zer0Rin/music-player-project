package com.musicplayer.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link AiRateLimitInterceptor} 的行为验证。
 *
 * <p>这些接口会真实产生模型费用，所以限流是成本控制的一部分，而不是可选优化：
 * 鉴权只决定"谁能用"，限流才决定"能用多少"。
 *
 * <p>不启动 Spring 上下文，只验证拦截器自身的计数与响应契约，因此不需要数据库。
 */
class AiRateLimitInterceptorTest {

    private static final int LIMIT = 3;
    private static final long WINDOW_SECONDS = 1L;

    private AiRateLimitInterceptor interceptor = new AiRateLimitInterceptor(LIMIT, WINDOW_SECONDS);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String userId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private MockHttpServletRequest requestFrom(String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        return request;
    }

    @Test
    @DisplayName("窗口内放行前 N 次，第 N+1 次返回 429 且带 Retry-After 与错误码")
    void blocksOnceTheWindowBudgetIsSpent() throws Exception {
        authenticateAs("user-1");

        for (int i = 0; i < LIMIT; i++) {
            MockHttpServletResponse ok = new MockHttpServletResponse();
            assertTrue(interceptor.preHandle(requestFrom("10.0.0.1"), ok, new Object()),
                    "第 " + (i + 1) + " 次应当放行");
            assertEquals(200, ok.getStatus());
        }

        MockHttpServletResponse blocked = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(requestFrom("10.0.0.1"), blocked, new Object()));

        assertEquals(429, blocked.getStatus());
        assertTrue(blocked.getHeaderValue("Retry-After") != null,
                "应返回 Retry-After 让调用方知道何时重试");
        assertTrue(blocked.getContentAsString().contains("RATE_LIMITED"),
                "响应体应包含可识别的错误码，实际为: " + blocked.getContentAsString());
    }

    @Test
    @DisplayName("额度按用户独立计算：一个人刷满不影响另一个人")
    void countsEachUserSeparately() throws Exception {
        authenticateAs("user-1");
        for (int i = 0; i < LIMIT; i++) {
            interceptor.preHandle(requestFrom("10.0.0.1"), new MockHttpServletResponse(), new Object());
        }
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(requestFrom("10.0.0.1"), blocked, new Object()));

        // 换用户，同一个来源地址也应重新获得完整额度
        authenticateAs("user-2");
        MockHttpServletResponse other = new MockHttpServletResponse();
        assertTrue(interceptor.preHandle(requestFrom("10.0.0.1"), other, new Object()),
                "另一个用户不应被前一个用户的用量牵连");
    }

    @Test
    @DisplayName("未登录调用按来源地址计数，换地址不共享额度")
    void anonymousCallersFallBackToRemoteAddress() throws Exception {
        for (int i = 0; i < LIMIT; i++) {
            assertTrue(interceptor.preHandle(requestFrom("10.0.0.9"), new MockHttpServletResponse(), new Object()));
        }
        assertFalse(interceptor.preHandle(requestFrom("10.0.0.9"), new MockHttpServletResponse(), new Object()));

        assertTrue(interceptor.preHandle(requestFrom("10.0.0.10"), new MockHttpServletResponse(), new Object()),
                "不同来源地址应有独立额度");
    }

    @Test
    @DisplayName("窗口过期后额度恢复")
    void budgetRecoversAfterTheWindowExpires() throws Exception {
        authenticateAs("user-1");
        for (int i = 0; i < LIMIT; i++) {
            interceptor.preHandle(requestFrom("10.0.0.1"), new MockHttpServletResponse(), new Object());
        }
        assertFalse(interceptor.preHandle(requestFrom("10.0.0.1"), new MockHttpServletResponse(), new Object()),
                "窗口内应已被限流");

        Thread.sleep(WINDOW_SECONDS * 1000L + 250L);

        assertTrue(interceptor.preHandle(requestFrom("10.0.0.1"), new MockHttpServletResponse(), new Object()),
                "窗口过期后应重新放行");
    }

    @Test
    @DisplayName("默认上限为 10 次/分钟（不依赖 application.yml 的注入值）")
    void defaultLimitIsTenPerMinute() {
        assertEquals(10, new AiRateLimitInterceptor(10, 60).limitPerMinute());
    }
}
