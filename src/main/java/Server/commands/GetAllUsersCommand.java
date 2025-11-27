package Server.commands;

import Server.UserDAO;
import Server.exceptions.*;
import java.io.*;

public class GetAllUsersCommand implements Command {
    private final String BASE_DIR;
    private final UserDAO userDAO;
    private final String currentUserId;

    public GetAllUsersCommand(String baseDir, UserDAO userDAO, String currentUserId) {
        this.BASE_DIR = baseDir;
        this.userDAO = userDAO;
        this.currentUserId = currentUserId;
    }

    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) 
            throws IOException, InvalidInputException, DatabaseException, 
                   AuthenticationException, BusinessLogicException {
        
        System.out.println("GET_ALL_USERS - 권한 확인 userId: " + currentUserId);
        
        // 권한 확인
        if (currentUserId == null || !userDAO.authorizeAccess(currentUserId)) {
            System.err.println("권한 없음: " + currentUserId);
            throw new AuthenticationException(
                    AuthenticationException.AuthFailureReason.INSUFFICIENT_PERMISSION,
                    "관리자 권한이 필요합니다"
            );
        }

        // 모든 사용자 전송
        sendUsersFromFile(BASE_DIR + "/users.txt", out);
        sendUsersFromFile(BASE_DIR + "/prof.txt", out);
        sendUsersFromFile(BASE_DIR + "/assistant.txt", out);
        out.println("END_OF_USERS");
        out.flush();

        return null;
    }

    private void sendUsersFromFile(String filePath, PrintWriter out) throws DatabaseException {
        File file = new File(filePath);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.println(line);
            }
        } catch (IOException e) {
            throw new DatabaseException(
                    file.getName(),
                    DatabaseException.OperationType.READ,
                    "사용자 목록을 읽는 중 오류가 발생했습니다",
                    e
            );
        }
    }
}
