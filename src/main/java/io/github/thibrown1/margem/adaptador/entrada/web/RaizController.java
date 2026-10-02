package io.github.thibrown1.margem.adaptador.entrada.web;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Hidden
@Controller
public class RaizController {

    @GetMapping("/")
    public String raiz() {
        return "redirect:/swagger-ui.html";
    }
}
