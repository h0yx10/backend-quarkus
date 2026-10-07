package com.events.infrastructure.adapter.in.rest.dto;

public record ProgressResponse(long done, long total, double percentage) {
}
