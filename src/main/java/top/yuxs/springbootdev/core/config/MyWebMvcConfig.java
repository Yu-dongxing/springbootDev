/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/04/11
 */

package top.yuxs.springbootdev.core.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import top.yuxs.springbootdev.core.config.file.FileProperties;

import java.io.File;

/**
 * 实现 WebMvcConfigurer 接口，将请求路径（URL）映射到物理磁盘路径
 */
@Configuration
public class MyWebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private FileProperties fileProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        /*
          addResourceHandler: 浏览器访问的虚拟路径
          addResourceLocations: 磁盘实际的物理路径
          注意：物理路径必须以 "file:" 开头，且建议为绝对路径
         */
        String uploadPath = fileProperties.getLocal().getUploadPath();
        String accessPath = fileProperties.getLocal().getAccessPath();
        
        // 确保路径以 / 结尾
        if (!uploadPath.endsWith(File.separator) && !uploadPath.endsWith("/")) {
            uploadPath += File.separator;
        }

        // 获取绝对路径并转换为标准的 file 协议格式
        File file = new File(uploadPath);
        String absolutePath = file.getAbsolutePath();
        
        // 统一处理 Windows 和 Linux 路径格式，确保以 file:/// 开头
        String protocolPrefix = "file:";
        if (!absolutePath.startsWith("/")) {
            protocolPrefix = "file:/";
        }
        
        String resourceLocation = protocolPrefix + absolutePath.replace("\\", "/");
        if (!resourceLocation.endsWith("/")) {
            resourceLocation += "/";
        }

        registry.addResourceHandler(accessPath)
                .addResourceLocations(resourceLocation);
    }

    /**
     * 在全局 HttpMessageConverter 最底层强制装配自定义 SmartLongSerializer 策略
     * 解决 Spring Boot 自动装配可能在特定环境（如第三方库覆盖 ObjectMapper）下失效的顽疾，
     * 彻底、无死角地保证所有 Spring MVC 返回大数时都被智能序列化为 String 类型。
     */
    @Override
    public void extendMessageConverters(java.util.List<org.springframework.http.converter.HttpMessageConverter<?>> converters) {
        for (org.springframework.http.converter.HttpMessageConverter<?> converter : converters) {
            if (converter instanceof org.springframework.http.converter.json.MappingJackson2HttpMessageConverter) {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = 
                        ((org.springframework.http.converter.json.MappingJackson2HttpMessageConverter) converter).getObjectMapper();
                
                com.fasterxml.jackson.databind.module.SimpleModule module = new com.fasterxml.jackson.databind.module.SimpleModule();
                // 1. 注入智能 Long 序列化器
                module.addSerializer(Long.class, SmartLongSerializer.instance);
                module.addSerializer(Long.TYPE, SmartLongSerializer.instance);
                // 2. 注入 BigInteger 序列化器
                module.addSerializer(java.math.BigInteger.class, com.fasterxml.jackson.databind.ser.std.ToStringSerializer.instance);
                
                objectMapper.registerModule(module);
            }
        }
    }
}