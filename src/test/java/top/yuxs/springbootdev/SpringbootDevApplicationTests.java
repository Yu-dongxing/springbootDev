/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/04/11
 */

package top.yuxs.springbootdev;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import top.yuxs.springbootdev.modules.system.entity.SysLog;
import top.yuxs.springbootdev.modules.system.service.SysLogService;
import java.time.LocalDateTime;

@SpringBootTest
class SpringbootDevApplicationTests {

    @Autowired
    private SysLogService sysLogService;

    @Test
    void contextLoads() {
        System.out.println("======= 开始测试数据库保存 =======");
        try {
            SysLog sysLog = new SysLog();
            sysLog.setUsername("user (尝试/访客)");
            sysLog.setIp("127.0.0.1");
            sysLog.setUrl("/api/common/auth/admin/login");
            sysLog.setMethod("POST");
            sysLog.setClassName("top.yuxs.springbootdev.modules.system.controller.AuthController");
            sysLog.setMethodName("loginAdmin");
            sysLog.setTitle("B端管理端 物理隔离安全登录");
            sysLog.setBusinessType("INSERT");
            sysLog.setParam("{\"param\":{\"username\":\"user\",\"password\":\"123456\"}}");
            sysLog.setStatus(0);
            sysLog.setResult(">>>>>> [Aegis Auth Guard] 拦截到跨端登录尝试！检测到该账户存在，但物理隔离拦截激活。账户名: [user], 当前库中实际类型: [USER], 请求的端点通道: [B 端管理后台 (ADMIN)]");
            sysLog.setErrorMsg("[神盾安全防护中心拦截警报]\n>>>>>> [Aegis Auth Guard] 拦截到跨端登录尝试！检测到该账户存在，但物理隔离拦截激活。账户名: [user], 当前库中实际类型: [USER], 请求的端点通道: [B 端管理后台 (ADMIN)]\n\n[异常详细堆栈信息]\ntop.yuxs.springbootdev.core.exception.BusinessException: 用户名或密码错误\n\tat top.yuxs.springbootdev.modules.system.service.impl.SysUserServiceImpl.loginAdmin(SysUserServiceImpl.java:165)");
            sysLog.setTakeTime(50L);
            sysLog.setRequestTime(LocalDateTime.now());
            
            boolean saved = sysLogService.save(sysLog);
            System.out.println("======= 保存结果：" + saved + "，ID：" + sysLog.getId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
