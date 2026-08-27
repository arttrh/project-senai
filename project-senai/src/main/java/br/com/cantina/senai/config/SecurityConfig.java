package br.com.cantina.senai.config;

import br.com.cantina.senai.security.UsuarioDetailsService;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Antes desta classe o projeto nao tinha autenticacao nenhuma: qualquer pessoa
 * na rede podia listar pedidos, mexer no estoque e apagar produtos chamando a
 * API direto. Aqui ficam as tres decisoes de seguranca:
 *
 *  1. quem e voce  -> login por e-mail e senha, hash BCrypt;
 *  2. o que pode   -> autorizacao por perfil (USUARIO / FUNCIONARIO / ADMIN);
 *  3. protecoes    -> CSRF nos formularios e no fetch, e cabecalhos de resposta.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final WebEndpointProperties webEndpointProperties;

    public SecurityConfig(WebEndpointProperties webEndpointProperties) {
        this.webEndpointProperties = webEndpointProperties;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * Registrado direto na cadeia, e nao como @Bean: um DaoAuthenticationProvider
     * exposto como bean desliga a configuracao automatica de UserDetailsService
     * do Spring Security e gera aviso no boot.
     */
    private DaoAuthenticationProvider authenticationProvider(UsuarioDetailsService detailsService,
                                                             PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(detailsService);
        provider.setPasswordEncoder(passwordEncoder);
        // Impede que a resposta diferencie "e-mail inexistente" de "senha errada".
        provider.setHideUserNotFoundExceptions(true);
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           UsuarioDetailsService detailsService,
                                           PasswordEncoder passwordEncoder) throws Exception {
        String actuator = webEndpointProperties.getBasePath();

        http
            .authenticationProvider(authenticationProvider(detailsService, passwordEncoder))
            // O token vai em cookie legivel pelo JS (XSRF-TOKEN) e volta no
            // header X-XSRF-TOKEN, que e como o fetch das telas envia.
            .csrf(csrf -> csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                    .ignoringRequestMatchers(PathPatternRequestMatcher.pathPattern(actuator + "/**")))

            .headers(headers -> headers
                    .frameOptions(frame -> frame.deny())
                    .contentSecurityPolicy(csp -> csp.policyDirectives(
                            "default-src 'self'; img-src 'self' data:; "
                                    + "style-src 'self' 'unsafe-inline'; script-src 'self'; "
                                    + "form-action 'self'; frame-ancestors 'none'; base-uri 'self'")))

            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                    .sessionFixation(fixation -> fixation.migrateSession()))

            .authorizeHttpRequests(auth -> auth
                    // Publico: login, auto-cadastro e assets.
                    .requestMatchers("/", "/login", "/erro").permitAll()
                    .requestMatchers(HttpMethod.GET, "/usuario/cadastrar").permitAll()
                    .requestMatchers(HttpMethod.POST, "/usuario/cadastrar").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/usuarios").permitAll()
                    .requestMatchers("/css/**", "/js/**", "/img/**", "/favicon.ico").permitAll()

                    // Observabilidade: liveness/readiness abertos para o
                    // orquestrador, o resto so para ADMIN.
                    .requestMatchers(actuator + "/health/**", actuator + "/info").permitAll()
                    .requestMatchers(actuator + "/**").hasRole("ADMIN")

                    // Operacao da cantina.
                    .requestMatchers("/funcionario/**", "/api/funcionario/**").hasAnyRole("FUNCIONARIO", "ADMIN")
                    .requestMatchers("/api/estoque/**").hasAnyRole("FUNCIONARIO", "ADMIN")
                    .requestMatchers(HttpMethod.POST, "/api/produtos/**").hasAnyRole("FUNCIONARIO", "ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/produtos/**").hasAnyRole("FUNCIONARIO", "ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/produtos/**").hasAnyRole("FUNCIONARIO", "ADMIN")

                    // Administracao de contas.
                    .requestMatchers(HttpMethod.GET, "/api/usuarios").hasRole("ADMIN")
                    .requestMatchers("/api/usuarios/*/tipo").hasRole("ADMIN")

                    // Cardapio: qualquer pessoa autenticada le.
                    .requestMatchers(HttpMethod.GET, "/api/produtos/**").authenticated()

                    .anyRequest().authenticated())

            .formLogin(form -> form
                    .loginPage("/login")
                    .loginProcessingUrl("/login")
                    .usernameParameter("email")
                    .passwordParameter("senha")
                    .successHandler(new RedirecionamentoPorPerfilHandler())
                    .failureUrl("/login?erro")
                    .permitAll())

            .logout(logout -> logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/login?saiu")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .permitAll())

            .exceptionHandling(handling -> handling
                    .defaultAuthenticationEntryPointFor(
                            new RespostaJsonNaoAutenticado(),
                            PathPatternRequestMatcher.pathPattern("/api/**")));

        return http.build();
    }
}
