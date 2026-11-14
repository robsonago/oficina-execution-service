package br.com.fiap.challange.execucao.infrastructure.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Mesmo comportamento do OS Service: reaproveita o X-Correlation-Id que
 * vier na requisição, para o log de um fluxo poder ser seguido entre serviços.
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    private static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            String incoming = request.getHeader(HEADER);
            String correlationId = incoming != null && !incoming.isBlank()
                    ? incoming : "execucao:" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            MDC.put(MDC_KEY, correlationId);
            response.setHeader(HEADER, correlationId);
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
