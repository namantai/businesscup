package com.aicompass.compass.shared.web;

/** Public Problem Details shape, including the request correlation id. */
public record ApiProblem(String type, String title, int status, String detail, String instance, String requestId) {}
