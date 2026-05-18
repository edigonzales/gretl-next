package ch.so.agi.gretl.control.server;

import ch.so.agi.gretl.control.server.config.GretlControlProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(GretlControlProperties.class)
public class GretlControlServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(GretlControlServerApplication.class, args);
    }
}
