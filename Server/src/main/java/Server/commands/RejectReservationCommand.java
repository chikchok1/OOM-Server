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

public class RejectReservationCommand implements Command {
    private final String BASE_DIR;
    private final Object FILE_LOCK;
    private final UserDAO userDAO;
    private final String currentUserId; // 🔥 추가: 현재 로그인한 조교 ID

    //  생성자에 currentUserId 추가
    public RejectReservationCommand(String baseDir, Object fileLock, UserDAO userDAO, String currentUserId) {
        this.BASE_DIR = baseDir;
        this.FILE_LOCK = fileLock;
        this.userDAO = userDAO;
        this.currentUserId = currentUserId;
    }

    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) throws IOException {
        if (params.length != 6) {
            System.err.println("[ERROR] REJECT_RESERVATION 파라미터 개수 오류: " + params.length);
            return "INVALID_REJECT_FORMAT";
        }

        //  수정: currentUserId로 권한 확인 (params[1]이 아님!)
        System.out.println("[DEBUG] REJECT_RESERVATION - 권한 확인 userId: " + currentUserId);
        
        if (currentUserId == null || !userDAO.authorizeAccess(currentUserId)) {
            System.err.println("[ERROR] 권한 없음: " + currentUserId);
            return "ACCESS_DENIED";
        }

        String id = params[1];      // 예약 요청자 ID
        String time = params[2];
        String day = params[3];
        String room = params[4];
        String name2 = params[5];

        System.out.println("[DEBUG] 거절 처리: 요청자=" + name2 + ", 방=" + room + ", 시간=" + time);

        synchronized (FILE_LOCK) {
            File[] targets = {
                new File(BASE_DIR + "/ReservationRequest.txt"),
                new File(BASE_DIR + "/ChangeRequest.txt")
            };

            boolean removed = false;

            for (File file : targets) {
                if (!file.exists()) {
                    System.out.println("[WARN] 파일 없음: " + file.getName());
                    continue;
                }

                File temp = new File(BASE_DIR + "/temp_" + file.getName());
                try (BufferedReader reader = new BufferedReader(new FileReader(file));
                     BufferedWriter writer = new BufferedWriter(new FileWriter(temp))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] tokens = line.split(",");
                        
                        // ReservationRequest 매칭
                        if (file.getName().equals("ReservationRequest.txt") && tokens.length >= 4 &&
                            tokens[0].trim().equals(name2.trim()) &&
                            tokens[1].trim().equals(room.trim()) &&
                            tokens[2].trim().equals(day.trim()) &&
                            tokens[3].trim().equals(time.trim())) {
                            removed = true;
                            System.out.println("[DEBUG] ReservationRequest 삭제: " + line);
                            continue;
                        } 
                        // ChangeRequest 매칭
                        else if (file.getName().equals("ChangeRequest.txt") && tokens.length >= 5 &&
                                  tokens[0].trim().equals(id.trim()) &&
                                  tokens[1].trim().equals(time.trim()) &&
                                  tokens[2].trim().equals(day.trim()) &&
                                  tokens[3].trim().equals(room.trim()) &&
                                  tokens[4].trim().equals(name2.trim())) {
                            removed = true;
                            System.out.println("[DEBUG] ChangeRequest 삭제: " + line);
                            continue;
                        }
                        
                        writer.write(line);
                        writer.newLine();
                    }
                }
                file.delete();
                temp.renameTo(file);
            }

            if (removed) {
                System.out.println("[DEBUG] 거절 처리 완료");
                return "REJECT_SUCCESS";
            } else {
                System.err.println("[ERROR] 거절할 요청을 찾을 수 없음");
                return "REJECT_FAILED";
            }
        }
    }
}
