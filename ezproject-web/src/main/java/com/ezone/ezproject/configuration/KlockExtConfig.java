package com.ezone.ezproject.configuration;

import org.redisson.config.SentinelServersConfig;
import org.springframework.boot.autoconfigure.klock.config.KlockConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = KlockConfig.PREFIX)
public class KlockExtConfig {
    private SentinelServersConfig  sentinelServer;

    public SentinelServersConfig getSentinelServer() {
        return sentinelServer;
    }

    public void setSentinelServer(SentinelServersConfig sentinelServer) {
        this.sentinelServer = sentinelServer;
    }
}
