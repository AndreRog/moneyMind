package com.moneymind.finance.adapters.in.web.http;


import com.moneymind.finance.adapters.in.web.http.hateoas.Link;

import java.util.List;

public record Page<T> (Link next, Link previous, List<T> _embedded){}