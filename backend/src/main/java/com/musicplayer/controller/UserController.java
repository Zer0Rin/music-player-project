package com.musicplayer.controller;

import com.musicplayer.model.User;
import com.musicplayer.repository.UserRepository;
import com.musicplayer.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserService userService,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 获取当前用户信息
    @GetMapping("/me")
    public ResponseEntity<?> getMe(Authentication auth) {
        User user = userService.findById(auth.getName());
        return ResponseEntity.ok(toDto(user));
    }

    // 更新昵称
    @PutMapping("/me")
    public ResponseEntity<?> updateMe(Authentication auth,
                                      @RequestBody Map<String, String> body) {
        User user = userService.updateProfile(auth.getName(), body.get("nickname"));
        return ResponseEntity.ok(toDto(user));
    }

    // 上传头像
    @PostMapping("/me/avatar")
    public ResponseEntity<?> uploadAvatar(Authentication auth,
                                          @RequestParam("file") MultipartFile file) {
        try {
            String filename = userService.uploadAvatar(auth.getName(), file);
            return ResponseEntity.ok(Map.of("avatarFile", filename));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "上传失败"));
        }
    }

    // 获取头像文件
    @GetMapping("/avatar/{filename}")
    public ResponseEntity<org.springframework.core.io.Resource> getAvatar(
            @PathVariable String filename) throws IOException {
        java.nio.file.Path path = java.nio.file.Paths.get("music-data/avatars/" + filename);
        org.springframework.core.io.Resource resource =
                new org.springframework.core.io.FileSystemResource(path);
        if (!resource.exists()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().header("Content-Type", "image/jpeg").body(resource);
    }

    @PutMapping("/me/password")
    public ResponseEntity<?> changePassword(Authentication auth,
                                            @RequestBody Map<String, String> body) {
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");

        if (newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("message", "新密码至少6位"));
        }
        try {
            User user = userService.findById(auth.getName());
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                return ResponseEntity.badRequest().body(Map.of("message", "原密码错误"));
            }
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "密码修改成功"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "修改失败"));
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