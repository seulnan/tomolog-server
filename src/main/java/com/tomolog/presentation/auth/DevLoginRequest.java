package com.tomolog.presentation.auth;

/** Optional body for dev-login; if {@code oauthId} is omitted a default test user is used. */
public record DevLoginRequest(String oauthId) {}
