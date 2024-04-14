package com.ezone.ezproject.configuration;

import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.boot.autoconfigure.elasticsearch.RestClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EsRestClientCustomConfiguration {
    @Bean
    public RestClientBuilderCustomizer restClientBuilderCustomizer() {
        return new RestClientBuilderCustomizer() {
            @Override
            public void customize(RestClientBuilder builder) { }

            @Override
            public void customize(HttpAsyncClientBuilder builder) {
                builder.setDefaultIOReactorConfig(IOReactorConfig.custom().setSoKeepAlive(true).build());
            }
        };
    }

}
