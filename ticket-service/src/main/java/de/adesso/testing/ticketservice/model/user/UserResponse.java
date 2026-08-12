package de.adesso.testing.ticketservice.model.user;

public record UserResponse(Long id, String name, Role role) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getRole());
    }
}