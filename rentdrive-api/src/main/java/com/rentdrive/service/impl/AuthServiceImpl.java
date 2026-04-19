package com.rentdrive.service.impl;

import com.rentdrive.config.JwtConfig;
import com.rentdrive.dto.request.LoginRequest;
import com.rentdrive.dto.response.AuthResponse;
import com.rentdrive.entity.User;
import com.rentdrive.enums.UserStatus;
import com.rentdrive.exception.ForbiddenException;
import com.rentdrive.exception.ValidationException;
import com.rentdrive.mapper.UserMapper;
import com.rentdrive.repository.UserRepository;
import com.rentdrive.security.JwtProvider;
import com.rentdrive.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository      userRepository;
    private final PasswordEncoder     passwordEncoder;
    private final JwtProvider         jwtProvider;
    private final JwtConfig           jwtConfig;
    private final StringRedisTemplate redisTemplate;
    private final UserMapper          userMapper;

    private static final String BLACKLIST_PREFIX = "blacklist:";

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailWithProfileAndRoles(request.email().toLowerCase().trim())
                .orElseThrow(() -> new ValidationException(
                        "Email ou mot de passe incorrect.",
                        Map.of("credentials", "Email ou mot de passe invalide")));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ValidationException(
                    "Email ou mot de passe incorrect.",
                    Map.of("credentials", "Email ou mot de passe invalide"));
        }

        if (user.getStatus() == UserStatus.BANNED) {
            throw new ForbiddenException("Ce compte a été banni.");
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new ForbiddenException("Ce compte est temporairement suspendu.");
        }

        List<String> roles = user.getRoles().stream()
                .map(r -> r.getName().name())
                .toList();

        String accessToken  = jwtProvider.generateAccessToken(user.getId(), roles);
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());

        log.info("Login : {} ({})", user.getEmail(), user.getId());
        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtConfig.getAccessExpirationMs(),
                userMapper.toResponse(user));
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken) || !jwtProvider.isRefreshToken(refreshToken)) {
            throw new ValidationException(
                    "Refresh token invalide ou expiré.",
                    Map.of("refreshToken", "Token invalide"));
        }

        var userId = jwtProvider.extractUserId(refreshToken);

        User user = userRepository.findByIdWithAllRelations(userId)
                .orElseThrow(() -> new ValidationException(
                        "Utilisateur introuvable.",
                        Map.of("refreshToken", "Utilisateur associé introuvable")));

        if (user.getStatus() == UserStatus.BANNED || user.getStatus() == UserStatus.SUSPENDED) {
            throw new ForbiddenException("Accès refusé.");
        }

        List<String> roles = user.getRoles().stream()
                .map(r -> r.getName().name())
                .toList();

        String newAccessToken  = jwtProvider.generateAccessToken(user.getId(), roles);
        String newRefreshToken = jwtProvider.generateRefreshToken(user.getId());

        return new AuthResponse(
                newAccessToken,
                newRefreshToken,
                jwtConfig.getAccessExpirationMs(),
                userMapper.toResponse(user));
    }

    @Override
    public void logout(String accessToken) {
        if (!jwtProvider.validateToken(accessToken)) {
            return;
        }
        String jti       = jwtProvider.extractJti(accessToken);
        long   remaining = jwtProvider.extractRemainingMs(accessToken);

        // Blacklist dans Redis jusqu'à expiration naturelle du token
        redisTemplate.opsForValue().set(
                BLACKLIST_PREFIX + jti,
                "1",
                remaining,
                TimeUnit.MILLISECONDS);

        log.info("Token blacklisté : jti={}", jti);
    }
}
