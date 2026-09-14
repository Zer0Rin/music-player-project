package com.musicplayer.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 启动期的密钥策略检查。
 *
 * <p>历史问题：{@code app.jwt.secret} 与 {@code app.admin.password} 曾把本地演示用的
 * 默认值直接提交到公开仓库。JWT 的签名密钥一旦公开，任何人都可以离线伪造任意
 * userId / role 的合法 token，{@code hasRole("ADMIN")} 之类的方法级鉴权随之失效。
 *
 * <p>因此这里做两件事：
 * <ol>
 *   <li>非 prod 环境使用默认值时打印醒目告警，避免“悄悄上线”；</li>
 *   <li>prod 环境使用默认值或密钥长度不足时直接拒绝启动（fail fast），
 *       而不是带着一个可伪造的密钥对外服务。</li>
 * </ol>
 */
@Component
public class SecretPolicy {

    private static final Logger log = LoggerFactory.getLogger(SecretPolicy.class);

    /** 与 application.yml 中的默认值保持一致，仅用于检测“未配置”。 */
    static final String DEV_JWT_SECRET = "dev-only-insecure-secret-change-me-please-32b";
    static final String DEV_ADMIN_PASSWORD = "admin123";

    /** HS256 要求密钥至少 256 bit。 */
    private static final int MIN_SECRET_BYTES = 32;

    private final Environment environment;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.admin.password}")
    private String adminPassword;

    public SecretPolicy(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void verify() {
        boolean production = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.startsWith("prod"));

        if (production) {
            if (DEV_JWT_SECRET.equals(jwtSecret)) {
                throw new IllegalStateException(
                        "REFUSING_TO_START: app.jwt.secret 仍是仓库中的默认值。"
                                + " 请设置环境变量 JWT_SECRET（openssl rand -base64 48）。");
            }
            if (DEV_ADMIN_PASSWORD.equals(adminPassword)) {
                throw new IllegalStateException(
                        "REFUSING_TO_START: app.admin.password 仍是仓库中的默认值。"
                                + " 请设置环境变量 ADMIN_PASSWORD。");
            }
        }

        int secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8).length;
        if (secretBytes < MIN_SECRET_BYTES) {
            String message = "app.jwt.secret 只有 " + secretBytes
                    + " 字节，HS256 至少需要 " + MIN_SECRET_BYTES + " 字节。";
            if (production) {
                throw new IllegalStateException("REFUSING_TO_START: " + message);
            }
            log.warn("SECURITY: {}", message);
        }

        if (!production && (DEV_JWT_SECRET.equals(jwtSecret) || DEV_ADMIN_PASSWORD.equals(adminPassword))) {
            log.warn("SECURITY: 正在使用仓库中的演示密钥（jwt.secret / admin.password）。"
                    + " 仅可用于本地演示，请勿用此配置对外提供服务。");
        }
    }
}
