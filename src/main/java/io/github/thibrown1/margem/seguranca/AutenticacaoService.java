package io.github.thibrown1.margem.seguranca;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AutenticacaoService {

    private static final String PREFIXO_PAPEL = "ROLE_";

    private final AuthenticationManager autenticador;
    private final JwtService jwt;

    public AutenticacaoService(AuthenticationManager autenticador, JwtService jwt) {
        this.autenticador = autenticador;
        this.jwt = jwt;
    }

    public TokenGerado login(String usuario, String senha) {
        Authentication autenticado = autenticador.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(usuario.trim().toLowerCase(Locale.ROOT), senha));
        Perfil perfil = autenticado.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(papel -> papel.startsWith(PREFIXO_PAPEL))
                .map(papel -> Perfil.valueOf(papel.substring(PREFIXO_PAPEL.length())))
                .findFirst()
                .orElseThrow(() -> new BadCredentialsException("Usuário sem perfil"));
        return jwt.gerar(autenticado.getName(), perfil);
    }
}
