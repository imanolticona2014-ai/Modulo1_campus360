package com.campus360.identidad.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Beans de aplicación de uso general.
 *
 * CORRECCIÓN (Smell 2): RestTemplate se declara como @Bean con timeout
 * configurado para que pueda ser inyectado en AuditoriaClient y
 * NotificacionClient, en lugar de ser creado con "new RestTemplate()"
 * dentro de cada servicio.
 *
 * Beneficios:
 *   - Permite configurar timeouts de conexión y lectura en un solo lugar.
 *   - Facilita el reemplazo por un mock en pruebas unitarias.
 *   - Sigue el principio DIP: las clases dependen de la abstracción
 *     inyectada, no de la creación concreta.
 */
@Configuration
public class AppConfig {

    /**
     * RestTemplate con timeouts razonables para integraciones con G7 y G9.
     * Si un módulo externo no responde en 3 segundos de conexión o 5 de
     * lectura, la excepción es capturada en el catch del cliente
     * correspondiente sin interrumpir el flujo principal.
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);  // 3 segundos para establecer conexión
        factory.setReadTimeout(5_000);     // 5 segundos para leer la respuesta
        return new RestTemplate(factory);
    }
}
