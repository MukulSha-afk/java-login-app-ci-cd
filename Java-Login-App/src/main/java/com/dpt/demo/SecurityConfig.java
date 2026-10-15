package com.dpt.demo;

import com.dpt.demo.repository.UserRepository;
import com.dpt.demo.service.DatabaseUserDetailsService;
import com.dpt.demo.service.LoginAttemptService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.IOException;
import java.util.Locale;

/**
 * Spring Security configuration for the Java Login App.
 *
 * <p>Security model:
 * <ul>
 *   <li>Public: login page, registration form, sign-out confirmation page,
 *       required static assets, the home page and the error page.</li>
 *   <li>Authenticated: the dashboard and all account-specific routes.</li>
 *   <li>Denied: every other route (deny-by-default instead of permit-all).</li>
 * </ul>
 *
 * <p>Hardening applied:
 * <ul>
 *   <li>CSRF protection for browser form submissions (cookie-based token repository).</li>
 *   <li>Session fixation prevented: the session ID is migrated on login and an
 *       {@link HttpSessionEventPublisher} keeps the session registry consistent.</li>
 *   <li>Logging out via POST only; the session is invalidated and the cookie deleted.</li>
 *   <li>Login errors are generic so usernames cannot be enumerated.</li>
 *   <li>Repeated failed logins are throttled by {@link LoginAttemptService}.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Routes reachable without authentication (GET only). */
    private static final String[] PUBLIC_GET_PATHS = {
            "/", "/home", "/login", "/register", "/logout", "/error",
            "/css/**", "/js/**", "/images/**", "/favicon.ico"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           LoginAttemptService loginAttemptService) throws Exception {
        http
                // ---- CSRF protection (required for browser form submissions) ----
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                )
                .authorizeHttpRequests(auth -> auth
                        // antMatchers: the Spring Security 5.7-compatible method+pattern form.
                        .antMatchers(HttpMethod.GET, PUBLIC_GET_PATHS).permitAll()
                        // Registration is a public POST; every other mutation is denied.
                        .antMatchers(HttpMethod.POST, "/register").permitAll()
                        .antMatchers("/dashboard", "/user").authenticated()
                        // Deny-by-default: anything not listed requires auth or is refused.
                        .anyRequest().denyAll()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        // POST /login is owned by the security filter chain; parameters are
                        // the Spring Security defaults "username" and "password".
                        .successHandler((request, response, authentication) -> {
                            String username = request.getParameter("username");
                            if (username != null) {
                                loginAttemptService.loginSucceeded(key(username));
                            }
                            response.sendRedirect(request.getContextPath() + "/dashboard");
                        })
                        .failureHandler((request, response, exception) -> {
                            String username = request.getParameter("username");
                            if (username != null) {
                                loginAttemptService.loginFailed(key(username));
                            }
                            // Single generic error regardless of cause (bad user, bad password,
                            // lockout) - prevents enumeration and hides the lockout logic.
                            response.sendRedirect(request.getContextPath() + "/login?error=invalid_credentials");
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        // POST-only logout (the CSRF filter requires a valid token); GET /logout
                        // therefore reaches the confirmation page controller instead.
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
                        .logoutSuccessUrl("/login?logout=success")
                        .invalidateHttpSession(true)
                        // The session cookie may use the default or the configured name.
                        .deleteCookies("JSESSIONID", "REQUESTCASCADE_SESSION")
                        .permitAll()
                )
                .sessionManagement(session -> session
                        .sessionFixation().migrateSession()
                        .maximumSessions(1)
                        .expiredUrl("/login?error=session_expired")
                )
                .exceptionHandling(ex -> ex
                        // Redirect instead of accessDeniedPage: that option forwards and
                        // keeps the original POST method, which made the GET-only /login
                        // mapping answer "method not supported" (405) and surfaced as 500.
                        .accessDeniedHandler(new SessionExpiredRedirectHandler())
                );

        return http.build();
    }

    /** Normalization shared by the success/failure handlers and the user-details lookup. */
    private static String key(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Loads the account for form login. The stored BCrypt hash is returned as the
     * {@code UserDetails} password so {@link BCryptPasswordEncoder} can verify it;
     * plaintext passwords are never stored or compared directly.
     */
    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository,
                                                 LoginAttemptService loginAttemptService) {
        return new DatabaseUserDetailsService(userRepository, loginAttemptService);
    }

    /**
     * Centralized session event publisher so session-expiry events (logout, timeout,
     * concurrent-session expiry) are reflected in the session registry.
     */
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    /**
     * Enforces a strong one-way password hash for every stored credential.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Sends denied requests (most commonly a failed CSRF check caused by a stale or
     * missing token) to the login page via <b>redirect</b>.
     *
     * <p>{@code accessDeniedPage} forwards internally and preserves the original HTTP
     * method, so a rejected POST reached the GET-only {@code /login} mapping, produced
     * "Request method 'POST' not supported" and surfaced to the user as a 500. A
     * redirect re-issues the request as a clean GET and the user sees a friendly
     * "session expired" notice instead of an error page.
     */
    private static final class SessionExpiredRedirectHandler implements AccessDeniedHandler {

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response,
                           AccessDeniedException exception) throws IOException {
            response.sendRedirect(request.getContextPath() + "/login?error=session_expired");
        }
    }
}
