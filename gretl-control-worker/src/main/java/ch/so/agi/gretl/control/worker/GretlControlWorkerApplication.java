package ch.so.agi.gretl.control.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GretlControlWorkerApplication {
    public static void main(String[] args) {
        SpringApplication.run(GretlControlWorkerApplication.class, args);
    }
}
