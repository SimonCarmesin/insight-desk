package de.adesso.testing.ticketservice.serivce;

import de.adesso.testing.ticketservice.exception.InvalidCredentialsException;
import de.adesso.testing.ticketservice.exception.InvalidUserDataException;
import de.adesso.testing.ticketservice.exception.UserNotFoundException;
import de.adesso.testing.ticketservice.model.user.Role;
import de.adesso.testing.ticketservice.model.user.User;
import de.adesso.testing.ticketservice.repository.UserRepo;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepo userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User createUser(String name, String password) {
        if (name == null || name.isBlank() || password == null || password.isBlank()) {
            throw new InvalidUserDataException("Name and password must not be empty");
        }
        return userRepo.save(new User(name, passwordEncoder.encode(password)));
    }

    @Transactional
    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    @Transactional
    public User getUserById(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional
    public User updateUsername(Long id, String newUsername) {
        if (newUsername == null || newUsername.isBlank()) {
            throw new InvalidUserDataException("Username must not be empty");
        }
        User user = userRepo.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        user.setName(newUsername);
        return user;
    }

    @Transactional
    public void updatePassword(Long id, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new InvalidUserDataException("Password must not be empty");
        }
        User user = userRepo.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        user.setPassword(passwordEncoder.encode(newPassword));
    }

    @Transactional
    public User updateUserRole(Long id, String newRole) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        try {
            user.setRole(Role.valueOf(newRole));
        } catch (IllegalArgumentException e) {
            throw new InvalidUserDataException("Invalid role: " + newRole);
        }
        return user;
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        userRepo.delete(user);
    }

    public User login(String name, String rawPassword) {
        User user = userRepo.findByName(name)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return user;
    }
}