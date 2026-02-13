package com.campus360.identidad.repository;
import com.campus360.identidad.domain.Usuario;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class UsuarioRepository {
    
    private final List<Usuario> usuarios = new ArrayList<>();
    
    public UsuarioRepository() {
        // Datos dummy para pruebas
        usuarios.add(new Usuario("1", "estudiante@campus360.com", "Juan Perez", "ESTUDIANTE"));
        usuarios.add(new Usuario("2", "admin@campus360.com", "Ana Lopez", "ADMIN"));
    }
    
    public Optional<Usuario> findByCorreo(String correo) {
        return usuarios.stream()
            .filter(u -> u.getCorreo().equals(correo))
            .findFirst();
    }
    
    public Usuario save(Usuario usuario) {
        usuarios.add(usuario);
        return usuario;
    }
    
    public List<Usuario> findAll() {
        return new ArrayList<>(usuarios);
    }
}
