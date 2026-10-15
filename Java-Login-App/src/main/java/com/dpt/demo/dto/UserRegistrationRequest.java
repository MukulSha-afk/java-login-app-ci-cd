package com.dpt.demo.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * Request payload for the registration form.
 *
 * <p>Bean validation annotations mirror the HTML form attributes so obviously
 * invalid submissions are rejected during binding; the authoritative checks
 * (trim, uniqueness, password policy, confirmation match) live in
 * {@code com.dpt.demo.service.UserService}.
 */
public class UserRegistrationRequest {

    @NotBlank(message = "First name is required.")
    @Size(max = 100, message = "First name must be 100 characters or fewer.")
    private String firstName;

    /** Optional; limited in length when provided. */
    @Size(max = 100, message = "Last name must be 100 characters or fewer.")
    private String lastName;

    /** Optional; when provided it must be a plausible email address. */
    @Email(message = "Email must be a valid email address.")
    @Size(max = 254, message = "Email must be 254 characters or fewer.")
    private String email;

    @NotBlank(message = "Username is required.")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters.")
    private String userName;

    /** Minimum length and complexity are enforced again in the service. */
    @NotBlank(message = "Password is required.")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters.")
    private String password;

    /** Compared against {@code password} server-side before anything is persisted. */
    @NotBlank(message = "Please confirm your password.")
    private String confirmPassword;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
