package com.example.docsigning.config;

import java.time.Duration;

public class AppConfig {

    private static final String DEFAULT_DIGICERT_BASE_URL = "https://www.digicert.com/services/v2";
    private static final String PARAM_NAME = "VAULT_SIGNING__SERVICE_FR_ACCOUNTING_DIGICERT_API_KEY";
    private static final String REDIS_DEFAULT_ENDPOINT = "127.0.0.1";
    private static final String REDIS_KEY = "VAULT_SIGNING_SERVICE_FR_ACCOUNTING_CACHED_CERT";

    public String getDigicertBaseUrl() {
        return env("DIGICERT_API_BASE_URL", DEFAULT_DIGICERT_BASE_URL);
    }

    public String getApiKeySsmParamName() {
        return env("DIGICERT_API_KEY_SSM_PARAM", PARAM_NAME);
    }

    public String getRedisEndpoint() {
        return env("COMPLIANCE_SERVICE_REDIS_CACHE", REDIS_DEFAULT_ENDPOINT);
    }

    public int getRedisPort() {
        return Integer.parseInt(env("REDIS_PORT", "6379"));
    }

    public String getRedisCacheKey() {
        return env("REDIS_CERT_CACHE_KEY", REDIS_KEY);
    }

    public int getRedisTtlSeconds() {
        return Integer.parseInt(env("REDIS_CERT_CACHE_TTL_SECONDS", "3600"));
    }

    public int getHttpConnectTimeoutMillis() {
        return (int) Duration.ofSeconds(15).toMillis();
    }

    public int getHttpReadTimeoutMillis() {
        return (int) Duration.ofSeconds(30).toMillis();
    }

    private String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }
}
