package com.musicplayer.controller;

import com.musicplayer.model.User;
import com.musicplayer.security.JwtUtil;
import com.musicplayer.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserService userService, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        try {
            String username = body.get("username");
            String password = body.get("password");
            String nickname = body.get("nickname");

            if (username == null || username.isBlank() || password == null || password.length() < 6) {
                return ResponseEntity.badRequest().body(Map.of("message", "用户名不能为空，密码至少6位"));
            }

            User user = userService.register(username, password, nickname);
            String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole().name());

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "user", toDto(user)
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            String username = body.get("username");
            String password = body.get("password");

            User user = userService.findByUsername(username);
            if (!passwordEncoder.matches(password, user.getPassword())) {
                return ResponseEntity.badRequest().body(Map.of("message", "密码错误"));
            }

            String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole().name());
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "user", toDto(user)
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户名不存在"));
        }
    }

    private Map<String, Object> toDto(User user) {
        return Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "nickname", user.getNickname() != null ? user.getNickname() : "",
                "role", user.getRole().name(),
                "avatarFile", user.getAvatarFile() != null ? user.getAvatarFile() : ""
        );
    }
}