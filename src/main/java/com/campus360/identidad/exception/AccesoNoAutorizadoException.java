package com.campus360.identidad.exception;

public class AccesoNoAutorizadoException extends RuntimeException {
    public AccesoNoAutorizadoException(String mensaje) {
        super(mensaje);
    }
}
