package com.musicplayer.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link StreamTicketService} 的契约验证。
 *
 * <p>这套票据是"不让 JWT 进入 URL"的替代方案，所以它的安全属性必须被钉死：
 * 一次性、会过期、绑定资源。任何一条失效，URL 里的凭据泄漏问题就会以另一种形式回来。
 */
class StreamTicketServiceTest {

    private static final long TTL_SECONDS = 1L;

    private final StreamTicketService service = new StreamTicketService(TTL_SECONDS);

    @Test
    @DisplayName("签发的票据可以被消费，且带回签发时的用户、角色与资源")
    void issuedTicketCarriesIdentityAndResource() {
        StreamTicketService.Issued issued = service.issue("user-1", "ADMIN", "song-42");

        assertTrue(issued.ticket().length() >= 32, "票据应是足够长的随机串");
        assertEquals(TTL_SECONDS, issued.expiresInSeconds());

        Optional<StreamTicketService.Ticket> consumed = service.consume(issued.ticket(), "song-42");
        assertTrue(consumed.isPresent());
        assertEquals("user-1", consumed.get().userId());
        assertEquals("ADMIN", consumed.get().role());
        assertEquals("song-42", consumed.get().resource());
    }

    @Test
    @DisplayName("票据是一次性的：第二次消费失败（防重放）")
    void ticketCanOnlyBeConsumedOnce() {
        StreamTicketService.Issued issued = service.issue("user-1", "USER", "song-42");

        assertTrue(service.consume(issued.ticket(), "song-42").isPresent());
        assertTrue(service.consume(issued.ticket(), "song-42").isEmpty(),
                "同一张票不应能第二次使用");
    }

    @Test
    @DisplayName("票据与资源绑定：拿去访问别的歌曲无效，且该票随即作废")
    void ticketIsBoundToItsResource() {
        StreamTicketService.Issued issued = service.issue("user-1", "USER", "song-42");

        assertTrue(service.consume(issued.ticket(), "song-99").isEmpty(),
                "不该能拿 song-42 的票去解析 song-99");
        assertTrue(service.consume(issued.ticket(), "song-42").isEmpty(),
                "资源不匹配也应消耗掉票据，不能留着再用");
    }

    @Test
    @DisplayName("过期的票据被拒绝")
    void expiredTicketIsRejected() throws Exception {
        StreamTicketService.Issued issued = service.issue("user-1", "USER", "song-42");
        Thread.sleep(TTL_SECONDS * 1000L + 250L);

        assertTrue(service.consume(issued.ticket(), "song-42").isEmpty(),
                "超过 TTL 的票据应失效");
    }

    @Test
    @DisplayName("空票据与未知票据被拒绝，且不抛异常")
    void blankOrUnknownTicketIsRejected() {
        assertTrue(service.consume(null, "song-42").isEmpty());
        assertTrue(service.consume("", "song-42").isEmpty());
        assertTrue(service.consume("   ", "song-42").isEmpty());
        assertTrue(service.consume("not-a-real-ticket", "song-42").isEmpty());
    }

    @Test
    @DisplayName("并发消费同一张票时只有一个能成功")
    void concurrentConsumeYieldsExactlyOneWinner() throws Exception {
        StreamTicketService.Issued issued = service.issue("user-1", "USER", "song-42");

        int threads = 8;
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicInteger winners = new java.util.concurrent.atomic.AtomicInteger();
        java.util.List<Thread> pool = new java.util.ArrayList<>();

        for (int i = 0; i < threads; i++) {
            Thread t = new Thread(() -> {
                try {
                    start.await();
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                if (service.consume(issued.ticket(), "song-42").isPresent()) {
                    winners.incrementAndGet();
                }
            });
            pool.add(t);
            t.start();
        }
        start.countDown();
        for (Thread t : pool) t.join();

        assertEquals(1, winners.get(), "8 个并发请求只应有 1 个拿到这张一次性票据");
    }

    @Test
    @DisplayName("每张票据彼此独立")
    void ticketsAreIndependent() {
        StreamTicketService.Issued a = service.issue("user-1", "USER", "song-1");
        StreamTicketService.Issued b = service.issue("user-1", "USER", "song-1");

        assertNotEquals(a.ticket(), b.ticket(), "两次签发的票据不应相同");
        assertTrue(service.consume(a.ticket(), "song-1").isPresent());
        assertTrue(service.consume(b.ticket(), "song-1").isPresent());
    }
}
