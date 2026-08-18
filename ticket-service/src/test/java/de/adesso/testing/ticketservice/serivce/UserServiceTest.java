package de.adesso.testing.ticketservice.serivce;

import de.adesso.testing.ticketservice.exception.InvalidCredentialsException;
import de.adesso.testing.ticketservice.exception.InvalidUserDataException;
import de.adesso.testing.ticketservice.exception.UserNotFoundException;
import de.adesso.testing.ticketservice.model.user.Role;
import de.adesso.testing.ticketservice.model.user.User;
import de.adesso.testing.ticketservice.repository.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = new User("Alex", "hashedPw123");
        existingUser.setId(1L);
        existingUser.setRole(Role.USER);
    }

    // --- createUser ---

    @Test
    void createUser_withValidData_savesAndReturnsUser() {
        when(passwordEncoder.encode("rawPw")).thenReturn("hashedPw123");
        when(userRepo.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.createUser("Alex", "rawPw");

        assertEquals("Alex", result.getName());
        assertEquals("hashedPw123", result.getPassword());
        verify(userRepo).save(any(User.class));
    }

    @Test
    void createUser_withBlankPassword_throwsException() {
        assertThrows(InvalidUserDataException.class,
                () -> userService.createUser("Alex", "   "));
        verify(userRepo, never()).save(any());
    }

    // --- getAllUsers ---

    @Test
    void getAllUsers_returnsAllUsersFromRepo() {
        User secondUser = new User("Sam", "hashedPw456");
        secondUser.setId(2L);
        when(userRepo.findAll()).thenReturn(List.of(existingUser, secondUser));

        List<User> result = userService.getAllUsers();

        assertEquals(2, result.size());
        assertTrue(result.contains(existingUser));
    }

    @Test
    void getAllUsers_whenEmpty_returnsEmptyList() {
        when(userRepo.findAll()).thenReturn(List.of());

        List<User> result = userService.getAllUsers();

        assertTrue(result.isEmpty());
    }

    // --- getUserById ---

    @Test
    void getUserById_existingId_returnsUser() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(existingUser));

        User result = userService.getUserById(1L);

        assertEquals(existingUser, result);
    }

    @Test
    void getUserById_unknownId_throwsException() {
        when(userRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserById(99L));
    }

    // --- updateUsername ---

    @Test
    void updateUsername_validName_updatesSuccessfully() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(existingUser));

        User result = userService.updateUsername(1L, "NewName");

        assertEquals("NewName", result.getName());
    }

    @Test
    void updateUsername_blankName_throwsException() {
        assertThrows(InvalidUserDataException.class,
                () -> userService.updateUsername(1L, ""));
        verify(userRepo, never()).findById(any());
    }

    // --- updatePassword ---

    @Test
    void updatePassword_validPassword_updatesSuccessfully() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode("newRawPw")).thenReturn("newHashedPw");

        userService.updatePassword(1L, "newRawPw");

        assertEquals("newHashedPw", existingUser.getPassword());
    }

    @Test
    void updatePassword_blankPassword_throwsException() {
        assertThrows(InvalidUserDataException.class,
                () -> userService.updatePassword(1L, "   "));
        verify(userRepo, never()).findById(any());
    }

    // --- updateUserRole ---

    @Test
    void updateUserRole_validRole_updatesSuccessfully() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(existingUser));

        User result = userService.updateUserRole(1L, "ADMIN");

        assertEquals(Role.ADMIN, result.getRole());
    }

    @Test
    void updateUserRole_invalidRole_throwsException() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(existingUser));

        assertThrows(InvalidUserDataException.class,
                () -> userService.updateUserRole(1L, "SUPERUSER"));
    }

    // --- deleteUser ---

    @Test
    void deleteUser_existingUser_deletesSuccessfully() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(existingUser));

        userService.deleteUser(1L);

        verify(userRepo).delete(existingUser);
    }

    @Test
    void deleteUser_unknownUser_throwsException() {
        when(userRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.deleteUser(99L));
        verify(userRepo, never()).delete(any());
    }

    // --- login ---

    @Test
    void login_validCredentials_returnsUser() {
        when(userRepo.findByName("Alex")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("rawPw", "hashedPw123")).thenReturn(true);

        User result = userService.login("Alex", "rawPw");

        assertEquals(existingUser, result);
    }

    @Test
    void login_wrongPassword_throwsException() {
        when(userRepo.findByName("Alex")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrongPw", "hashedPw123")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> userService.login("Alex", "wrongPw"));
    }

    @Test
    void login_unknownUsername_throwsException() {
        when(userRepo.findByName("Ghost")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> userService.login("Ghost", "anyPw"));
    }
}