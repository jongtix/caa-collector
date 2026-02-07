package com.custom.trader;

import com.custom.trader.testcontainers.MySQLTestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(MySQLTestcontainersConfig.class)
class AlgorithmicTradingWithClaudeCodeApplicationTests {

    @Test
    void contextLoads() {
    }

}
