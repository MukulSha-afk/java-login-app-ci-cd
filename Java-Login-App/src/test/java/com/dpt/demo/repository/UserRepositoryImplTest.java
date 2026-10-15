package com.dpt.demo.repository;

import com.dpt.demo.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Repository tests against an isolated in-memory H2 database in MySQL
 * compatibility mode. The schema is re-created from {@code test-schema.sql} for
 * every test method, so tests are order-independent and never touch MySQL.
 */
class UserRepositoryImplTest {

    private static final String DB_URL = "jdbc:h2:mem:userrepo;MODE=MySQL;DB_CLOSE_DELAY=-1";

    private UserRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(DB_URL, "sa", "");
        dataSource.setDriverClassName("org.h2.Driver");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        // Drop everything from the previous test, then apply the test DDL.
        jdbcTemplate.execute("DROP ALL OBJECTS");
        new ResourceDatabasePopulator(new ClassPathResource("test-schema.sql")).execute(dataSource);
        repository = new UserRepositoryImpl(jdbcTemplate);
    }

    @Test
    void saveReturnsGeneratedIdAndRowIsFindable() {
        String hash = "$2a$10$storedBcryptHashIsOpaqueToTheRepository";
        long id = repository.save("Aarav", "Sharma", "aarav@example.com", "aarav", hash);

        assertTrue(id > 0, "insert must return the generated primary key");

        User found = repository.findByUsernameOrEmail("aarav");
        assertNotNull(found);
        assertEquals(Long.valueOf(id), found.getId());
        assertEquals("Aarav", found.getFirstName());
        assertEquals("Sharma", found.getLastName());
        assertEquals("aarav@example.com", found.getEmail());
        assertEquals("aarav", found.getUserName());
        assertEquals(hash, found.getPasswordHash(), "hash must round-trip unchanged");
    }

    @Test
    void lookupIsCaseInsensitiveForUserNameAndEmail() {
        repository.save("Aarav", null, "Aarav@Example.com", "Aarav", "hash-1");

        assertNotNull(repository.findByUsernameOrEmail("AARAV"));
        assertNotNull(repository.findByUsernameOrEmail("aarav"));
        assertNotNull(repository.findByUsernameOrEmail("AARAV@EXAMPLE.COM"));
        assertNotNull(repository.findByUsernameOrEmail("aarav@example.com"));
        assertNull(repository.findByUsernameOrEmail("nobody"));
    }

    @Test
    void blankOrNullLookupsReturnNullOrFalseWithoutQuerying() {
        assertNull(repository.findByUsernameOrEmail(null));
        assertNull(repository.findByUsernameOrEmail("   "));
        assertFalse(repository.existsByUsername(null));
        assertFalse(repository.existsByUsername("  "));
        assertFalse(repository.existsByEmail(""));
        assertEquals(0, repository.countByUsername(null));
        assertEquals(0, repository.countByEmail("   "));
    }
    @Test
    void existsAndCountUseCaseInsensitiveMatching() {
        repository.save("Aarav", null, "aarav@example.com", "aarav", "hash-1");
        repository.save("Bella", null, "bella@example.com", "bella", "hash-2");

        assertTrue(repository.existsByUsername("AARAV"));
        assertTrue(repository.existsByEmail("BELLA@EXAMPLE.COM"));
        assertFalse(repository.existsByUsername("carol"));
        assertFalse(repository.existsByEmail("carol@example.com"));

        assertEquals(1, repository.countByUsername("aarav"));
        assertEquals(1, repository.countByEmail("aarav@example.com"));
        assertEquals(0, repository.countByUsername("carol"));
    }

    @Test
    void duplicateUsernameViolatesTheUniqueConstraint() {
        repository.save("Aarav", null, "first@example.com", "duplicate", "hash-1");

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.save("Impostor", null, "second@example.com", "duplicate", "hash-2"));
        assertEquals(1, repository.countByUsername("duplicate"), "only the first row survives");
    }

    @Test
    void duplicateEmailViolatesTheUniqueConstraint() {
        repository.save("Aarav", null, "taken@example.com", "user-one", "hash-1");

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.save("Bella", null, "taken@example.com", "user-two", "hash-2"));
        assertEquals(1, repository.countByEmail("taken@example.com"));
    }
}