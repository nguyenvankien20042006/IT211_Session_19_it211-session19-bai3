package com.example.bai3.controller;

import com.example.bai3.model.LogoutRequest;
import com.example.bai3.model.RefreshToken;
import com.example.bai3.model.TokenRequest;
import com.example.bai3.service.RefreshTokenService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/refresh-token")
@AllArgsConstructor
public class RefreshTokenController {
    private final RefreshTokenService refreshTokenService;

    @PostMapping
    public ResponseEntity<RefreshToken> createRefreshToken(@RequestBody TokenRequest tokenRequest) {
        return new ResponseEntity<>(refreshTokenService.generateRefreshToken(tokenRequest), HttpStatus.CREATED);
    }

    @PostMapping("/logout")
    public ResponseEntity<RefreshToken> logout(@RequestBody LogoutRequest logoutRequest) {
        return new ResponseEntity<>(refreshTokenService.logout(logoutRequest), HttpStatus.OK);
    }

    @PostMapping("/logoutAllDevices")
    public ResponseEntity<?> logoutAll(@RequestBody TokenRequest tokenRequest) {
        refreshTokenService.logoutAll(tokenRequest);
        return new ResponseEntity<>("Logout all successfully", HttpStatus.OK);
    }
}
