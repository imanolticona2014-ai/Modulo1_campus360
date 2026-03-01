package com.campus360.identidad.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuditoriaClient {

    // URL del módulo G9 - cuando esté disponible cambiar esta URL
    private static final String G9_URL = "http://localhost:8090/api/v1/auditoria/eventos";

    private final RestTemplate restTemplate;

    public AuditoriaClient() {
        this.restTemplate = new RestTemplate();
    }

    public void registrar(String tipoEvento, String usuario, String ip, String descripcion) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("tipoEvento", tipoEvento);
            payload.put("usuario", usuario);
            payload.put("ipAddress", ip);
            payload.put("descripcion", descripcion);
            payload.put("timestamp", LocalDateTime.now().toString());
            payload.put("modulo", "G1-IDENTIDAD-ACCESOS");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            restTemplate.postForEntity(G9_URL, request, String.class);

            System.out.println("✅ [AUDITORIA G9] Evento registrado: " + tipoEvento + " | Usuario: " + usuario);

        } catch (Exception e) {
            // G9 no está disponible - registrar localmente sin interrumpir el flujo
            System.out.println("⚠️ [AUDITORIA G9 - MOCK] " + tipoEvento +
                    " | Usuario: " + usuario +
                    " | IP: " + ip +
                    " | Desc: " + descripcion +
                    " | Timestamp: " + LocalDateTime.now());
        }
    }
}
