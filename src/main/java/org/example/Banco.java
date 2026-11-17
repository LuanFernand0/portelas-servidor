package org.example;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Conexão com o MySQL num lugar só.
 * Os dados vêm do application.properties (spring.datasource.*).
 * No servidor de produção, dá para trocar sem mexer no código com variáveis de ambiente:
 * SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME e SPRING_DATASOURCE_PASSWORD.
 */
@Component
public class Banco {

    @Value("${spring.datasource.url}")
    private String urlConfig;

    @Value("${spring.datasource.username}")
    private String usuarioConfig;

    @Value("${spring.datasource.password}")
    private String senhaConfig;

    private static String url;
    private static String usuario;
    private static String senha;

    @PostConstruct
    void carregar() {
        url = urlConfig;
        usuario = usuarioConfig;
        senha = senhaConfig;
    }

    public static Connection conectar() throws SQLException {
        return DriverManager.getConnection(url, usuario, senha);
    }
}
