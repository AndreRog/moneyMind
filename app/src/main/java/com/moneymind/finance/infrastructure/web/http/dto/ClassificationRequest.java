package com.moneymind.finance.infrastructure.web.http.dto;

public record ClassificationRequest(boolean highConfidence, String cursor) {
}
