package com.musicplayer.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.Disposable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link UpstreamSubscriptionGuard} 的契约验证。
 *
 * <p>它存在的意义是"客户端断开就停止计费"，所以关键就是两件事：
 * <b>断开之后订阅必须被 dispose</b>，以及<b>断开的时机早于订阅建立时也不能漏</b>。
 * 后者是真实竞态（订阅在异步线程建立，而断开回调可能在更早的线程触发），
 * 只存一个引用会漏掉这个顺序。
 */
class UpstreamSubscriptionGuardTest {

    /** 可观察的 Disposable。 */
    private record FakeSubscription(AtomicBoolean disposed, AtomicInteger disposeCount) implements Disposable {
        static FakeSubscription create() {
            return new FakeSubscription(new AtomicBoolean(false), new AtomicInteger());
        }

        @Override
        public void dispose() {
            disposed.set(true);
            disposeCount.incrementAndGet();
        }

        @Override
        public boolean isDisposed() {
            return disposed.get();
        }
    }

    @Test
    @DisplayName("先登记订阅、后断开 → 订阅被取消，release 返回 true")
    void releaseAfterRegisterDisposes() {
        UpstreamSubscriptionGuard guard = new UpstreamSubscriptionGuard();
        FakeSubscription subscription = FakeSubscription.create();

        guard.register(subscription);
        assertFalse(subscription.isDisposed(), "登记阶段不应取消");

        assertTrue(guard.release(), "应报告确实取消了一个已建立的订阅");
        assertTrue(subscription.isDisposed());
    }

    @Test
    @DisplayName("★ 先断开、后建立订阅 → register 就地取消（竞态不漏）")
    void releaseBeforeRegisterStillDisposes() {
        UpstreamSubscriptionGuard guard = new UpstreamSubscriptionGuard();

        // 客户端在 subscribe() 返回之前就断开了
        assertFalse(guard.release(), "此时还没有订阅，应报告没有取消到东西");

        FakeSubscription late = FakeSubscription.create();
        guard.register(late);

        assertTrue(late.isDisposed(),
                "断开早于登记时，register 必须就地取消，否则模型会一直跑到自然结束");
        assertEquals(1, late.disposeCount().get(), "不应重复取消");
    }

    @Test
    @DisplayName("release 幂等：重复调用只有第一次返回 true")
    void releaseIsIdempotent() {
        UpstreamSubscriptionGuard guard = new UpstreamSubscriptionGuard();
        FakeSubscription subscription = FakeSubscription.create();
        guard.register(subscription);

        assertTrue(guard.release());
        assertFalse(guard.release(), "第二次不应再报告取消");
        assertFalse(guard.release());
        assertEquals(1, subscription.disposeCount().get());
        assertTrue(guard.isReleased());
    }

    @Test
    @DisplayName("没有订阅时 release 返回 false，且不抛异常")
    void releaseWithoutSubscriptionIsSafe() {
        UpstreamSubscriptionGuard guard = new UpstreamSubscriptionGuard();
        assertFalse(guard.release());
        assertTrue(guard.isReleased());
    }

    @Test
    @DisplayName("register(null) 不抛异常")
    void registerNullIsSafe() {
        UpstreamSubscriptionGuard guard = new UpstreamSubscriptionGuard();
        guard.register(null);
        assertFalse(guard.release(), "空登记后不应有可取消的订阅");
    }

    @Test
    @DisplayName("★ 并发 register / release：订阅最终一定被取消，且只取消一次")
    void concurrentRegisterAndReleaseAlwaysEndsDisposed() throws Exception {
        for (int round = 0; round < 200; round++) {
            UpstreamSubscriptionGuard guard = new UpstreamSubscriptionGuard();
            FakeSubscription subscription = FakeSubscription.create();
            CountDownLatch start = new CountDownLatch(1);

            Thread releaser = new Thread(() -> {
                try {
                    start.await();
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                guard.release();
            });
            Thread registerer = new Thread(() -> {
                try {
                    start.await();
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                guard.register(subscription);
            });

            releaser.start();
            registerer.start();
            start.countDown();
            releaser.join();
            registerer.join();

            assertTrue(subscription.isDisposed(),
                    "第 " + round + " 轮：并发下漏掉了取消，模型会继续计费");
            assertTrue(subscription.disposeCount().get() <= 2,
                    "第 " + round + " 轮：取消次数异常 " + subscription.disposeCount().get());
        }
    }
}
