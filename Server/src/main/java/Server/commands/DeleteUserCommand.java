/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Server.commands;

/**
 *
 * @author YangJinWon
 */
import Server.UserDAO;
import java.io.*;

public class DeleteUserCommand implements Command {

    private final String BASE_DIR;
    private final Object FILE_LOCK;
    private final UserDAO userDAO;
    private final String currentUserId;

    public DeleteUserCommand(String baseDir, Object fileLock, UserDAO userDAO, String currentUserId) {
        this.BASE_DIR = baseDir;
        this.FILE_LOCK = fileLock;
        this.userDAO = userDAO;
        this.currentUserId = currentUserId;
    }

    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) throws IOException {
        if (params.length != 2) {
            System.err.println("[ERROR] DELETE_USER 파라미터 개수 오류: " + params.length);
            return "INVALID_DELETE_FORMAT";
        }

        //  수정: currentUserId로 권한 확인
        System.out.println("[DEBUG] DELETE_USER - 권한 확인 userId: " + currentUserId);

        if (currentUserId == null || !userDAO.authorizeAccess(currentUserId)) {
            System.err.println("[ERROR] 권한 없음: " + currentUserId);
            return "ACCESS_DENIED";
        }

        String targetUserId = params[1].trim();
        System.out.println("[DEBUG] 삭제 대상: " + targetUserId);

        synchronized (FILE_LOCK) {
            boolean result = deleteUserById(targetUserId);
            return result ? "DELETE_SUCCESS" : "DELETE_FAILED";
        }
    }

    private boolean deleteUserById(String userId) {
        String filePath = getUserFileById(userId);
        if (filePath == null) {
            return false;
        }

        File inputFile = new File(filePath);
        File tempFile = new File(filePath + ".tmp");

        boolean found = false;
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile)); BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 2 && parts[1].trim().equals(userId)) {
                    found = true;
                    System.out.println("[DEBUG] 사용자 삭제: " + line);
                    continue;
                }
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        if (found) {
            inputFile.delete();
            return tempFile.renameTo(inputFile);
        } else {
            tempFile.delete();
            return false;
        }
    }

    private String getUserFileById(String userId) {
        if (userId == null) {
            return null;
        }
        if (userId.startsWith("S")) {
            return BASE_DIR + "/users.txt";
        }
        if (userId.startsWith("P")) {
            return BASE_DIR + "/prof.txt";
        }
        if (userId.startsWith("A")) {
            return BASE_DIR + "/assistant.txt";
        }
        return null;
    }
}
