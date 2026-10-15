package com.dpt.demo.repository;

import com.dpt.demo.model.User;

/**
 * Data-access interface for the Employee table.
 *
 * <p>The {@link UserRepositoryImpl} implementation centralizes all database access.
 * Every query uses parameterized statements; no raw SQL is concatenated with user input.
 */
public interface UserRepository {

    /**
     * Locate a user by username or email (case-insensitive match).
     *
     * <p>Needed for login so both fields are accepted while avoiding user enumeration
     * through distinct error messages.
     */
    User findByUsernameOrEmail(String userName);

    /**
     * Check whether a username already exists.
     */
    boolean existsByUsername(String userName);

    /**
     * Check whether an email already exists.
     */
    boolean existsByEmail(String email);

    /**
     * Count the number of rows matching the provided username.
     */
    long countByUsername(String userName);

    /**
     * Count the number of rows matching the provided email.
     */
    long countByEmail(String email);

    /**
     * Insert a new user account.
     *
     * @param firstName  first name
     * @param lastName   last name
     * @param email      email address
     * @param userName   unique username
     * @param passwordHash  BCrypt hash produced by {@link
     *                      org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder}
     * @return the auto-generated primary key, or -1 if the insert failed
     */
    long save(String firstName, String lastName, String email,
              String userName, String passwordHash);
}