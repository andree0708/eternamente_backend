package com.eternamente.common.api;

public record ApiError(
    String code,
    String message
) {
}
