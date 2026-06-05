package com.example.bai3.service;

import com.example.bai3.model.LogoutRequest;
import com.example.bai3.model.TokenRequest;
import com.example.bai3.model.RefreshToken;
import com.example.bai3.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public RefreshToken generateRefreshToken(TokenRequest request) {
        RefreshToken refreshToken = RefreshToken.builder()
                .username(request.getUsername())
                .token(UUID.randomUUID().toString())
                .revoked(false)
                .expired(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24))
                .deviceId(UUID.randomUUID().toString())
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken logout(LogoutRequest logoutRequest) {
        RefreshToken refreshToken = refreshTokenRepository.findByDeviceId(logoutRequest.getDeviceId()).orElseThrow(() -> new RuntimeException("token not found"));
        refreshToken.setRevoked(true);
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public void logoutAll(TokenRequest tokenRequest) {
        refreshTokenRepository.logoutAll(tokenRequest.getUsername());
    }
}
