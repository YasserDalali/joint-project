package com.finrisk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class FinRiskApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinRiskApplication.class, args);
    }
}
