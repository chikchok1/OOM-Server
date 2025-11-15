package Server.commands;

import Server.UserDAO;
import java.io.*;

public class RejectReservationCommand implements Command {
    private final String BASE_DIR;
    private final Object FILE_LOCK;
    private final UserDAO userDAO;
    private final String currentUserId;

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

        boolean isChangeRequest = false;
        String date = "";  // 날짜 정보 저장

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
                            tokens[3].trim().equals(day.trim()) &&
                            tokens[4].trim().equals(time.trim())) {
                            removed = true;
                            if (tokens.length >= 3) {
                                date = tokens[2].trim();  // 날짜 저장
                            }
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
                            isChangeRequest = true;
                            date = tokens[2].trim();  // 날짜 저장
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
                // 🔔 Observer 패턴: 예약 거절 알림 (로그로 확인)
                String notificationType = isChangeRequest ? "CHANGE_REJECTED" : "REJECTED";
                String message = isChangeRequest 
                    ? String.format("%s %s(%s) %s 예약 변경이 거절되었습니다.", room, date, day, time)
                    : String.format("%s %s(%s) %s 예약이 거절되었습니다.", room, date, day, time);
                
                System.out.println("[Observer 패턴] " + id + "에게 알림 전송: " + message);
                System.out.println("[Observer 패턴] 알림 유형: " + notificationType);
                
                System.out.println("[DEBUG] 거절 처리 완료");
                return "REJECT_SUCCESS";
            } else {
                System.err.println("[ERROR] 거절할 요청을 찾을 수 없음");
                return "REJECT_FAILED";
            }
        }
    }
}
