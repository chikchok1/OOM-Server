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
                        String dateString = "";
                        String day = "";
                        String time = "";
                        String purpose = "";
                        String role = "";
                        String requestedPeople = "0";
                        String fileUserId = "";
                        
                        // ✅ 새 형식 (10개 필드: 이름,방,날짜,요일,시간,목적,권한,상태,학생수,userId)
                        if (lineParts.length >= 10) {
                            dateString = lineParts[2];
                            day = lineParts[3];
                            time = lineParts[4];
                            purpose = lineParts[5];
                            role = lineParts[6];
                            requestedPeople = lineParts[8];
                            fileUserId = lineParts[9];
                        }
                        // ✅ 중간 형식 (9개 필드: 이름,방,날짜,요일,시간,목적,권한,상태,학생수)
                        else if (lineParts.length >= 9) {
                            dateString = lineParts[2];
                            day = lineParts[3];
                            time = lineParts[4];
                            purpose = lineParts[5];
                            role = lineParts[6];
                            requestedPeople = lineParts[8];
                        }
                        // 구 형식 (이름,방,요일,시간,목적,권한,상태,학생수)
                        else {
                            day = lineParts[2];
                            time = lineParts[3];
                            purpose = lineParts[4];
                            role = lineParts[5];
                            requestedPeople = (lineParts.length >= 8) ? lineParts[7] : "0";
                        }

                        // ✅ userId 기반으로 필터링 (새 형식)
                        boolean shouldShow = false;
                        String userIdToSend = requestUserId;
                        
                        if (!fileUserId.isEmpty()) {
                            // userId 필드가 있으면 userId로 비교
                            if (isAssistant || fileUserId.equals(requestUserId)) {
                                shouldShow = true;
                                userIdToSend = fileUserId;
                            }
                        } else {
                            // userId 필드가 없으면 이름으로 비교 (호환성)
                            if (isAssistant || fileUserName.equals(userName)) {
                                shouldShow = true;
                                userIdToSend = isAssistant ? userDAO.getUserIdByName(fileUserName) : requestUserId;
                            }
                        }
                        
                        if (shouldShow) {
                            out.println(String.join(",", userIdToSend, time, dateString.isEmpty() ? day : dateString, room, fileUserName, purpose, requestedPeople));
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