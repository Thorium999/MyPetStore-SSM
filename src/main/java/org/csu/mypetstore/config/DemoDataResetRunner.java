package org.csu.mypetstore.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;

/**
 * 演示数据重置。只有显式加 --app.reset-db=true 才执行（见 pom 的 reset-db profile）：
 *   1. 00-reset-demo.sql 删掉七张表
 *   2. 02-schema-with-demo.sql 重建并灌种子数据
 * 平时启动完全不受影响。
 */
@Component
@ConditionalOnProperty(name = "app.reset-db", havingValue = "true")
public class DemoDataResetRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataResetRunner.class);

    private static final String RESET_SCRIPT = "database/00-reset-demo.sql";
    private static final String DEMO_SCRIPT = "database/02-schema-with-demo.sql";

    private final DataSource dataSource;

    public DemoDataResetRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            runScript(connection, RESET_SCRIPT);
            runScript(connection, DEMO_SCRIPT);
        }
        log.warn("演示数据已重置：七张表删除后按 {} 重建完成", DEMO_SCRIPT);
    }

    /** 脚本里有中文商品名，必须显式按 UTF-8 读取，否则 Windows 默认编码会读成乱码。 */
    private void runScript(Connection connection, String path) {
        ScriptUtils.executeSqlScript(connection,
                new EncodedResource(new ClassPathResource(path), StandardCharsets.UTF_8));
    }
}
