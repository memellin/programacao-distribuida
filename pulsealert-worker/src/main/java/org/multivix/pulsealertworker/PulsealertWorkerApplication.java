package org.multivix.pulsealertworker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PulsealertWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PulsealertWorkerApplication.class, args);
    }

}
