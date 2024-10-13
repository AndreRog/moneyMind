package com.adapters.in.web.http;

import jakarta.ws.rs.core.Link;

import java.util.List;

public record Page<T> (Link next, Link previous, List<T> items){}