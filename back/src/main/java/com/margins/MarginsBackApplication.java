package com.margins;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class MarginsBackApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarginsBackApplication.class, args);
    }
}
