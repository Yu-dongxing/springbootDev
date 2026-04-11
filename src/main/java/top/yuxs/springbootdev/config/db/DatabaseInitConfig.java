/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/04/11
 */

package top.yuxs.springbootdev.config.db;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.yuxs.springbootdev.db.DatabaseInitService;

import java.sql.SQLException;

@Configuration
@ConditionalOnProperty(prefix = "db.init", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseInitConfig {

    private final DatabaseInitService databaseInitService;
    public DatabaseInitConfig(DatabaseInitService databaseInitService) {
        this.databaseInitService = databaseInitService;
    }
    @Bean
    public DatabaseInitService initDatabase() throws SQLException, InterruptedException {
        databaseInitService.initDatabase();
        return databaseInitService; // 返回数据库初始化服务的实例
    }
}