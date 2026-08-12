package de.adesso.testing.ticketservice.controller;

import de.adesso.testing.ticketservice.model.user.UserResponse;
import de.adesso.testing.ticketservice.model.user.userrequests.*;
import de.adesso.testing.ticketservice.model.user.User;
import de.adesso.testing.ticketservice.serivce.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@RequestBody CreateUserRequest request) {
        User createdUser = userService.createUser(request.name(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(createdUser));
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getUsers() {
        List<UserResponse> users = userService.getAllUsers().stream()
                .map(UserResponse::from)
                .toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(UserResponse.from(userService.getUserById(id)));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<UserResponse> updateUsername(
            @PathVariable Long id,
            @RequestBody UpdateUsernameRequest request) {
        return ResponseEntity.ok(UserResponse.from(userService.updateUsername(id, request.newUsername())));
    }

    @PutMapping("/users/{id}/password")
    public ResponseEntity<Void> updatePassword(
            @PathVariable Long id,
            @RequestBody UpdatePasswordRequest request) {
        userService.updatePassword(id, request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable Long id,
            @RequestBody UpdateUserRoleRequest request) {
        return ResponseEntity.ok(UserResponse.from(userService.updateUserRole(id, request.newRole())));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/login")
    public ResponseEntity<User> login(@RequestBody LoginRequest request) {
        User user = userService.login(request.name(), request.password());
        return ResponseEntity.ok(user);
    }
}

