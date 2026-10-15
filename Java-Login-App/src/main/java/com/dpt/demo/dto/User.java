package com.dpt.demo.dto;

/**
 * Value object representing the currently authenticated user identity for the dashboard.
 *
 * <p>This is a minimal projection of the {@link com.dpt.demo.model.User} table row
 * needed to render the dashboard UI. It never carries the password hash.
 */
public class User {

    private final Long id;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String userName;

    public User(Long id, String firstName, String lastName, String email, String userName) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.userName = userName;
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getUserName() {
        return userName;
    }
}