package com.musicplayer.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 一次性、短期、绑定用户与资源的流式访问票据。
 *
 * <p>背景：浏览器的 {@code EventSource} 无法设置请求头，所以 SSE 接口过去是用
 * {@code ?token=<JWT>} 传凭据的。把长期有效的 JWT 放进 URL 会让它泄漏到服务端访问日志、
 * 浏览器历史、Referer 以及任何中间代理里，而且泄漏后在被撤销之前一直可用。
 *
 * <p>改为：先用正常的 Authorization 头换一张票据，再用票据开流。票据
 * <ul>
 *   <li><b>一次性</b>：{@link #consume} 用 {@code Map.remove} 原子取出，重放无效；</li>
 *   <li><b>短期</b>：默认 30 秒，够开一条流，不够被翻出来复用；</li>
 *   <li><b>绑定</b>：记下签发时的 userId、角色与目标资源，拿去访问别的歌曲无效。</li>
 * </ul>
 *
 * <p>票据存在进程内存中，只对单实例部署有效；多实例需要换成共享存储。这是刻意的取舍，
 * 与 {@link com.musicplayer.config.AiRateLimitInterceptor} 一致。
 */
@Service
public class StreamTicketService {

    private static final Logger log = LoggerFactory.getLogger(StreamTicketService.class);

    /** 票据明文内容。 */
    public record Ticket(String userId, String role, String resource) {}

    /** 签发给客户端的内容：票据本身与有效期（秒）。 */
    public record Issued(String ticket, long expiresInSeconds) {}

    private record Stored(Ticket ticket, long expiresAtMillis) {}

    private final ConcurrentHashMap<String, Stored> tickets = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final AtomicLong lastSweep = new AtomicLong(System.currentTimeMillis());

    private final long ttlMillis;

    public StreamTicketService(@Value("${app.ai.stream-ticket.ttl-seconds:30}") long ttlSeconds) {
        this.ttlMillis = Math.max(1L, ttlSeconds) * 1000L;
    }

    /** 为 {@code resource} 签发一张属于 {@code userId} 的票据。 */
    public Issued issue(String userId, String role, String resource) {
        sweepIfDue();
        String id = newTicketId();
        tickets.put(id, new Stored(new Ticket(userId, role, resource),
                System.currentTimeMillis() + ttlMillis));
        return new Issued(id, ttlMillis / 1000L);
    }

    /**
     * 消费票据。无论校验是否通过，票据都会被移除，因此同一张票只能成功一次。
     *
     * @param resource 本次请求实际要访问的资源；不匹配则视为无效
     */
    public Optional<Ticket> consume(String ticketId, String resource) {
        if (ticketId == null || ticketId.isBlank()) return Optional.empty();

        // remove 是原子操作：两个并发请求只有一个能拿到非 null，天然防重放
        Stored removed = tickets.remove(ticketId);
        if (removed == null) return Optional.empty();

        if (System.currentTimeMillis() >= removed.expiresAtMillis()) {
            log.debug("流式票据已过期，拒绝");
            return Optional.empty();
        }
        if (resource != null && !resource.equals(removed.ticket().resource())) {
            log.warn("流式票据与请求资源不匹配，拒绝：票={} 请求={}",
                    removed.ticket().resource(), resource);
            return Optional.empty();
        }
        return Optional.of(removed.ticket());
    }

    private String newTicketId() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** 防止长期运行后 map 无界增长。 */
    private void sweepIfDue() {
        long now = System.currentTimeMillis();
        long last = lastSweep.get();
        if (now - last < ttlMillis || !lastSweep.compareAndSet(last, now)) return;
        tickets.entrySet().removeIf(entry -> now >= entry.getValue().expiresAtMillis());
    }

    /** 供测试观察当前未消费的票据数量。 */
    int pendingCount() {
        return tickets.size();
    }
}
