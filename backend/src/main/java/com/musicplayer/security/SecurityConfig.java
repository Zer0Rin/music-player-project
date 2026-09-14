package com.musicplayer.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
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
                // 未认证时返回 401 而不是默认的 403：
                // 前端 plugins/auth.js 只在 401 时清理 token 并跳转登录，403 只记录日志。
                // 若这里返回 403，token 过期后用户会停在页面上反复失败而不会被引导重新登录。
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        // 关键修复 2：无条件放行所有跨域的 OPTIONS 预检请求！
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 登录注册放行
                        .requestMatchers("/api/auth/**").permitAll()

                        // AI DJ 会调用付费模型：必须登录，并由 AiRateLimitInterceptor 按用户限流
                        .requestMatchers("/api/ai-dj/**").authenticated()
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
                        // AI 评论检索
                        .requestMatchers("/api/admin/comments/**").hasRole("ADMIN")
                        //热门 歌单/曲推荐
                        .requestMatchers(HttpMethod.GET, "/api/songs/hot").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/hot").authenticated()

                        // 最近 歌曲
                        .requestMatchers("/api/recent/**").authenticated()

                        //AI 歌词分析（SSE，前端通过 ?token= 传 JWT）：同样会调用付费模型，必须登录
                        .requestMatchers("/api/ai/analysis/**").authenticated()

                        //AI乐评人
                        .requestMatchers("/api/ai-comment/generate/**").hasRole("ADMIN")

                        // 热度刷新
                        .requestMatchers("/api/hot/refresh").hasRole("ADMIN")

                        //日推歌单
                        .requestMatchers("/api/daily/recommend").authenticated()
                        //日推刷新
                        .requestMatchers("/api/daily/recommend").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/daily/recommend/refresh").authenticated() // 新增

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}