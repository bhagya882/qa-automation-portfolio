package com.qaportfolio.api.model;

public record LoginRequest(String username, String password, Integer expiresInMins) {
}
