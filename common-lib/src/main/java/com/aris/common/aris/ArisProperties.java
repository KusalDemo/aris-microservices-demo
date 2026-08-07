package com.aris.common.aris;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ARIS policy client settings ({@code aris.*} / {@code ARIS_*} env vars).
 */
@ConfigurationProperties(prefix = "aris")
public class ArisProperties {

    /**
     * Base URL of the external ARIS policy service, e.g. {@code http://localhost:18080}.
     */
    private String baseUrl = "http://localhost:18080";

    private String decidePath = "/decide";

    private int connectTimeoutMs = 500;

    private int readTimeoutMs = 1000;

    private StaticDefaults staticDefaults = new StaticDefaults();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getDecidePath() {
        return decidePath;
    }

    public void setDecidePath(String decidePath) {
        this.decidePath = decidePath;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public StaticDefaults getStaticDefaults() {
        return staticDefaults;
    }

    public void setStaticDefaults(StaticDefaults staticDefaults) {
        this.staticDefaults = staticDefaults;
    }

    public String decideUrl() {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String path = decidePath.startsWith("/") ? decidePath : "/" + decidePath;
        return base + path;
    }

    public ArisDecideResponse toFailOpenResponse() {
        return new ArisDecideResponse(
                staticDefaults.getRetry(),
                staticDefaults.getBackoffMultiplier(),
                (double) staticDefaults.getTimeoutMs(),
                List.of("aris_fail_open_static_defaults"),
                false
        );
    }

    public ArisSuggestedAction toSuggestedAction() {
        return new ArisSuggestedAction(
                staticDefaults.getRetry(),
                staticDefaults.getBackoffMultiplier(),
                (double) staticDefaults.getTimeoutMs()
        );
    }

    public static class StaticDefaults {
        private int retry = 3;
        private double backoffMultiplier = 1.5;
        private long timeoutMs = 3000L;

        public int getRetry() {
            return retry;
        }

        public void setRetry(int retry) {
            this.retry = retry;
        }

        public double getBackoffMultiplier() {
            return backoffMultiplier;
        }

        public void setBackoffMultiplier(double backoffMultiplier) {
            this.backoffMultiplier = backoffMultiplier;
        }

        public long getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }
}
