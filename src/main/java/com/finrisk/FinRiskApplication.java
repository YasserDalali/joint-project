package com.finrisk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/** Root Spring Boot application class that wires FinRisk together and starts the embedded web server. */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class FinRiskApplication {

    /** Starts the FinRisk Spring Boot application from the command line. */
    public static void main(String[] args) {
        SpringApplication.run(FinRiskApplication.class, args);
    }
}
