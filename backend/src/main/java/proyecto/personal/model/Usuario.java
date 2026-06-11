package proyecto.personal.model;

import jakarta.persistence.*;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Entity
@Table(name = "usuario")
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean enabled = true;

    // SISTEMA DE EVALUACIÓN DEL USUARIO

    @Column(name = "nota_maxima", nullable = false)
    private Double notaMaxima = 20.0;

    @Column(name = "nota_minima", nullable = false)
    private Double notaMinimaAprobatoria = 12.0;

    // RELACIÓN CON CURSOS

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Curso> cursos;

    // GETTERS Y SETTERS

    public Long getId() {
        return id;
    }

    public String getUsernameValue() {
        return username;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Double getNotaMaxima() {
        return notaMaxima;
    }

    public void setNotaMaxima(Double notaMaxima) {
        this.notaMaxima = notaMaxima;
    }

    public Double getNotaMinimaAprobatoria() {
        return notaMinimaAprobatoria;
    }

    public void setNotaMinimaAprobatoria(Double notaMinimaAprobatoria) {
        this.notaMinimaAprobatoria = notaMinimaAprobatoria;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<Curso> getCursos() {
        return cursos;
    }

    public void setCursos(List<Curso> cursos) {
        this.cursos = cursos;
    }

    // MÉTODOS DE SPRING SECURITY

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList(); // roles después
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}