package com.zootopia;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.zootopia.mapper")
public class ZootopiaApplication {
    public static void main(String[] args) {
        SpringApplication.run(ZootopiaApplication.class, args);
    }
}

