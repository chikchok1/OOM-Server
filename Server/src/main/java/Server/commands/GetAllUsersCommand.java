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

public class GetAllUsersCommand implements Command {
    private final String BASE_DIR;
    private final UserDAO userDAO;
    private final String currentUserId; // 🔥 추가

    // 🔥 생성자에 currentUserId 추가
    public GetAllUsersCommand(String baseDir, UserDAO userDAO, String currentUserId) {
        this.BASE_DIR = baseDir;
        this.userDAO = userDAO;
        this.currentUserId = currentUserId;
    }

    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) throws IOException {
        // 🔥 수정: currentUserId로 권한 확인
        System.out.println("[DEBUG] GET_ALL_USERS - 권한 확인 userId: " + currentUserId);
        
        if (currentUserId == null || !userDAO.authorizeAccess(currentUserId)) {
            System.err.println("[ERROR] 권한 없음: " + currentUserId);
            return "ACCESS_DENIED";
        }

        sendUsersFromFile(BASE_DIR + "/users.txt", out);
        sendUsersFromFile(BASE_DIR + "/prof.txt", out);
        sendUsersFromFile(BASE_DIR + "/assistant.txt", out); // 조교도 추가
        out.println("END_OF_USERS");
        out.flush();

        return null;
    }

    private void sendUsersFromFile(String filePath, PrintWriter out) {
        File file = new File(filePath);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}