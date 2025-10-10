package com.example.javadns;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class JavadnsApplication {

    public static void main(String[] args) {
        SpringApplication.run(JavadnsApplication.class, args);
    }

}

