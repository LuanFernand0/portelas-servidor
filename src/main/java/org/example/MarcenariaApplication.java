package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MarcenariaApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarcenariaApplication.class, args);
        System.out.println("🚀 SERVIDOR DA MARCENARIA RODANDO NA PORTA 8080!");
    }
    // CORS, pasta de fotos e proteção do painel ficam no WebConfig.
}
