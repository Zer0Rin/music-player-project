package com.musicplayer.service;

import com.musicplayer.model.Playlist;
import com.musicplayer.model.User;
import com.musicplayer.repository.PlaylistRepository;
import com.musicplayer.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PlaylistRepository playlistRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    private static final String AVATAR_DIR = "music-data/avatars/";

    public UserService(UserRepository userRepository,
                       PlaylistRepository playlistRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.playlistRepository = playlistRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 启动时初始化 admin
    @Bean
    public ApplicationRunner initAdmin() {
        return args -> {
            if (!userRepository.existsByUsername(adminUsername)) {
                User admin = new User();
                admin.setId(UUID.randomUUID().toString());
                admin.setUsername(adminUsername);
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setNickname("管理员");
                admin.setRole(User.Role.ADMIN);
                userRepository.save(admin);

                // 给 admin 创建"我喜欢"系统歌单
                createFavoritesPlaylist(admin.getId());
                System.out.println("✅ Admin account initialized: " + adminUsername);
            }
        };
    }

    // 注册普通用户
    public User register(String username, String password, String nickname) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname != null ? nickname : username);
        user.setRole(User.Role.USER);
        userRepository.save(user);

        // 新用户自动创建"我喜欢"歌单
        createFavoritesPlaylist(user.getId());
        return user;
    }

    // 创建系统"我喜欢"歌单
    private void createFavoritesPlaylist(String userId) {
        Playlist favorites = new Playlist();
        favorites.setId("favorites-" + userId);
        favorites.setName("我喜欢");
        favorites.setSystem(true);
        favorites.setUserId(userId);
        playlistRepository.save(favorites);
    }

    // 上传头像
    public String uploadAvatar(String userId, MultipartFile file) throws IOException {
        Files.createDirectories(Paths.get(AVATAR_DIR));
        String ext = getExtension(file.getOriginalFilename());
        String filename = userId + "." + ext;
        Path dest = Paths.get(AVATAR_DIR + filename);
        file.transferTo(dest);

        User user = userRepository.findById(userId).orElseThrow();
        user.setAvatarFile(filename);
        userRepository.save(user);
        return filename;
    }

    // 更新昵称
    public User updateProfile(String userId, String nickname) {
        User user = userRepository.findById(userId).orElseThrow();
        if (nickname != null && !nickname.isBlank()) {
            user.setNickname(nickname);
        }
        return userRepository.save(user);
    }

    public User findById(String id) {
        return userRepository.findById(id).orElseThrow();
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow();
    }

    private String getExtension(String filename) {
        if (filename == null) return "jpg";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1) : "jpg";
    }
}