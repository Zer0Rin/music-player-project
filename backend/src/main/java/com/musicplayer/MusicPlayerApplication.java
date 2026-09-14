package com.musicplayer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

import org.springframework.scheduling.annotation.EnableScheduling;

import org.springframework.scheduling.annotation.EnableAsync;

// 排除 UserDetailsServiceAutoConfiguration：本应用是纯 JWT 的无状态 API，没有表单登录，
// 也不需要 AuthenticationManager/UserDetailsService（鉴权全部走自建的 UserService + JwtAuthFilter）。
// 不排除的话，Spring Boot 每次启动都会生成一个名为 user 的内存账号，并把随机密码以 WARN 级别
// 打进日志——那是一个用不上、但会被误当凭据的账号，也污染启动日志。
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableScheduling //开启定时任务
@EnableAsync//异步扫描
public class MusicPlayerApplication {
    public static void main(String[] args) {
        SpringApplication.run(MusicPlayerApplication.class, args);
    }
}
