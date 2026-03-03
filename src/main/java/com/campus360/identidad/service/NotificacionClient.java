package com.campus360.identidad.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Cliente REST para el Módulo de Notificaciones (G7).
 *
 * CORRECCIÓN (Smell 2): RestTemplate inyectado por constructor (mismo
 * razonamiento que AuditoriaClient). Al ser el mismo @Bean declarado en
 * AppConfig, ambos clientes comparten la misma configuración de timeouts.
 */
@Service
public class NotificacionClient {

    private static final String G7_URL = "http://localhost:8070/api/v1/events";

    private final RestTemplate restTemplate;

    public NotificacionClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void enviarBienvenida(String correo, String nombre, String passwordTemporal) {
        enviar("BIENVENIDA", correo, Map.of(
            "nombre", nombre,
            "passwordTemporal", passwordTemporal,
            "mensaje", "Bienvenido a Campus360. Tu cuenta ha sido creada."
        ));
    }

    public void enviarRecuperacionPassword(String correo, String nombre, String tokenRecuperacion) {
        enviar("RECUPERACION_PASSWORD", correo, Map.of(
            "nombre", nombre,
            "tokenRecuperacion", tokenRecuperacion,
            "enlace", "http://localhost:8080/recuperar?token=" + tokenRecuperacion,
            "mensaje", "Solicitud de recuperación de contraseña."
        ));
    }

    public void enviarAlertaBloqueo(String correo, String nombre, String ip) {
        enviar("CUENTA_BLOQUEADA", correo, Map.of(
            "nombre", nombre,
            "ip", ip,
            "mensaje", "Tu cuenta ha sido bloqueada temporalmente por intentos fallidos."
        ));
    }

    public void enviarCambioPassword(String correo, String nombre) {
        enviar("CAMBIO_PASSWORD", correo, Map.of(
            "nombre", nombre,
            "mensaje", "Tu contraseña ha sido actualizada exitosamente."
        ));
    }

    // Método base — punto único de envío al G7
    private void enviar(String eventType, String recipient, Map<String, String> data) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventType", eventType);
            payload.put("recipient", recipient);
            payload.put("data", data);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            restTemplate.postForEntity(G7_URL, request, String.class);

            System.out.println("[NOTIFICACION G7] Evento enviado: " + eventType + " a: " + recipient);

        } catch (Exception e) {
            // G7 no disponible — log local sin interrumpir el flujo principal
            System.out.println("[NOTIFICACION G7 - FALLBACK] Tipo: " + eventType
                    + " | Destinatario: " + recipient);
        }
    }
}
