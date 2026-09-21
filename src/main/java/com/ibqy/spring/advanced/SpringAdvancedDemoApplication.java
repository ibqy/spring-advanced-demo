package com.ibqy.spring.advanced;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Spring Boot 4.1 + Spring Framework 7.0 高级特性教学项目
 *
 * @author ibqy
 * @since 2026-09-21
 */
@SpringBootApplication
@EnableAsync
public class SpringAdvancedDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAdvancedDemoApplication.class, args);
        System.out.println("""
                
                ╔══════════════════════════════════════════════════════════╗
                ║     Spring Advanced Demo — 高级特性教学项目              ║
                ║     Spring Boot 4.1.1 + Spring Framework 7.0.8         ║
                ║     Java 21 · 10 个高阶 Demo                           ║
                ╚══════════════════════════════════════════════════════════╝
                
                Demo 列表:
                  01. gRPC 服务端 (Demo01)
                  02. HTTP Interface Client (Demo02)
                  03. Virtual Threads 并发 (Demo03)
                  04. AOT & Native Image (Demo04)
                  05. 可观测性 Micrometer + Actuator (Demo05)
                  06. Spring MVC 进阶 (Demo06)
                  07. Spring Data JPA 进阶 (Demo07)
                  08. 事件驱动机制 (Demo08)
                  09. 自定义 Actuator 端点 (Demo09)
                  10. Spring Security 6 进阶 (Demo10)
                
                """);
    }
}
