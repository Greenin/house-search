package dev.oscar2ia.housesearch.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityFilterConfig {

	@Bean
	public FilterRegistrationBean<ApiKeyFilter> apiKeyFilterRegistration(ApiKeyFilter apiKeyFilter) {
		FilterRegistrationBean<ApiKeyFilter> registration = new FilterRegistrationBean<>(apiKeyFilter);
		registration.addUrlPatterns("/api/house/insert", "/api/search/run", "/api/search/run-sin-playwright");
		return registration;
	}
}
