package org.example;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Protege as rotas do painel. O site público só precisa ler a galeria e o mostruário;
 * todo o resto (clientes, orçamentos, enviar e excluir) exige login.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String metodo = request.getMethod();
        String caminho = request.getRequestURI();

        if ("OPTIONS".equalsIgnoreCase(metodo)) return true;
        if (rotaPublica(metodo, caminho)) return true;
        if (AuthController.tokenValido(request.getHeader("Authorization"))) return true;

        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"erro\":\"Faça login no painel para continuar.\"}");
        return false;
    }

    private boolean rotaPublica(String metodo, String caminho) {
        if (caminho.equals("/api/auth/login") || caminho.equals("/api/auth/sair")) return true;
        if (!"GET".equalsIgnoreCase(metodo)) return false;
        return caminho.equals("/api/galeria/listar")
                || caminho.startsWith("/api/galeria/listar/")
                || caminho.startsWith("/api/galeria/uploads/")
                || caminho.equals("/api/mostruario/listar");
    }
}
