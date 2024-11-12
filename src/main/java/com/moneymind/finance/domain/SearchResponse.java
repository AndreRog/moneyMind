package com.moneymind.finance.domain;

import java.util.List;

public record SearchResponse<T>(List<T> list, int limit, String cursor) {
}
