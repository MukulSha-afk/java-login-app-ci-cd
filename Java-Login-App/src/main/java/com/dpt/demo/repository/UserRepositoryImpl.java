package com.dpt.demo.repository;

import com.dpt.demo.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

/**
 * {@link UserRepository} implementation backed by Spring's {@link JdbcTemplate}.
 *
 * <p>Persistence rules enforced here:
 * <ul>
 *   <li>Every statement is parameterized - no user input is ever concatenated
 *       into SQL (no SQL injection surface).</li>
 *   <li>Lookups are case-insensitive so login works with the username or email
 *       in any casing.</li>
 *   <li>Spring translates driver errors into its {@link DataAccessException}
 *       hierarchy so callers never see raw SQL/driver messages.</li>
 * </ul>
 */
@Repository
public class UserRepositoryImpl implements UserRepository {

    private static final String FIND_BY_USERNAME_OR_EMAIL =
            "SELECT id, first_name, last_name, email, username, password "
                    + "FROM Employee "
                    + "WHERE LOWER(username) = LOWER(?) OR LOWER(email) = LOWER(?) "
                    + "LIMIT 1";

    private static final String COUNT_BY_USERNAME =
            "SELECT COUNT(*) FROM Employee WHERE LOWER(username) = LOWER(?)";

    private static final String COUNT_BY_EMAIL =
            "SELECT COUNT(*) FROM Employee WHERE LOWER(email) = LOWER(?)";

    private static final String INSERT_USER =
            "INSERT INTO Employee (first_name, last_name, email, username, password, regdate) "
                    + "VALUES (?, ?, ?, ?, ?, CURDATE())";

    private static final RowMapper<User> USER_ROW_MAPPER = (rs, rowNum) -> new User(
            rs.getLong("id"),
            rs.getString("first_name"),
            rs.getString("last_name"),
            rs.getString("email"),
            rs.getString("username"),
            rs.getString("password")
    );

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public UserRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public User findByUsernameOrEmail(String userName) throws DataAccessException {
        if (userName == null || userName.trim().isEmpty()) {
            return null;
        }
        String identifier = userName.trim();
        List<User> matches = jdbcTemplate.query(FIND_BY_USERNAME_OR_EMAIL, USER_ROW_MAPPER,
                identifier, identifier);
        return matches.isEmpty() ? null : matches.get(0);
    }

    @Override
    public boolean existsByUsername(String userName) throws DataAccessException {
        if (userName == null || userName.trim().isEmpty()) {
            return false;
        }
        Long count = jdbcTemplate.queryForObject(COUNT_BY_USERNAME, Long.class, userName.trim());
        return count != null && count > 0;
    }

    @Override
    public boolean existsByEmail(String email) throws DataAccessException {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        Long count = jdbcTemplate.queryForObject(COUNT_BY_EMAIL, Long.class, email.trim());
        return count != null && count > 0;
    }

    @Override
    public long countByUsername(String userName) throws DataAccessException {
        if (userName == null || userName.trim().isEmpty()) {
            return 0;
        }
        Long count = jdbcTemplate.queryForObject(COUNT_BY_USERNAME, Long.class, userName.trim());
        return count == null ? 0 : count;
    }

    @Override
    public long countByEmail(String email) throws DataAccessException {
        if (email == null || email.trim().isEmpty()) {
            return 0;
        }
        Long count = jdbcTemplate.queryForObject(COUNT_BY_EMAIL, Long.class, email.trim());
        return count == null ? 0 : count;
    }

    @Override
    public long save(String firstName, String lastName, String email,
                     String userName, String passwordHash) throws DataAccessException {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        // NOTE: deliberately NO try-with-resources around the statement - JdbcTemplate
        // closes the PreparedStatement it is handed once the update has executed.
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(INSERT_USER, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setString(3, email);
            ps.setString(4, userName);
            ps.setString(5, passwordHash);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        return key == null ? -1L : key.longValue();
    }
}
