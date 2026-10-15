package com.dpt.demo.model;

/**
 * Domain representation of an Employee record in the database.
 *
 * <p>This is a pure data object. The password field is intentionally not exposed
 * through any API; only a BCrypt hash is stored and compared, never returned.
 */
public class User {

    private final Long id;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String userName;
    private final String passwordHash;

    public User(Long id, String firstName, String lastName, String email,
                String userName, String passwordHash) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.userName = userName;
        this.passwordHash = passwordHash;
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

    public String getPasswordHash() {
        return passwordHash;
    }
}