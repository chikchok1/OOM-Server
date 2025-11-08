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

public class ViewAllReservationsCommand implements Command {
    private final String BASE_DIR;
    private final UserDAO userDAO;
    private final String currentUserId; // 🔥 추가

    // 🔥 생성자에 currentUserId 추가
    public ViewAllReservationsCommand(String baseDir, UserDAO userDAO, String currentUserId) {
        this.BASE_DIR = baseDir;
        this.userDAO = userDAO;
        this.currentUserId = currentUserId;
    }

    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) throws IOException {
        // 🔥 수정: currentUserId로 권한 확인
        System.out.println("[DEBUG] VIEW_ALL_RESERVATIONS - 권한 확인 userId: " + currentUserId);
        
        if (currentUserId == null || !userDAO.authorizeAccess(currentUserId)) {
            System.err.println("[ERROR] 권한 없음: " + currentUserId);
            return "ACCESS_DENIED";
        }

        System.out.println("VIEW_ALL_RESERVATIONS 요청 수신");

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

                        String resolvedUserId = userDAO.getUserIdByName(fileUserName);
                        out.println(String.join(",", resolvedUserId, time, day, room, fileUserName));
                    }
                }
            } catch (IOException e) {
                out.println("ERROR_READING_ALL_RESERVATIONS");
                out.flush();
                continue;
            }
        }

        out.println("END_OF_MY_RESERVATIONS");
        out.flush();

        return null;
    }

}
