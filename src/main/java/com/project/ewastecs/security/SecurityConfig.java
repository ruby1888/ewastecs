package com.project.ewastecs.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserloginDetailsService userloginDetailsService;

    public SecurityConfig(UserloginDetailsService userloginDetailsService) {
        this.userloginDetailsService = userloginDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userloginDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationSuccessHandler successHandler() {
        return (HttpServletRequest req, HttpServletResponse res, Authentication auth) -> {
            // Each login form sends a hidden field _portal = "client" | "agent" | "admin"
            String portal = req.getParameter("_portal");

            boolean isAdmin  = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
            boolean isAgent  = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_AGENT"));
            boolean isClient = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_CLIENT"));

            // Detect portal/role mismatch and block cross-portal login
            boolean mismatch = false;
            String correctLogin = "/client/login";

            if ("client".equals(portal) && !isClient) {
                mismatch = true;
                correctLogin = isAdmin ? "/admin/login" : "/agent/login";
            } else if ("agent".equals(portal) && !isAgent) {
                mismatch = true;
                correctLogin = isAdmin ? "/admin/login" : "/client/login";
            } else if ("admin".equals(portal) && !isAdmin) {
                mismatch = true;
                correctLogin = isAgent ? "/agent/login" : "/client/login";
            }

            if (mismatch) {
                req.getSession().invalidate(); // do NOT leave them authenticated
                res.sendRedirect(req.getContextPath() + correctLogin + "?wrongportal");
                return;
            }

            // Normal redirect by role
            String redirect;
            if (isAdmin)       redirect = "/admin/dashboard";
            else if (isAgent)  redirect = "/agent/dashboard";
            else               redirect = "/client/dashboard";
            res.sendRedirect(req.getContextPath() + redirect);
        };
    }

    @Bean
    public AuthenticationFailureHandler failureHandler() {
        return (HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) -> {
            String referer = req.getHeader("Referer");
            String redirect = "/client/login?error";
            if (referer != null) {
                if (referer.contains("/admin/login"))  redirect = "/admin/login?error";
                else if (referer.contains("/agent/login")) redirect = "/agent/login?error";
            }
            res.sendRedirect(req.getContextPath() + redirect);
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(authenticationProvider())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", "/index", "/about", "/contact", "/contact/save",
                    "/client/register", "/client/register/save",
                    "/agent/register",  "/agent/register/save",
                    "/admin/login", "/client/login", "/agent/login",
                    "/api/**",
                    "/css/**", "/js/**", "/images/**", "/assets/**",
                    "/uploads/**", "/error", "/access-denied"
                ).permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/client/**").hasRole("CLIENT")
                .requestMatchers("/agent/**").hasRole("AGENT")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(successHandler())
                .failureHandler(failureHandler())
                .loginPage("/client/login")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/access-denied")
                .authenticationEntryPoint((request, response, authException) -> {
                    String path = request.getRequestURI();
                    if (path.startsWith("/admin"))       response.sendRedirect(request.getContextPath() + "/admin/login");
                    else if (path.startsWith("/agent"))  response.sendRedirect(request.getContextPath() + "/agent/login");
                    else                                 response.sendRedirect(request.getContextPath() + "/client/login");
                })
            );
        return http.build();
    }
}
