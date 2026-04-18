package com.project.ewastecs.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginSuccessController {

    @GetMapping("/login-success")
    public String loginSuccess(Authentication auth) {
        if (auth == null) return "redirect:/client/login";
        if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")))
            return "redirect:/admin/dashboard";
        if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_AGENT")))
            return "redirect:/agent/dashboard";
        return "redirect:/client/dashboard";
    }
}
