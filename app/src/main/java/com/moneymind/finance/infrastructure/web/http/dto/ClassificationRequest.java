package com.moneymind.finance.infrastructure.web.http.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ClassificationRequest(String cursor) {
}
