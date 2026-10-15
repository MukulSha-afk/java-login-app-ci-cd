package com.dpt.demo.controller;

import com.dpt.demo.model.User;
import com.dpt.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack integration tests: real Spring Security filter chain, real service
 * and repository layers, H2 in MySQL mode behind the {@code test} profile.
 * Nothing here ever touches the production MySQL database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM Employee");
    }

    /** Random suffix so tests are isolated from any leftover data or locks. */
    private String uniqueName() {
        return "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    /** Posts a registration that must be rejected by validation/service. */
    @SuppressWarnings("unchecked")
    private Map<String, String> registerExpectingFormErrors(String userName, String email,
                                                             String password, String confirmPassword) throws Exception {
        MvcResult result = mockMvc.perform(post("/register").with(csrf())
                        .param("firstName", "Test")
                        .param("lastName", "User")
                        .param("email", email)
                        .param("userName", userName)
                        .param("password", password)
                        .param("confirmPassword", confirmPassword))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/pages/register.jsp"))
                .andReturn();
        return (Map<String, String>) result.getModelAndView().getModel().get("errors");
    }

    /** Posts a registration that must succeed (redirect to login, PRG). */
    private void registerSuccessfully(String userName, String email, String password) throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("firstName", "Test")
                        .param("lastName", "User")
                        .param("email", email)
                        .param("userName", userName)
                        .param("password", password)
                        .param("confirmPassword", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ------------------------------------------------------------------
    // Registration: persistence, hashing, validation, uniqueness
    // ------------------------------------------------------------------

    @Test
    void registrationStoresOnlyABcryptHashNeverPlaintext() throws Exception {
        String name = uniqueName();
        registerSuccessfully(name, name + "@example.com", "Password1!");

        User row = userRepository.findByUsernameOrEmail(name);
        assertNotNull(row, "the row must be persisted");
        assertEquals("Test", row.getFirstName());
        assertEquals("User", row.getLastName());
        assertNotEquals("Password1!", row.getPasswordHash(), "plaintext must never be stored");
        assertTrue(row.getPasswordHash().startsWith("$2"), "stored hash must be a BCrypt hash");
        assertTrue(passwordEncoder.matches("Password1!", row.getPasswordHash()));
    }

    @Test
    void duplicateUsernameIsRejectedAndNothingIsPersisted() throws Exception {
        String name = uniqueName();
        registerSuccessfully(name, name + "@example.com", "Password1!");

        Map<String, String> errors = registerExpectingFormErrors(
                name, "other-" + name + "@example.com", "Password1!", "Password1!");

        assertNotNull(errors);
        assertTrue(errors.containsKey("userName"), "duplicate username must be reported");
        assertEquals(1, userRepository.countByUsername(name), "the duplicate must not persist");
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        String name = uniqueName();
        String email = name + "@example.com";
        registerSuccessfully(name, email, "Password1!");

        Map<String, String> errors = registerExpectingFormErrors(
                name + "-two", email, "Password1!", "Password1!");

        assertNotNull(errors);
        assertTrue(errors.containsKey("email"), "duplicate email must be reported");
        assertEquals(1, userRepository.countByEmail(email));
    }

    @Test
    void weakPasswordIsRejectedBeforeAnyInsert() throws Exception {
        String name = uniqueName();

        Map<String, String> errors = registerExpectingFormErrors(
                name, name + "@example.com", "weak", "weak");

        assertNotNull(errors);
        assertTrue(errors.containsKey("password"), "weak password must be reported");
        assertEquals(0, userRepository.countByUsername(name));
    }

    @Test
    void passwordConfirmationMismatchIsRejectedBeforeAnyInsert() throws Exception {
        String name = uniqueName();

        Map<String, String> errors = registerExpectingFormErrors(
                name, name + "@example.com", "Password1!", "Password2!");

        assertNotNull(errors);
        assertTrue(errors.containsKey("confirmPassword"), "mismatch must be reported");
        assertEquals(0, userRepository.countByUsername(name));
    }

    // ------------------------------------------------------------------
    // End-to-end authentication against the real user store
    // ------------------------------------------------------------------

    @Test
    void fullRegisterLoginDashboardLogoutFlow() throws Exception {
        String name = uniqueName();
        registerSuccessfully(name, name + "@example.com", "Password1!");

        MvcResult login = mockMvc.perform(post("/login")
                        .param("username", name)
                        .param("password", "Password1!")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andReturn();
        MockHttpSession authedSession = (MockHttpSession) login.getRequest().getSession(false);
        assertNotNull(authedSession, "a successful login must establish a session");

        MvcResult dashboard = mockMvc.perform(get("/dashboard").session(authedSession))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/pages/user.jsp"))
                .andReturn();
        com.dpt.demo.dto.User principal =
                (com.dpt.demo.dto.User) dashboard.getModelAndView().getModel().get("user");
        assertNotNull(principal, "the dashboard must expose the signed-in user");
        assertEquals(name, principal.getUserName());

        MvcResult logout = mockMvc.perform(post("/logout")
                        .with(csrf())
                        .session(authedSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=success"))
                .andReturn();
        assertNull(logout.getRequest().getSession(false), "logout must invalidate the session");

        boolean cookieCleared = logout.getResponse().getHeaders("Set-Cookie").stream()
                .anyMatch(h -> h.contains("REQUESTCASCADE_SESSION"));
        assertTrue(cookieCleared, "logout must clear the hardened session cookie");
    }

    @Test
    void sessionIdIsMigratedOnLoginToPreventSessionFixation() throws Exception {
        String name = uniqueName();
        registerSuccessfully(name, name + "@example.com", "Password1!");

        MockHttpSession preSession = new MockHttpSession();
        String idBeforeLogin = preSession.getId();

        MvcResult login = mockMvc.perform(post("/login")
                        .param("username", name)
                        .param("password", "Password1!")
                        .with(csrf())
                        .session(preSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andReturn();

        MockHttpSession authedSession = (MockHttpSession) login.getRequest().getSession(false);
        assertNotNull(authedSession);
        assertNotEquals(idBeforeLogin, authedSession.getId(),
                "the pre-login session id must be migrated on authentication");
    }

    // ------------------------------------------------------------------
    // Security behaviour: generic errors, lockout, CSRF, deny-by-default
    // ------------------------------------------------------------------

    @Test
    void failedLoginsReturnOneGenericErrorForUnknownUserAndWrongPassword() throws Exception {
        String name = uniqueName();
        registerSuccessfully(name, name + "@example.com", "Password1!");

        String wrongPassword = mockMvc.perform(post("/login")
                        .param("username", name).param("password", "Nope1234!").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();

        String unknownUser = mockMvc.perform(post("/login")
                        .param("username", name + "-ghost").param("password", "Nope1234!").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();

        assertEquals("/login?error=invalid_credentials", wrongPassword);
        assertEquals(wrongPassword, unknownUser,
                "identical redirect target prevents username enumeration");
    }

    @Test
    void accountLocksAfterRepeatedFailuresEvenWithTheCorrectPassword() throws Exception {
        String name = uniqueName();
        registerSuccessfully(name, name + "@example.com", "Password1!");

        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/login")
                            .param("username", name).param("password", "Wrong1234!").with(csrf()))
                    .andExpect(redirectedUrl("/login?error=invalid_credentials"));
        }

        // The default budget of 5 failures is exhausted: even the correct password must
        // fail now, and the response must stay generic so the lockout is not advertised.
        mockMvc.perform(post("/login")
                        .param("username", name).param("password", "Password1!").with(csrf()))
                .andExpect(redirectedUrl("/login?error=invalid_credentials"));
    }

    @Test
    void dashboardRequiresAuthentication() throws Exception {
        // The security entry point builds an absolute URL in MockMvc, hence the pattern.
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        // An authenticated session without a backing account is bounced to login.
        mockMvc.perform(get("/dashboard").with(user("intruder")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void registrationWithoutCsrfTokenIsRejectedAndPersistsNothing() throws Exception {
        String name = uniqueName();

        MvcResult result = mockMvc.perform(post("/register")
                        .param("firstName", "Evil")
                        .param("lastName", "Actor")
                        .param("email", name + "@example.com")
                        .param("userName", name)
                        .param("password", "Password1!")
                        .param("confirmPassword", "Password1!"))
                .andReturn();

        assertNull(result.getFlashMap() == null ? null : result.getFlashMap().get("message"),
                "no success flash without CSRF");
        assertEquals(0, userRepository.countByUsername(name), "nothing may be persisted");
    }
}