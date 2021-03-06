package com.internet.shop.dao.jdbc;

import com.internet.shop.dao.UserDao;
import com.internet.shop.exception.DataProcessingException;
import com.internet.shop.lib.Dao;
import com.internet.shop.model.Role;
import com.internet.shop.model.User;
import com.internet.shop.util.ConnectionUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Dao
public class UserDaoJdbcImpl implements UserDao {
    private static final Logger LOGGER = LogManager.getLogger(UserDaoJdbcImpl.class);

    @Override
    public User create(User element) {
        String insertUserQuery
                = "INSERT INTO users (name, login, password, salt) VALUES (?, ?, ?, ?);";
        try (Connection connection = ConnectionUtil.getConnection();
                PreparedStatement statement
                        = connection.prepareStatement(insertUserQuery,
                        Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, element.getName());
            statement.setString(2, element.getLogin());
            statement.setString(3, element.getPassword());
            statement.setBytes(4, element.getSalt());
            statement.executeUpdate();
            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                resultSet.next();
                element.setId(resultSet.getLong(1));
                insertUsersRoles(element, connection);
                LOGGER.log(Level.INFO, "{} was created", element);
                return element;
            }
        } catch (SQLException e) {
            throw new DataProcessingException("Unable to create " + element, e);
        }
    }

    @Override
    public Optional<User> findByLogin(String login) {
        String selectUserQuery = "SELECT * FROM users WHERE login = ?;";
        try (Connection connection = ConnectionUtil.getConnection();
                PreparedStatement statement
                        = connection.prepareStatement(selectUserQuery)) {
            statement.setString(1, login);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    User user = getUserFromResultSet(resultSet);
                    user.setRoles(getRolesFromUserId(user.getId(), connection));
                    return Optional.of(user);
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataProcessingException("Unable to get user with login " + login, e);
        }
    }

    @Override
    public Optional<User> get(Long id) {
        String selectUserQuery = "SELECT * FROM users WHERE user_id = ?;";
        try (Connection connection = ConnectionUtil.getConnection();
                PreparedStatement statement
                        = connection.prepareStatement(selectUserQuery)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    User user = getUserFromResultSet(resultSet);
                    user.setRoles(getRolesFromUserId(user.getId(), connection));
                    return Optional.of(user);
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataProcessingException("Unable to get user with ID " + id, e);
        }
    }

    @Override
    public List<User> getAll() {
        String selectAllUsersQuery = "SELECT * FROM users;";
        try (Connection connection = ConnectionUtil.getConnection();
                PreparedStatement statement
                        = connection.prepareStatement(selectAllUsersQuery);
                ResultSet resultSet = statement.executeQuery()) {
            List<User> allUsers = new ArrayList<>();
            while (resultSet.next()) {
                User user = getUserFromResultSet(resultSet);
                user.setRoles(getRolesFromUserId(user.getId(), connection));
                allUsers.add(user);
            }
            return allUsers;
        } catch (SQLException e) {
            throw new DataProcessingException("Unable to retrieve all users", e);
        }
    }

    @Override
    public User update(User element) {
        String updateUserQuery = "UPDATE users SET name = ?, login = ?, password = ?, salt = ? "
                + "WHERE user_id = ?;";
        try (Connection connection = ConnectionUtil.getConnection();
                PreparedStatement statement
                        = connection.prepareStatement(updateUserQuery)) {
            statement.setString(1, element.getName());
            statement.setString(2, element.getLogin());
            statement.setString(3, element.getPassword());
            statement.setBytes(4, element.getSalt());
            statement.setLong(5, element.getId());
            statement.executeUpdate();
            deleteUserFromUsersRoles(element.getId(), connection);
            insertUsersRoles(element, connection);
            LOGGER.log(Level.INFO, "{} was updated", element);
            return element;
        } catch (SQLException e) {
            throw new DataProcessingException("Unable to update " + element, e);
        }
    }

    @Override
    public boolean delete(Long id) {
        String deleteUserQuery = "DELETE FROM users WHERE user_id = ?;";
        try (Connection connection = ConnectionUtil.getConnection();
                PreparedStatement statement
                        = connection.prepareStatement(deleteUserQuery)) {
            deleteUserFromUsersRoles(id, connection);
            statement.setLong(1, id);
            int numberOfRowsDeleted = statement.executeUpdate();
            LOGGER.log(Level.INFO, "A user with id {} was deleted", id);
            return numberOfRowsDeleted != 0;
        } catch (SQLException e) {
            throw new DataProcessingException("Unable to delete user with ID " + id, e);
        }
    }

    private void insertUsersRoles(User user, Connection connection) throws SQLException {
        String selectRoleIdQuery = "SELECT role_id FROM roles WHERE role_name = ?";
        String insertUsersRolesQuery = "INSERT INTO users_roles (user_id, role_id) VALUES (?, ?);";
        try (PreparedStatement selectStatement
                     = connection.prepareStatement(selectRoleIdQuery);
                PreparedStatement insertStatement
                        = connection.prepareStatement(insertUsersRolesQuery)) {
            for (Role role : user.getRoles()) {
                selectStatement.setString(1, role.getRoleName().name());
                try (ResultSet resultSet = selectStatement.executeQuery()) {
                    resultSet.next();
                    insertStatement.setLong(1, user.getId());
                    insertStatement.setLong(2, resultSet.getLong("role_id"));
                    insertStatement.executeUpdate();
                }
            }
        }
    }

    private User getUserFromResultSet(ResultSet resultSet) throws SQLException {
        Long id = resultSet.getLong("user_id");
        String name = resultSet.getString("name");
        String login = resultSet.getString("login");
        String password = resultSet.getString("password");
        byte[] salt = resultSet.getBytes("salt");
        User user = new User(name, login, password);
        user.setId(id);
        user.setSalt(salt);
        return user;
    }

    private Set<Role> getRolesFromUserId(Long userId, Connection connection) throws SQLException {
        String selectRoleNameQuery = "SELECT role_name FROM users_roles "
                + "JOIN roles USING (role_id) WHERE user_id = ?;";
        try (PreparedStatement statement
                     = connection.prepareStatement(selectRoleNameQuery)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                Set<Role> roles = new HashSet<>();
                while (resultSet.next()) {
                    roles.add(Role.of(resultSet.getString("role_name")));
                }
                return roles;
            }
        }
    }

    private void deleteUserFromUsersRoles(Long userId, Connection connection) throws SQLException {
        String deleteUserQuery = "DELETE FROM users_roles WHERE user_id = ?;";
        try (PreparedStatement statement
                     = connection.prepareStatement(deleteUserQuery)) {
            statement.setLong(1, userId);
            statement.executeUpdate();
        }
    }
}
