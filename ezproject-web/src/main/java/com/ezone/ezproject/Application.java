package com.ezone.ezproject;

import lombok.NoArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import tk.mybatis.spring.annotation.MapperScan;

@MapperScan(basePackages = {
        "com.ezone.ezproject.dal.extmapper", "com.ezone.ezproject.dal.mapper",
        "org.springframework.boot.autoconfigure.klock.KlockAutoConfiguration",
        "org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration"})
@ComponentScan(basePackages = {"com.ezone"})
@SpringBootApplication
@NoArgsConstructor
public class Application implements SpringApplicationRunListener {
    private static ConfigurableApplicationContext CONTEXT = null;

    /**
     * spi 扩展SpringApplicationRunListener所必需constructor
     */
    public Application(SpringApplication sa, String[] args) { }

    @Override
    public void contextPrepared(ConfigurableApplicationContext context) {
        CONTEXT = context;
    }

    public static ConfigurableApplicationContext context() {
        return CONTEXT;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}