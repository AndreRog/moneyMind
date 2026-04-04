package com.moneymind.classifier.domain;

public record ClassificationResult(String predictedCategory, double confidence, boolean highConfidence) {
}
