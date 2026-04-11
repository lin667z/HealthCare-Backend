package com.nlpai4h.healthydemobacked;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@MapperScan("com.nlpai4h.healthydemobacked.mapper")
public class HealthyDemoBackedApplication {

    public static void main(String[] args) {
        SpringApplication.run(HealthyDemoBackedApplication.class, args);
    }

}
