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

public class ViewMyReservationsCommand implements Command {
    private final String BASE_DIR;
    private final UserDAO userDAO;
    private final String currentUserId; // 🔥 추가

    // 🔥 생성자에 currentUserId 추가
    public ViewMyReservationsCommand(String baseDir, UserDAO userDAO, String currentUserId) {
        this.BASE_DIR = baseDir;
        this.userDAO = userDAO;
        this.currentUserId = currentUserId;
    }

    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) throws IOException {
        // 🔥 수정: params가 없으면 currentUserId 사용
        String requestUserId = (params.length > 1) ? params[1].trim() : currentUserId;
        
        if (requestUserId == null) {
            System.err.println("[ERROR] VIEW_MY_RESERVATIONS - userId가 null");
            return "INVALID_VIEW_MY_RESERVATIONS_FORMAT";
        }
        
        String userName = userDAO.getUserNameById(requestUserId);
        System.out.println("VIEW_MY_RESERVATIONS 요청: " + requestUserId + " → 이름: " + userName);

        boolean isAssistant = requestUserId.startsWith("A");

        File[] files = {
            new File(BASE_DIR + "/ReserveClass.txt"),
            new File(BASE_DIR + "/ReserveLab.txt")
        };

        for (File file : files) {
            if (!file.exists()) continue;

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] lineParts = line.split(",");

                    if (lineParts.length >= 7) {
                        String fileUserName = lineParts[0];
                        String room = lineParts[1];
                        String day = lineParts[2];
                        String time = lineParts[3];
                        String purpose = lineParts[4];
                        String role = lineParts[5];

                        if (isAssistant || fileUserName.equals(userName)) {
                            String userIdToSend = isAssistant ? userDAO.getUserIdByName(fileUserName) : requestUserId;
                            out.println(String.join(",", userIdToSend, time, day, room, fileUserName, purpose, role));
                        }
                    }
                }
            }
        }

        out.println("END_OF_MY_RESERVATIONS");
        out.flush();
        
        return null;
    }
}