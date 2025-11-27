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
import Server.exceptions.*;

public class ViewAllReservationsCommand implements Command {
    private final String BASE_DIR;
    private final UserDAO userDAO;
    private final String currentUserId;

    public ViewAllReservationsCommand(String baseDir, UserDAO userDAO, String currentUserId) {
        this.BASE_DIR = baseDir;
        this.userDAO = userDAO;
        this.currentUserId = currentUserId;
    }

    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) throws IOException, InvalidInputException, DatabaseException, AuthenticationException, BusinessLogicException {
        System.out.println("VIEW_ALL_RESERVATIONS - 권한 확인 userId: " + currentUserId);
        
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
                    // ✅ 파일 형식: 이름,방,날짜,요일,시간,목적,역할,상태,인원,userId
                    if (lineParts.length >= 10) {
                        String fileUserName = lineParts[0].trim();
                        String room = lineParts[1].trim();
                        String date = lineParts[2].trim();
                        String day = lineParts[3].trim();
                        String time = lineParts[4].trim();
                        String studentCount = lineParts[8].trim();
                        String resolvedUserId = lineParts[9].trim();

                        // ✅ 클라이언트 형식: userId,time,day,date,room,name,count
                        out.println(String.join(",", resolvedUserId, time, day, date, room, fileUserName, studentCount));
                        System.out.println("[ViewAllReservations] 전송: " + resolvedUserId + "," + time + "," + day + "," + date + "," + room + "," + fileUserName + "," + studentCount);
                    } else if (lineParts.length >= 9) {
                        // ✅ 구 형식 지원 (userId 없음)
                        String fileUserName = lineParts[0].trim();
                        String room = lineParts[1].trim();
                        String date = lineParts[2].trim();
                        String day = lineParts[3].trim();
                        String time = lineParts[4].trim();
                        String studentCount = lineParts[8].trim();
                        String resolvedUserId = userDAO.getUserIdByName(fileUserName);

                        // ✅ 클라이언트 형식: userId,time,day,date,room,name,count
                        out.println(String.join(",", resolvedUserId, time, day, date, room, fileUserName, studentCount));
                        System.out.println("[ViewAllReservations] 전송: " + resolvedUserId + "," + time + "," + day + "," + date + "," + room + "," + fileUserName + "," + studentCount);
                    }
                }
            } catch (IOException e) {
                out.println("ERROR_READING_ALL_RESERVATIONS");
                out.flush();
                continue;
            }
        }

        out.println("END_OF_APPROVED_RESERVATIONS");
        out.flush();

        return null;
    }

}
