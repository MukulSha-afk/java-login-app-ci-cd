package com.dpt.demo.dto;

import javax.validation.constraints.NotBlank;

/**
 * Request payload for credential verification.
 *
 * <p>Carries the submitted username (or email) and the submitted password exactly
 * as typed. The password field is only ever handed to
 * {@code PasswordEncoder.matches(...)} against the stored BCrypt hash; it is never
 * persisted or logged.
 */
public class UserLoginRequest {

    @NotBlank(message = "Username is required.")
    private String userName;

    @NotBlank(message = "Password is required.")
    private String password;

    public UserLoginRequest() {
    }

    public UserLoginRequest(String userName, String password) {
        this.userName = userName;
        this.password = password;
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
}
