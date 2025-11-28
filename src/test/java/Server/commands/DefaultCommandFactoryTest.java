package Server.commands;

import org.junit.jupiter.api.*;
import Server.UserDAO;

import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultCommandFactoryTest {

    private DefaultCommandFactory factory;

    @BeforeEach
    void setUp() {
        UserDAO userDAO = new UserDAO();
        String baseDir = System.getProperty("user.dir") + java.io.File.separator + "data";
        Object fileLock = new Object();
        ConcurrentHashMap<String, Socket> loggedIn = new ConcurrentHashMap<>();
        AtomicInteger current = new AtomicInteger(0);
        int maxClients = 3;

        factory = new DefaultCommandFactory(userDAO, baseDir, fileLock, loggedIn, current, maxClients);
    }

    @Test
    void createLoginCommand() {
        Command cmd = factory.createCommand("LOGIN,S123,pass");
        assertNotNull(cmd);
        assertEquals(LoginCommand.class, cmd.getClass());
    }

    @Test
    void createRegisterCommand() {
        Command cmd = factory.createCommand("REGISTER,Name,S123,pass");
        assertNotNull(cmd);
        assertEquals(RegisterCommand.class, cmd.getClass());
    }

    @Test
    void createChangePasswordCommand() {
        Command cmd = factory.createCommand("CHANGE_PASSWORD,S123,old,new");
        assertNotNull(cmd);
        assertEquals(ChangePasswordCommand.class, cmd.getClass());
    }

    @Test
    void createGetAllUsersCommand() {
        Command cmd = factory.createCommand("GET_ALL_USERS");
        assertNotNull(cmd);
        assertEquals(GetAllUsersCommand.class, cmd.getClass());
    }

    @Test
    void createUpdateRoomCapacityCommand() {
        Command cmd = factory.createCommand("UPDATE_ROOM_CAPACITY");
        assertNotNull(cmd);
        assertEquals(UpdateRoomCapacityCommand.class, cmd.getClass());
    }

    @Test
    void createUnknownCommandReturnsNull() {
        Command cmd = factory.createCommand("FOOBAR_CMD");
        assertNull(cmd);
    }
}
