package com.musicplayer.controller;

import reactor.core.Disposable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 保证 SSE 客户端断开时，上游模型流被释放。
 *
 * <p>为什么需要它：{@code chatClient...stream().subscribe()} 会把模型调用挂在 Reactor 上持续产生
 * token（也就是持续产生费用）。SSE 的 emitter 完成/断开**不会**自动取消这个上游订阅——
 * 不做处理的话，用户关掉标签页后模型仍会跑完，账单照记。
 *
 * <p>它同时要处理一个真实的竞态：**断开可能发生在订阅建立之前**。
 * 订阅是在异步线程里建立的（{@code subscribe()} 返回后才拿得到句柄），而
 * {@code onCompletion/onError} 可能在更早的线程上先触发。只存一个引用会漏掉
 * "先断开、后建立"的顺序，导致订阅建立后无人取消、一直跑到底。
 *
 * <p>因此用 {@code released} 标志 + 双检：断开早于登记时，{@link #register} 自己立刻取消。
 */
public class UpstreamSubscriptionGuard {

    private final AtomicReference<Disposable> subscription = new AtomicReference<>();
    private final AtomicBoolean released = new AtomicBoolean(false);

    /**
     * 登记已建立的上游订阅。如果此前已经释放过（客户端早于订阅建立就断开了），
     * 则立即取消，不留悬挂。
     */
    public void register(Disposable disposable) {
        if (disposable == null) return;
        if (released.get()) {
            disposable.dispose();
            return;
        }
        subscription.set(disposable);
        // 双检：register 与 release 可能交错，这里再确认一次
        if (released.get()) {
            Disposable raced = subscription.getAndSet(null);
            if (raced != null) raced.dispose();
        }
    }

    /**
     * 释放上游订阅。幂等，可从多个回调重复调用。
     *
     * @return 是否真的取消了一个已建立的订阅（用于区分"确实拦下了计费"与"本来就没有流"）
     */
    public boolean release() {
        released.set(true);
        Disposable disposable = subscription.getAndSet(null);
        if (disposable == null) return false;
        disposable.dispose();
        return true;
    }

    public boolean isReleased() {
        return released.get();
    }
}
