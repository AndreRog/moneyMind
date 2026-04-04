package com.moneymind.finance.infrastructure.web.http;

import com.moneymind.finance.infrastructure.web.http.hateoas.Link;

import java.util.List;

public record Page<T> (Link next, List<T> _embedded){}
