package io.github.thibrown1.margem.integracao;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SegurancaIT extends TesteDeIntegracao {

    @Test
    void loginDevolveTokenComOPerfil() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"DONO\",\"senha\":\"dono-teste\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.perfil").value("DONO"))
                .andExpect(jsonPath("$.expiraEm").value("2026-09-29T20:00:00-03:00"));
    }

    @Test
    void senhaErradaDa401SemDizerOQueEstavaErrado() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"dono\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Usuário ou senha inválidos."));
    }

    @Test
    void semTokenOuComTokenInvalidoDa401() throws Exception {
        mvc.perform(get("/api/produtos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Não autenticado"));

        mvc.perform(get("/api/produtos").header("Authorization", "Bearer token.falso.aqui"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Token inválido ou expirado. Faça login de novo."));
    }

    @Test
    void expedicaoVeFilaDeEnvioEEstoqueMasNaoVeDinheiro() throws Exception {
        String token = tokenDaExpedicao();

        executar(get("/api/pedidos/a-enviar"), token).andExpect(status().isOk());
        executar(get("/api/estoque"), token).andExpect(status().isOk());
        executar(get("/api/pedidos"), token).andExpect(status().isOk());

        executar(get("/api/relatorios/resumo"), token).andExpect(status().isForbidden());
        executar(get("/api/produtos"), token).andExpect(status().isForbidden());
        executar(get("/api/pedidos/QUALQUER/lucro"), token).andExpect(status().isForbidden());
        executar(post("/api/simulador/lucro").contentType(MediaType.APPLICATION_JSON).content("{}"), token)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acesso negado"));
    }

    @Test
    void documentacaoESaudeSaoPublicas() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andExpect(jsonPath("$.info.title").value("Margem API"));
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }
}
