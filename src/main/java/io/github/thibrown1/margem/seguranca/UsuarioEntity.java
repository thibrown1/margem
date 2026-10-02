package io.github.thibrown1.margem.seguranca;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "usuario")
public class UsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String login;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Column(nullable = false, length = 20)
    private String perfil;

    @Column(nullable = false)
    private boolean ativo;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected UsuarioEntity() {
    }

    public UsuarioEntity(String login, String senhaHash, Perfil perfil, Instant criadoEm) {
        this.login = login;
        this.senhaHash = senhaHash;
        this.perfil = perfil.name();
        this.ativo = true;
        this.criadoEm = criadoEm;
    }

    public String getLogin() {
        return login;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public Perfil getPerfil() {
        return Perfil.valueOf(perfil);
    }

    public boolean isAtivo() {
        return ativo;
    }
}
