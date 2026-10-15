package com.dpt.demo.controller;

import com.dpt.demo.HomeController;
import com.dpt.demo.SecurityConfig;
import com.dpt.demo.dto.UserRegistrationRequest;
import com.dpt.demo.model.User;
import com.dpt.demo.repository.UserRepository;
import com.dpt.demo.service.LoginAttemptService;
import com.dpt.demo.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test around the full Spring Security filter chain from
 * {@link SecurityConfig} combined with the real {@code login}, {@code register}
 * and {@link HomeController} controllers. Services and the repository are
 * mocked (no database); JSP rendering is asserted as a forward to the view.
 */
@WebMvcTest(controllers = {login.class, register.class, HomeController.class})
@Import(SecurityConfig.class)
class AuthFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private LoginAttemptService loginAttemptService;

    /** Makes the mocked repository return an account with a real BCrypt hash. */
    private void stubExistingUser(String name, String rawPassword) {
        String hash = passwordEncoder.encode(rawPassword);
        when(userRepository.findByUsernameOrEmail(name))
                .thenReturn(new User(1L, "First", "Last", name + "@example.com", name, hash));
    }

    // ------------------------------------------------------------------
    // Public pages, deny-by-default routing, generic error rendering
    // ------------------------------------------------------------------

    @Test
    void publicPagesRenderWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/pages/home.jsp"));
        mockMvc.perform(get("/home"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/pages/home.jsp"));
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/pages/login.jsp"));
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/pages/register.jsp"));
    }

    @Test
    void loginPageShowsOnlyGenericMessagesForAllFailureCauses() throws Exception {
        mockMvc.perform(get("/login").param("error", "invalid_credentials"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("genericLoginError"));

        mockMvc.perform(get("/login").param("error", "access_denied"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("genericLoginError"));

        mockMvc.perform(get("/login").param("error", "session_expired"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("errorMessage"));

        mockMvc.perform(get("/login").param("logout", "success"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("message"));
    }

    @Test
    void authenticatedVisitorIsForwardedFromLoginToDashboard() throws Exception {
        mockMvc.perform(get("/login").with(user("alice")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void dashboardRequiresAuthenticationAndUnknownRoutesAreDenied() throws Exception {
        // The security entry point redirects with an absolute URL in MockMvc,
        // hence the pattern matcher instead of an exact relative path.
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // ------------------------------------------------------------------
    // Login processing (POST /login owned by the security filter chain)
    // ------------------------------------------------------------------

    @Test
    void validCredentialsAuthenticateAndRedirectToDashboard() throws Exception {
        stubExistingUser("alice", "Password1");

        mockMvc.perform(post("/login")
                        .param("username", "alice")
                        .param("password", "Password1")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(loginAttemptService).loginSucceeded("alice");
        verify(loginAttemptService, never()).loginFailed(any());
    }

    @Test
    void unknownUserAndWrongPasswordShareOneGenericError() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "ghost")
                        .param("password", "Whatever1")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=invalid_credentials"));

        stubExistingUser("alice", "Password1");
        mockMvc.perform(post("/login")
                        .param("username", "alice")
                        .param("password", "WrongPassword1")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=invalid_credentials"));

        verify(loginAttemptService).loginFailed("ghost");
        verify(loginAttemptService).loginFailed("alice");
        verify(loginAttemptService, never()).loginSucceeded(any());
    }

    @Test
    void loginWithoutCsrfTokenNeverAttemptsAuthentication() throws Exception {
        MvcResult result = mockMvc.perform(post("/login")
                        .param("username", "alice")
                        .param("password", "Password1"))
                .andReturn();

        assertNotEquals("/dashboard", result.getResponse().getRedirectedUrl());
        verifyNoInteractions(loginAttemptService);
        verifyNoInteractions(userRepository);
    }

    // ------------------------------------------------------------------
    // Logout: POST-only, CSRF-protected, session invalidated
    // ------------------------------------------------------------------

    @Test
    void logoutConfirmationIsShownOnlyToAuthenticatedUsers() throws Exception {
        mockMvc.perform(get("/logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/logout").with(user("alice")))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/pages/logout.jsp"));
    }

    @Test
    void logoutPostWithCsrfInvalidatesTheSession() throws Exception {
        MockHttpSession session = new MockHttpSession();

        MvcResult result = mockMvc.perform(post("/logout")
                        .with(user("alice"))
                        .with(csrf())
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=success"))
                .andReturn();

        assertNull(result.getRequest().getSession(false), "session must be invalidated");
    }

    @Test
    void logoutWithoutCsrfTokenNeverSignsOut() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("marker", "alive");

        MvcResult result = mockMvc.perform(post("/logout")
                        .with(user("alice"))
                        .session(session))
                .andExpect(status().is3xxRedirection())
                // sendRedirect() keeps this path relative in MockMvc, so an exact match is safe.
                .andExpect(redirectedUrl("/login?error=session_expired"))
                .andReturn();

        assertEquals("alive", session.getAttribute("marker"),
                "the session must survive a logout attempt without a CSRF token");
    }

    // ------------------------------------------------------------------
    // Dashboard: identity comes from the security context only
    // ------------------------------------------------------------------

    @Test
    void dashboardRendersTheAuthenticatedUserAndIgnoresTamperedParameters() throws Exception {
        com.dpt.demo.dto.User alice = new com.dpt.demo.dto.User(
                1L, "Alice", "Liddell", "alice@example.com", "alice");
        when(userService.getUser("alice")).thenReturn(alice);

        mockMvc.perform(get("/dashboard")
                        .with(user("alice"))
                        .param("username", "mallory"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/pages/user.jsp"))
                .andExpect(model().attribute("user", alice));

        verify(userService).getUser("alice");
        verify(userService, never()).getUser("mallory");
    }

    @Test
    void dashboardRedirectsWhenTheAccountNoLongerExists() throws Exception {
        when(userService.getUser("ghost")).thenReturn(null);

        mockMvc.perform(get("/dashboard").with(user("ghost")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ------------------------------------------------------------------
    // Registration form handling (mocked service)
    // ------------------------------------------------------------------

    @Test
    void validRegistrationDelegatesToServiceAndRedirectsWithFlashMessage() throws Exception {
        when(userService.register(any(UserRegistrationRequest.class)))
                .thenReturn(UserService.Result.ok());

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("firstName", "Aarav")
                        .param("lastName", "Sharma")
                        .param("email", "aarav@example.com")
                        .param("userName", "aarav")
                        .param("password", "Password1")
                        .param("confirmPassword", "Password1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("message",
                        "Your account has been created. Please sign in."));

        verify(userService).register(any(UserRegistrationRequest.class));
    }

    @Test
    void beanValidationFailuresRerenderTheFormWithoutCallingTheService() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("firstName", "")
                        .param("lastName", "")
                        .param("email", "not-an-email")
                        .param("userName", "")
                        .param("password", "x")
                        .param("confirmPassword", ""))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/pages/register.jsp"))
                .andExpect(model().attributeHasFieldErrors("registration",
                        "firstName", "email", "userName", "password"));

        verifyNoInteractions(userService);
    }

    @Test
    void serviceLevelErrorsAreRenderedInline() throws Exception {
        Map<String, String> errors = new LinkedHashMap<>();
        errors.put("userName", "That username is already taken.");
        when(userService.register(any(UserRegistrationRequest.class)))
                .thenReturn(UserService.Result.of(errors));

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("firstName", "Aarav")
                        .param("lastName", "Sharma")
                        .param("email", "aarav@example.com")
                        .param("userName", "aarav")
                        .param("password", "Password1")
                        .param("confirmPassword", "Password1"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/pages/register.jsp"))
                .andExpect(model().attribute("errors", errors));
    }

    @Test
    void registrationWithoutCsrfTokenNeverReachesTheService() throws Exception {
        MvcResult result = mockMvc.perform(post("/register")
                        .param("firstName", "Evil")
                        .param("lastName", "Actor")
                        .param("email", "evil@example.com")
                        .param("userName", "evil")
                        .param("password", "Password1")
                        .param("confirmPassword", "Password1"))
                .andReturn();

        assertNull(result.getFlashMap() == null ? null : result.getFlashMap().get("message"),
                "no success flash without CSRF");
        verifyNoInteractions(userService);
    }
}