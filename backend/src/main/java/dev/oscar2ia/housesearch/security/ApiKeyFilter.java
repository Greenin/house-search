package dev.oscar2ia.housesearch.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Filtro de servlet puro (no Spring Security) que exige el header X-API-Key en
 * las rutas sensibles registradas en SecurityFilterConfig. Deja pasar OPTIONS
 * sin comprobar (preflight CORS) y anade Access-Control-Allow-Origin a mano en
 * el 401 porque ese error se genera antes de que Spring MVC aplique CORS.
 */
@Component
public class ApiKeyFilter extends HttpFilter {

	private static final String HEADER = "X-API-Key";

	private final String apiKeyEsperada;
	private final String allowedOrigin;

	public ApiKeyFilter(
			@Value("${app.insert-api-key}") String apiKeyEsperada,
			@Value("${app.cors.allowed-origin}") String allowedOrigin) {
		this.apiKeyEsperada = apiKeyEsperada;
		this.allowedOrigin = allowedOrigin;
	}

	@Override
	protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			chain.doFilter(request, response);
			return;
		}

		String apiKeyRecibida = request.getHeader(HEADER);
		if (apiKeyRecibida == null || !apiKeyRecibida.equals(apiKeyEsperada)) {
			String origin = request.getHeader("Origin");
			if (allowedOrigin.equals(origin)) {
				response.setHeader("Access-Control-Allow-Origin", allowedOrigin);
			}
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "API key invalida o ausente");
			return;
		}

		chain.doFilter(request, response);
	}
}
