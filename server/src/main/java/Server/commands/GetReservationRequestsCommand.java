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

public class GetReservationRequestsCommand implements Command {

    private final String BASE_DIR;
    private final UserDAO userDAO;
        private final String currentUserId; // 추가: 현재 로그인한 사용자 ID

      // 생성자 수정: currentUserId 추가
    public GetReservationRequestsCommand(String baseDir, UserDAO userDAO, String currentUserId) {
        this.BASE_DIR = baseDir;
        this.userDAO = userDAO;
        this.currentUserId = currentUserId;
    }

    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) throws IOException {
        // 파라미터에서 userId를 가져오거나, 없으면 currentUserId 사용
        String userId = params.length > 1 ? params[1] : currentUserId;
        
        System.out.println("[DEBUG] GET_RESERVATION_REQUESTS - userId: " + userId);
        
        if (userId == null || userId.isEmpty()) {
            System.err.println("[ERROR] userId가 null입니다!");
            return "ACCESS_DENIED";
        }
        
        if (!userDAO.authorizeAccess(userId)) {
            System.out.println("[DEBUG] 권한 없음: " + userId);
            return "ACCESS_DENIED";
        }

        File[] files = {
            new File(BASE_DIR + "/ChangeRequest.txt"),
            new File(BASE_DIR + "/ReservationRequest.txt")
        };

        for (File file : files) {
            if (!file.exists()) continue;

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] partsLine = line.split(",");

                    if (file.getName().equals("ChangeRequest.txt") && partsLine.length >= 5) {
                        String id = partsLine[0].trim();
                        String time = partsLine[1].trim();
                        String day = partsLine[2].trim();
                        String room = partsLine[3].trim();
                        String userName = partsLine[4].trim();
                        
                        // 학생 수 추출 (필드 10번째, 인덱스 10)
                        String studentCount = "1"; // 기본값
                        if (partsLine.length >= 11) {
                            studentCount = partsLine[10].trim();
                        }
                        
                        out.println(String.join(",", id, time, day, room, userName, studentCount));
                    } else if (file.getName().equals("ReservationRequest.txt") && partsLine.length >= 4) {
                        String name2 = partsLine[0].trim();
                        String room = partsLine[1].trim();
                        String day = partsLine[2].trim();
                        String time = partsLine[3].trim();
                        String id2 = userDAO.getUserIdByName(name2);
                        
                        // 학생 수 추출 (필드 7번째, 인덱스 7)
                        String studentCount = "1"; // 기본값
                        if (partsLine.length >= 8) {
                            studentCount = partsLine[7].trim();
                        }
                        
                        out.println(String.join(",", id2, time, day, room, name2, studentCount));
                    }
                }
            } catch (IOException e) {
                out.println("GET_REQUESTS_ERROR");
                out.flush();
                continue;
            }
        }

        out.println("END_OF_REQUESTS");
        out.flush();
        
        return null;
    }
}