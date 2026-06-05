package com.smartoa;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.smartoa.mapper")
@EnableScheduling
public class SmartoaApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartoaApplication.class, args);
    }

}
