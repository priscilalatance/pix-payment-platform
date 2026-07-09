package com.pagamento;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MsPagamentoApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsPagamentoApplication.class, args);
    }
}
