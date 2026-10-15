package com.dpt.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Main application entry point.
 *
 * <p>Extends {@link SpringBootServletInitializer} so the WAR bootstraps when it
 * is deployed to an external Tomcat (the {@code dptweb-1.0.war} pipeline path);
 * {@link #main(String[])} keeps {@code java -jar} / embedded runs working too.
 *
 * <p>Spring Boot auto-configuration wires up the embedded Tomcat server, Spring MVC,
 * Spring Security, the JSP view resolver, the HikariCP connection pool and the
 * transaction manager. Session-cookie hardening (HttpOnly / Secure / SameSite) is
 * configured through {@code application.properties} and environment variables.
 */
@SpringBootApplication
public class MyWebAppApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(MyWebAppApplication.class, args);
    }
}
