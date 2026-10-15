package com.dpt.demo.controller;

import com.dpt.demo.dto.User;
import com.dpt.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Page controller for the sign-in flow and the authenticated dashboard.
 *
 * <p>Authentication itself is <b>not</b> implemented here: POST {@code /login} and
 * POST {@code /logout} are owned by the Spring Security filter chain (CSRF-checked,
 * BCrypt-verified, session-fixation protected). This controller only serves the
 * JSP pages and the dashboard content for the authenticated principal.
 */
@Controller
public class login {

    private final UserService userService;

    @Autowired
    public login(UserService userService) {
        this.userService = userService;
    }

    /**
     * Sign-in page. Already-authenticated visitors are forwarded to the dashboard.
     * Error/log-out query parameters from the security handlers are translated into
     * generic user-facing messages - no cause details are ever echoed back.
     */
    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Model model) {
        if (isAuthenticated()) {
            return "redirect:/dashboard";
        }
        if (error != null) {
            if ("session_expired".equals(error)) {
                model.addAttribute("errorMessage", "Your session has expired. Please sign in again.");
            } else {
                // invalid_credentials, access_denied and unknown causes share one message.
                model.addAttribute("genericLoginError",
                        "Invalid username or password. Please try again.");
            }
        }
        if (logout != null) {
            model.addAttribute("message", "You have been signed out. Please sign in again.");
        }
        return "login";
    }

    /**
     * Account dashboard. Reads the username from the Spring Security context (the
     * single source of truth for the current session) - never from a request
     * parameter - so users cannot render someone else's dashboard by crafting a URL.
     */
    @GetMapping({"/dashboard", "/user"})
    public String dashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!isAuthenticated()) {
            return "redirect:/login";
        }
        String username = auth.getName();
        User user = userService.getUser(username);
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        return "user";
    }

    /**
     * Sign-out <b>confirmation</b> page. The actual sign-out is a CSRF-protected
     * POST handled by Spring Security; this GET never terminates a session by
     * itself (unsafe GET side effects are avoided on purpose).
     */
    @GetMapping("/logout")
    public String logoutPage() {
        if (!isAuthenticated()) {
            return "redirect:/login";
        }
        return "logout";
    }

    /** True only for a real, authenticated (non-anonymous) principal. */
    private static boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }
}
