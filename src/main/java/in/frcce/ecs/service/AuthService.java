package in.frcce.ecs.service;

import in.frcce.ecs.dao.UserDAO;
import in.frcce.ecs.model.User;
import in.frcce.ecs.util.PasswordUtil;

import java.util.Optional;

/**
 * Authentication service. Verifies credentials against the users table.
 */
public class AuthService {

    private final UserDAO userDAO;
    private User currentUser;

    public AuthService() {
        this.userDAO = new UserDAO();
    }

    /**
     * Attempt login. Returns the authenticated User, or empty if credentials are wrong.
     */
    public Optional<User> login(String username, String password) {
        Optional<User> opt = userDAO.findByUsername(username.trim().toLowerCase());
        if (opt.isEmpty()) return Optional.empty();
        User u = opt.get();
        if (!u.isActive()) return Optional.empty();
        if (!PasswordUtil.verify(password, u.getPasswordHash())) return Optional.empty();
        userDAO.updateLastLogin(username);
        this.currentUser = u;
        return Optional.of(u);
    }

    public User getCurrentUser() { return currentUser; }

    public boolean isAdmin() { return currentUser != null && currentUser.isAdmin(); }

    public void logout() { currentUser = null; }
}

