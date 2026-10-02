package io.github.thibrown1.margem.integracao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base dos testes de integração: sobe a aplicação inteira (Spring, segurança, JPA, Flyway) com o relógio parado
 * em 29/09/2026 12:00 (horário de Brasília) e o banco limpo antes de cada teste.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TesteDeIntegracao.RelogioParado.class)
public abstract class TesteDeIntegracao {

    public static final Instant AGORA = Instant.parse("2026-09-29T15:00:00Z");

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void limparBanco() {
        jdbc.execute("DELETE FROM item_pedido");
        jdbc.execute("DELETE FROM pedido");
        jdbc.execute("DELETE FROM produto");
        jdbc.execute("DELETE FROM alerta_enviado");
    }

    protected String tokenDoDono() throws Exception {
        return token("dono", "dono-teste");
    }

    protected String tokenDaExpedicao() throws Exception {
        return token("expedicao", "expedicao-teste");
    }

    protected String token(String usuario, String senha) throws Exception {
        String resposta = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("usuario", usuario, "senha", senha))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(resposta).get("token").asText();
    }

    protected ResultActions executar(MockHttpServletRequestBuilder requisicao, String token) throws Exception {
        return mvc.perform(requisicao.header("Authorization", "Bearer " + token));
    }

    protected JsonNode corpo(ResultActions resultado) throws Exception {
        return json.readTree(resultado.andReturn().getResponse().getContentAsString());
    }

    @TestConfiguration
    static class RelogioParado {

        @Bean
        @Primary
        Clock relogioDeTeste() {
            return Clock.fixed(AGORA, ZoneId.of("America/Sao_Paulo"));
        }
    }
}
