package de.adesso.testing.ticketservice.serivce;

import de.adesso.testing.ticketservice.AbstractIntegrationTest;
import de.adesso.testing.ticketservice.exception.InvalidUserDataException;
import de.adesso.testing.ticketservice.exception.UserNotFoundException;
import de.adesso.testing.ticketservice.model.user.User;
import de.adesso.testing.ticketservice.model.user.userrequests.CreateUserRequest;
import de.adesso.testing.ticketservice.repository.TicketRepo;
import de.adesso.testing.ticketservice.repository.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class UserServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TicketRepo ticketRepo;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepo userRepo;

    private User testUser;

    @BeforeEach
    void setUp() {
        ticketRepo.deleteAll();
        userRepo.deleteAll();
        testUser = userRepo.save(new User("Alex", "hashedPw"));
    }


    @Test
    void createUser_persistsToRealDatabase() {
        CreateUserRequest request = new CreateUserRequest("Bob", "rawPw");

        User created = userService.createUser(request.name(), request.password());

        User fromDb = userRepo.findById(created.getId()).orElseThrow();
        assertEquals("Bob", fromDb.getName());
        assertNotEquals("rawPw", fromDb.getPassword());
    }

    @Test
    void createUser_withBlankPassword_throwsException() {
        CreateUserRequest request = new CreateUserRequest("Bob", "");

        assertThrows(InvalidUserDataException.class, () -> userService.createUser(request.name(), request.password()));
    }

    @Test
    void createUser_withBlankName_throwsException() {
        CreateUserRequest request = new CreateUserRequest("", "rawPw");

        assertThrows(InvalidUserDataException.class, () -> userService.createUser(request.name(), request.password()));
    }

    @Test
    void getUserById_existingUser_returnsUser() {
        User found = userService.getUserById(testUser.getId());

        assertEquals(testUser.getName(), found.getName());
    }

    @Test
    void getUserById_nonExistingUser_throwsException() {
        assertThrows(UserNotFoundException.class, () -> userService.getUserById(999L));
    }

    @Test
    void updateUsername_valid() {
        User updated = userService.updateUsername(testUser.getId(), "NewName");

        User fromDb = userRepo.findById(updated.getId()).orElseThrow();

        assertEquals("NewName", fromDb.getName());
    }

}
