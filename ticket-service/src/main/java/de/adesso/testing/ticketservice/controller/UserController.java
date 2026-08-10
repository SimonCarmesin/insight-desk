package de.adesso.testing.ticketservice.controller;

import de.adesso.testing.ticketservice.model.UpdateUsernameRequest;
import de.adesso.testing.ticketservice.model.User;
import de.adesso.testing.ticketservice.serivce.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class UserController {

    public record CreateUserRequest(String name, String password) {
    }

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/users")
    public String createUser(@RequestBody CreateUserRequest request) {
        userService.createUser(request.name(), request.password());
        return "User created";
    }

    @GetMapping("/users")
    public List<User> getUsers() {
        return userService.getAllUsers();
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUsername(
            @PathVariable Long id,
            @RequestBody UpdateUsernameRequest request) {
        userService.updateUsername(id, request.newUsername());
        return ResponseEntity.ok("Username updated");
    }
}
