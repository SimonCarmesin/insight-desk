package de.adesso.testing.userservice.controller;

import de.adesso.testing.userservice.exception.InvalidCredentialsException;
import de.adesso.testing.userservice.exception.InvalidUserDataException;
import de.adesso.testing.userservice.exception.UserNotFoundException;
import de.adesso.testing.userservice.model.User;
import de.adesso.testing.userservice.model.Role;
import de.adesso.testing.userservice.exception.GlobalExceptionHandler;
import de.adesso.testing.userservice.serivce.UserService;
import de.adesso.testing.userservice.model.userrequests.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = new User("Alex", "hashedPw123");
        existingUser.setId(1L);
        existingUser.setRole(Role.USER);
    }

    // --- POST /users ---

    @Test
    void createUser_validRequest_returns201() throws Exception {
        CreateUserRequest request = new CreateUserRequest("Alex", "rawPw");
        when(userService.createUser("Alex", "rawPw")).thenReturn(existingUser);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Alex"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void createUser_invalidData_returns400() throws Exception {
        CreateUserRequest request = new CreateUserRequest("", "");
        when(userService.createUser("", ""))
                .thenThrow(new InvalidUserDataException("Name and password must not be empty"));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // --- GET /users ---

    @Test
    void getUsers_returnsListOf200() throws Exception {
        User secondUser = new User("Sam", "hashedPw456");
        secondUser.setId(2L);
        when(userService.getAllUsers()).thenReturn(List.of(existingUser, secondUser));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Alex"));
    }

    @Test
    void getUsers_whenEmpty_returnsEmptyList() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of());

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // --- GET /users/{id} ---

    @Test
    void getUserById_existingId_returns200() throws Exception {
        when(userService.getUserById(1L)).thenReturn(existingUser);

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alex"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void getUserById_unknownId_returns404() throws Exception {
        when(userService.getUserById(99L)).thenThrow(new UserNotFoundException(99L));

        mockMvc.perform(get("/users/99"))
                .andExpect(status().isNotFound());
    }

    // --- PUT /users/{id} ---

    @Test
    void updateUsername_validRequest_returns200() throws Exception {
        UpdateUsernameRequest request = new UpdateUsernameRequest("NewName");
        User updatedUser = new User("NewName", "hashedPw123");
        updatedUser.setId(1L);
        when(userService.updateUsername(1L, "NewName")).thenReturn(updatedUser);

        mockMvc.perform(put("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("NewName"));
    }

    @Test
    void updateUsername_unknownId_returns404() throws Exception {
        UpdateUsernameRequest request = new UpdateUsernameRequest("NewName");
        when(userService.updateUsername(99L, "NewName")).thenThrow(new UserNotFoundException(99L));

        mockMvc.perform(put("/users/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // --- PUT /users/{id}/password ---

    @Test
    void updatePassword_validRequest_returns204() throws Exception {
        UpdatePasswordRequest request = new UpdatePasswordRequest("newRawPw");

        mockMvc.perform(put("/users/1/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    void updatePassword_blankPassword_returns400() throws Exception {
        UpdatePasswordRequest request = new UpdatePasswordRequest("   ");
        org.mockito.Mockito.doThrow(new InvalidUserDataException("Password must not be empty"))
                .when(userService).updatePassword(eq(1L), eq("   "));

        mockMvc.perform(put("/users/1/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // --- PUT /users/{id}/role ---

    @Test
    void updateUserRole_validRequest_returns200() throws Exception {
        UpdateUserRoleRequest request = new UpdateUserRoleRequest("ADMIN");
        User adminUser = new User("Alex", "hashedPw123");
        adminUser.setId(1L);
        adminUser.setRole(Role.ADMIN);
        when(userService.updateUserRole(1L, "ADMIN")).thenReturn(adminUser);

        mockMvc.perform(put("/users/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void updateUserRole_invalidRole_returns400() throws Exception {
        UpdateUserRoleRequest request = new UpdateUserRoleRequest("SUPERUSER");
        when(userService.updateUserRole(1L, "SUPERUSER"))
                .thenThrow(new InvalidUserDataException("Invalid role: SUPERUSER"));

        mockMvc.perform(put("/users/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // --- DELETE /users/{id} ---

    @Test
    void deleteUser_existingId_returns204() throws Exception {
        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteUser_unknownId_returns404() throws Exception {
        org.mockito.Mockito.doThrow(new UserNotFoundException(99L))
                .when(userService).deleteUser(99L);

        mockMvc.perform(delete("/users/99"))
                .andExpect(status().isNotFound());
    }

    // --- POST /users/login ---

    @Test
    void login_validCredentials_returns200() throws Exception {
        LoginRequest request = new LoginRequest("Alex", "rawPw");
        when(userService.login("Alex", "rawPw")).thenReturn(existingUser);

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alex"));
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        LoginRequest request = new LoginRequest("Alex", "wrongPw");
        when(userService.login("Alex", "wrongPw")).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}