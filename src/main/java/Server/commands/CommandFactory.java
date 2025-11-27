package Server.commands;

import Server.UserDAO;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 추상 팩토리: 팩토리 메소드(createCommandInstance)를 서브클래스가 구현하도록 강제
 * LoginServer는 이 추상 클래스를 통해 명령을 생성한다.
 */
public abstract class CommandFactory {

    protected final UserDAO userDAO;
    protected final String BASE_DIR;
    protected final Object FILE_LOCK;
    protected final ConcurrentHashMap<String, Socket> loggedInUsers;
    protected final AtomicInteger currentClients;
    protected final int MAX_CLIENTS;

    public CommandFactory(UserDAO userDAO,
            String baseDir,
            Object fileLock,
            ConcurrentHashMap<String, Socket> loggedInUsers,
            AtomicInteger currentClients,
            int maxClients) {
        this.userDAO = userDAO;
        this.BASE_DIR = baseDir;
        this.FILE_LOCK = fileLock;
        this.loggedInUsers = loggedInUsers;
        this.currentClients = currentClients;
        this.MAX_CLIENTS = maxClients;
    }

    /**
     * 템플릿 메서드: 요청을 파싱하고, 서브클래스의 팩토리 메소드(createCommandInstance)를 호출
     */
    public Command createCommand(String request, String currentUserId) {
        if (request == null || request.isEmpty()) {
            return null;
        }

        String commandType = request.split(",")[0];
        return createCommandInstance(commandType, request, currentUserId);
    }

    public Command createCommand(String request) {
        return createCommand(request, null);
    }

    /**
     * 서브클래스가 구현할 팩토리 메소드
     */
    protected abstract Command createCommandInstance(String commandType, String request, String currentUserId);
}
