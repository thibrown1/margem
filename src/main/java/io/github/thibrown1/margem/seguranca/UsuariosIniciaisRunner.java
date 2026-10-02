package io.github.thibrown1.margem.seguranca;

import io.github.thibrown1.margem.configuracao.MargemProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.HexFormat;

/**
 * Cria os usuários "dono" e "expedicao" na primeira execução.
 * A senha vem da configuração; se não houver, é gerada uma aleatória e mostrada uma única vez no log.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UsuariosIniciaisRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UsuariosIniciaisRunner.class);

    private final SpringDataUsuarioRepository usuarios;
    private final PasswordEncoder codificador;
    private final MargemProperties propriedades;
    private final Clock relogio;

    public UsuariosIniciaisRunner(SpringDataUsuarioRepository usuarios, PasswordEncoder codificador,
                                  MargemProperties propriedades, Clock relogio) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.propriedades = propriedades;
        this.relogio = relogio;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        criarSeNaoExistir("dono", Perfil.DONO, propriedades.seguranca().senhaInicialDono(), "MARGEM_SENHA_DONO");
        criarSeNaoExistir("expedicao", Perfil.EXPEDICAO, propriedades.seguranca().senhaInicialExpedicao(),
                "MARGEM_SENHA_EXPEDICAO");
    }

    private void criarSeNaoExistir(String login, Perfil perfil, String senhaConfigurada, String variavel) {
        if (usuarios.existsByLogin(login)) {
            return;
        }
        String senha = senhaConfigurada;
        if (senha == null || senha.isBlank()) {
            byte[] bytes = new byte[9];
            new SecureRandom().nextBytes(bytes);
            senha = HexFormat.of().formatHex(bytes);
            log.warn("Usuário '{}' criado com a senha gerada {} (defina {} para escolher a senha)", login, senha, variavel);
        } else {
            log.info("Usuário '{}' criado com a senha definida em {}", login, variavel);
        }
        usuarios.save(new UsuarioEntity(login, codificador.encode(senha), perfil, relogio.instant()));
    }
}
