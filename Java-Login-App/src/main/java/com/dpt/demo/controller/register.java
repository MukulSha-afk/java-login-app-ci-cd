package com.dpt.demo.controller;

import com.dpt.demo.dto.UserRegistrationRequest;
import com.dpt.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registration flow controller (Post/Redirect/Get).
 *
 * <p>GET renders the form; POST performs server-side validation twice: bean
 * validation on the {@link UserRegistrationRequest} DTO and full business rules in
 * {@link UserService} (trim, uniqueness, password policy, confirmation match,
 * BCrypt hashing, safe database error handling). On success the user is redirected
 * to the login page with a flash message; on failure the form re-renders with
 * field-level messages and no data is persisted.
 */
@Controller
@RequestMapping("/register")
public class register {

    private final UserService userService;

    @Autowired
    public register(UserService userService) {
        this.userService = userService;
    }

    /** Fresh, empty form object for binding. */
    @ModelAttribute("registration")
    public UserRegistrationRequest registrationRequest() {
        return new UserRegistrationRequest();
    }

    @GetMapping
    public String showForm() {
        return "register";
    }

    @PostMapping
    public String register(@Valid @ModelAttribute("registration") UserRegistrationRequest request,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (bindingResult.hasErrors()) {
            for (FieldError fieldError : bindingResult.getFieldErrors()) {
                errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
            }
        } else {
            UserService.Result result = userService.register(request);
            if (result.hasErrors()) {
                errors.putAll(result.getErrors());
            }
        }

        if (!errors.isEmpty()) {
            // Re-render the form with inline messages; nothing was persisted.
            model.addAttribute("errors", errors);
            return "register";
        }

        redirectAttributes.addFlashAttribute("message",
                "Your account has been created. Please sign in.");
        return "redirect:/login";
    }
}
