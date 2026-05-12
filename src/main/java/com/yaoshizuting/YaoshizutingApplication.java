package com.yaoshizuting;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@MapperScan("com.yaoshizuting.mapper")
@EnableTransactionManagement
@EnableAsync
@EnableScheduling
public class YaoshizutingApplication {
    public static void main(String[] args) {
        SpringApplication.run(YaoshizutingApplication.class, args);
    }
}
