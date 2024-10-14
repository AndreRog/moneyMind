package com.app.domain;

import java.util.List;

public record SearchResponse<T>(List<T> list, int limit, String cursor) {
}
