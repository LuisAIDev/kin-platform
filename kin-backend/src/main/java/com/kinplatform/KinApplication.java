package com.kinplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching

@SpringBootApplication
public class KinApplication {

    public static void main(String[] args) {
        SpringApplication.run(KinApplication.class, args);
    }
}
