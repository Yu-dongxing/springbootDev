/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/04/11
 */

package top.yuxs.springbootdev.core.config.db;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import top.yuxs.springbootdev.core.db.DatabaseInitService;

@Configuration
@ConditionalOnProperty(prefix = "db.init", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseInitConfig implements SmartInitializingSingleton {

    private final DatabaseInitService databaseInitService;

    public DatabaseInitConfig(DatabaseInitService databaseInitService) {
        this.databaseInitService = databaseInitService;
    }

    @Override
    public void afterSingletonsInstantiated() {
        databaseInitService.initDatabase();
    }
}