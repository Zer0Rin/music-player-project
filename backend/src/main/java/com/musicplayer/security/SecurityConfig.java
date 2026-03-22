package com.musicplayer.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 关键修复 1：必须通知 Spring Security 启用 CORS 跨域支持！
                .cors(org.springframework.security.config.Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 关键修复 2：无条件放行所有跨域的 OPTIONS 预检请求！
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 登录注册放行
                        .requestMatchers("/api/auth/**").permitAll()

                        //  AI DJ 接口发“免死金牌”，允许所有人调用
                        .requestMatchers("/api/ai-dj/**").permitAll()
                        .requestMatchers("/error").permitAll()

                        //分享码
                        .requestMatchers("/api/playlists/share/preview/**").permitAll()

                        // 音频/封面/歌词 (建议用 ** 代替 *，防止 ID 里有特殊字符导致匹配失败)
                        .requestMatchers("/api/songs/*/audio").permitAll()
                        .requestMatchers("/api/songs/*/cover").permitAll()
                        .requestMatchers("/api/songs/*/lyrics").permitAll()
                        .requestMatchers("/api/playlists/*/cover").permitAll()
                        .requestMatchers("/api/user/avatar/**").permitAll()

                        // 歌曲列表需要登录
                        .requestMatchers(HttpMethod.GET, "/api/songs").authenticated()
                        // 上传/删除歌曲只有 ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/songs/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/songs/**").hasRole("ADMIN")

                        //修改密码
                        .requestMatchers(HttpMethod.PUT, "/api/songs/**").hasRole("ADMIN")

                        // 歌单相关需要登录
                        .requestMatchers("/api/playlists/**").authenticated()
                        // 用户自己的信息
                        .requestMatchers("/api/user/**").authenticated()

                        //放行 评论
                        .requestMatchers(HttpMethod.GET, "/api/comments/**").permitAll()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}