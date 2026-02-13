package com.campus360.identidad;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class G1IdentidadApplication {

	public static void main(String[] args) {
        SpringApplication.run(G1IdentidadApplication.class, args);
        System.out.println("Módulo G1 - Identidad y Accesos iniciado en http://localhost:8080");
    }

}
