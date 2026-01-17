package com.custom.trader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AlgorithmicTradingWithClaudeCodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlgorithmicTradingWithClaudeCodeApplication.class, args);
    }

}
