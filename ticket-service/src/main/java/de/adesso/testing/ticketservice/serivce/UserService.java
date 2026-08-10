package de.adesso.testing.ticketservice.serivce;

import de.adesso.testing.ticketservice.model.User;
import de.adesso.testing.ticketservice.repository.UserRepo;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepo userRepo;

    public UserService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public void createUser(String name, String password) {
        if (name == null || name.isEmpty() || password == null || password.isEmpty()) {
            return;
        }
        userRepo.save(new User(name, password));
    }

    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    @Transactional
    public void updateUsername(Long id, String newUsername) {
        userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .setName(newUsername);
    }
}
