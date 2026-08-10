package de.adesso.testing.ticketservice.controller;

import de.adesso.testing.ticketservice.exception.UserNotFoundException;
import de.adesso.testing.ticketservice.model.CreateUserRequest;
import de.adesso.testing.ticketservice.model.UpdateUsernameRequest;
import de.adesso.testing.ticketservice.model.User;
import de.adesso.testing.ticketservice.repository.UserRepo;
import de.adesso.testing.ticketservice.serivce.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService, UserRepo userRepo) {
        this.userService = userService;
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request) {
        userService.createUser(request.name(), request.password());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users")
    public ResponseEntity<?> getUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            User user = userService.getUserById(id);
            return ResponseEntity.ok(user);
        } catch (UserNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUsername(
            @PathVariable Long id,
            @RequestBody UpdateUsernameRequest request) {
        userService.updateUsername(id, request.newUsername());
        return ResponseEntity.ok("Username updated");
    }
}
