package com.rlnkoo.notificationservice.api.dto;

public record PasswordResetConfirmRequest(String token, String newPassword) {}