package com.ruoyi.framework.config;

import net.cz88.czdb.DbSearcher;
import net.cz88.czdb.QueryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * 纯真IP数据库配置
 * 
 * @author ruoyi
 */
@Configuration
public class CzdbConfig
{
    private static final Logger log = LoggerFactory.getLogger(CzdbConfig.class);

    private static final String KEY = "vcc/OUC9tX6CFEXmftVxZg==";
    private static final String IPV4_DB_PATH = "czdb/cz88_public_v4.czdb";
    private static final String IPV6_DB_PATH = "czdb/cz88_public_v6.czdb";

    /**
     * 从classpath复制文件到临时目录
     */
    private String copyResourceToTempFile(String resourcePath) throws Exception {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        
        // 尝试直接获取文件（开发环境）
        try {
            if (resource.exists() && resource.isFile()) {
                return resource.getFile().getAbsolutePath();
            }
        } catch (Exception e) {
            // 忽略，继续使用InputStream方式
        }
        
        // 从JAR包中复制到临时文件（生产环境）
        File tempFile = Files.createTempFile("czdb_", ".czdb").toFile();
        tempFile.deleteOnExit();
        
        try (InputStream inputStream = resource.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        
        return tempFile.getAbsolutePath();
    }

    /**
     * IPv4 数据库搜索器
     */
    @Bean(name = "ipv4DbSearcher")
    public DbSearcher ipv4DbSearcher()
    {
        try {
            String dbPath = copyResourceToTempFile(IPV4_DB_PATH);
            log.info("初始化IPv4数据库: {}", dbPath);
            return new DbSearcher(dbPath, QueryType.MEMORY, KEY);
        } catch (Exception e) {
            log.error("初始化IPv4数据库失败: {}", e.getMessage(), e);
            throw new RuntimeException("初始化IPv4数据库失败", e);
        }
    }

    /**
     * IPv6 数据库搜索器
     */
    @Bean(name = "ipv6DbSearcher")
    public DbSearcher ipv6DbSearcher()
    {
        try {
            String dbPath = copyResourceToTempFile(IPV6_DB_PATH);
            log.info("初始化IPv6数据库: {}", dbPath);
            return new DbSearcher(dbPath, QueryType.MEMORY, KEY);
        } catch (Exception e) {
            log.error("初始化IPv6数据库失败: {}", e.getMessage(), e);
            throw new RuntimeException("初始化IPv6数据库失败", e);
        }
    }
}

