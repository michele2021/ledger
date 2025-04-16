package com.accounting.core.swagger;

import java.util.Set;

public class ApiDocConstants {

    public static final String API_KEY = "API_KEY";
    public static final String SWAGGER_ENDPOINT = "/swagger";
    public static final String JWT_TOKEN = "JWT_TOKEN";
    public static final String SWAGGER_URI_PREFIX = "/accounting-docs";
    public static final Set<String> RESOURCE_PACKAGES = Set.of("com.accounting.core.resources", "com.accounting.core.admin");

    public static final String HTTP_STATUS_OK = "200";
    public static final String OK_STATUS_DESCRIPTION = "OK - successful response";
    public static final String HTTP_STATUS_ACCEPTED = "202";
    public static final String ACCEPTED_STATUS_DESCRIPTION = "Accepted - successful response";
}
