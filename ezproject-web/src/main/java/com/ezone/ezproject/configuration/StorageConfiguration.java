package com.ezone.ezproject.configuration;

import com.ezone.ezproject.Application;
import com.ezone.ezproject.common.serialize.SelectorObjectMapperBuilder;
import com.ezone.ezproject.common.storage.CacheableStorage;
import com.ezone.ezproject.common.storage.FileStorage;
import com.ezone.ezproject.common.storage.IStorage;
import com.ezone.ezproject.common.storage.Ks3Storage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksyun.ks3.http.HttpClientConfig;
import com.ksyun.ks3.service.Ks3Client;
import com.ksyun.ks3.service.Ks3ClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfiguration {
    public static final ObjectMapper OBJECT_MAPPER = SelectorObjectMapperBuilder.builder()
            .clazzImpls(IStorage.class, Ks3Storage.class, FileStorage.class, CacheableStorage.class)
            .clazzImpls(Ks3Client.class)
            .context(Application.context())
            .build();

    public static final String STORAGE_RESOURCE = "/attachment-storage.yaml";

    @Bean("attachmentStorage")
    public IStorage attachmentStorage() throws Exception {
        IStorage storage = OBJECT_MAPPER.readValue(getClass().getResource(STORAGE_RESOURCE), IStorage.class);
        return storage;
    }

    @Configuration
    @ConditionalOnProperty(prefix = "ksyun.ks3", name = "endpoint")
    public class Ks3ClientConfiguration {
        @Value("${ksyun.ks3.endpoint}")
        private String endpoint;
        @Value("${ksyun.ks3.ak}")
        private String ak;
        @Value("${ksyun.ks3.sk}")
        private String sk;
        @Value("${ksyun.ks3.bucket.name}")
        private String bucketName;
        @Value("${ksyun.ks.product.name}")
        private String productName;

        private static final int MAX_CONNECTIONS = 500;
        private static final int CONNECTION_TIME_OUT = 25000;
        private static final int SOCKET_TIME_OUT = 25000;
        private static final int MAX_RETRY = 1;

        @Bean
        public Ks3Client ks3Client() {
            Ks3ClientConfig config = new Ks3ClientConfig();
            config.setEndpoint(endpoint);
            config.setDomainMode(true);
            config.setProtocol(Ks3ClientConfig.PROTOCOL.http);
            HttpClientConfig httpClientConfig = new HttpClientConfig();
            httpClientConfig.setMaxRetry(MAX_RETRY);
            httpClientConfig.setMaxConnections(MAX_CONNECTIONS);
            httpClientConfig.setConnectionTimeOut(CONNECTION_TIME_OUT);
            httpClientConfig.setSocketTimeOut(SOCKET_TIME_OUT);
            config.setHttpClientConfig(httpClientConfig);
            return new Ks3Client(ak, sk, config);
        }
    }

}
