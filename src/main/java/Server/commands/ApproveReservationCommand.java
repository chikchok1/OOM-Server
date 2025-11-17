package Server.commands;

import Server.UserDAO;
import common.manager.ClassroomManager;
import java.io.*;

public class ApproveReservationCommand implements Command {

    private final String BASE_DIR;
    private final Object FILE_LOCK;
    private final UserDAO userDAO;
    private final String currentUserId;

    public ApproveReservationCommand(String baseDir, Object fileLock, UserDAO userDAO, String currentUserId) {
        this.BASE_DIR = baseDir;
        this.FILE_LOCK = fileLock;
        this.userDAO = userDAO;
        this.currentUserId = currentUserId;
    }

    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) throws IOException {
        if (params.length != 7) {
            System.err.println("[ERROR] APPROVE_RESERVATION 파라미터 개수 오류: " + params.length);
            return "INVALID_APPROVE_FORMAT";
        }

        System.out.println("APPROVE_RESERVATION - 권한 확인 userId: " + currentUserId);

        // TA 또는 관리자 권한 확인
        if (currentUserId == null || !userDAO.authorizeAccess(currentUserId)) {
            System.err.println("[ERROR] 권한 없음: " + currentUserId);
            return "ACCESS_DENIED";
        }

        // 파라미터
        String userId = params[1].trim();      // 예약자 ID
        String time = params[2].trim();
        String date = params[3].trim();        // 날짜
        String day = params[4].trim();         // 요일
        String room = params[5].trim();
        String requesterName = params[6].trim();

        System.out.println("승인 처리: 요청자=" + requesterName + ", ID=" + userId + ", 방=" + room + ", 날짜=" + date + ", 요일=" + day + ", 시간=" + time);

        synchronized (FILE_LOCK) {
            String purpose = "", role = "";
            int studentCount = 0;
            String originalTime = "", originalDay = "", originalRoom = "";
            boolean found = false;
            boolean isChangeRequest = false;

            File[] sources = {
                new File(BASE_DIR + "/ReservationRequest.txt"),
                new File(BASE_DIR + "/ChangeRequest.txt")
            };

            for (File file : sources) {
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

                        if (file.getName().equals("ChangeRequest.txt") && tokens.length == 7) {
                            System.out.println("[WARN] 구버전 ChangeRequest 무시: " + line);
                            continue;
                        }

                        // ✅ ReservationRequest 처리
                        if (file.getName().equals("ReservationRequest.txt") && tokens.length >= 7 &&
                            tokens[0].trim().equals(requesterName.trim()) &&
                            tokens[1].trim().equals(room.trim())) {

                            String fileDate = "", fileDay = "", fileTime = "";
                            if (tokens.length >= 10) {
                                fileDate = tokens[2].trim();
                                fileDay = tokens[3].trim();
                                fileTime = tokens[4].trim();
                            }

                            boolean dateMatch = !fileDate.isEmpty() && fileDate.equals(date.trim());
                            boolean dayMatch = !fileDay.isEmpty() && fileDay.equals(day.trim());
                            boolean timeMatch = fileTime.equals(time.trim());

                            if (dateMatch && dayMatch && timeMatch) {
                                found = true;
                                purpose = tokens[5].trim();
                                role = tokens[6].trim();

                                try {
                                    studentCount = Integer.parseInt(tokens[8].trim());
                                } catch (NumberFormatException e) {
                                    studentCount = 1;
                                }

                                System.out.println("[ReservationRequest 찾음] " + line);
                                continue; // 승인 항목은 원본 파일에서 제거
                            }
                        }

                        // ✅ ChangeRequest 처리
                        if (file.getName().equals("ChangeRequest.txt") && tokens.length >= 10 &&
                            tokens[0].trim().equals(userId.trim()) &&
                            tokens[1].trim().equals(time.trim()) &&
                            tokens[2].trim().equals(date.trim()) &&
                            tokens[3].trim().equals(room.trim()) &&
                            tokens[4].trim().equals(requesterName.trim())) {

                            purpose = tokens[5].trim();
                            role = tokens[6].trim();
                            originalTime = tokens[7].trim();
                            originalDay = tokens[8].trim();
                            originalRoom = tokens[9].trim();

                            if (tokens.length >= 11) {
                                try {
                                    studentCount = Integer.parseInt(tokens[10].trim());
                                } catch (NumberFormatException e) {
                                    studentCount = 1;
                                }
                            }

                            found = true;
                            isChangeRequest = true;
                            System.out.println("[ChangeRequest 찾음] " + line);
                            continue; // 승인 항목 제거
                        }

                        // 다른 줄은 그대로 복사
                        writer.write(line);
                        writer.newLine();
                    }

                } catch (IOException e) {
                    System.err.println("[ERROR] 파일 처리 오류: " + e.getMessage());
                    return "APPROVE_FAILED_IO";
                }

                file.delete();
                temp.renameTo(file);
            }

            if (!found) {
                System.err.println("[오류] 승인할 요청을 찾을 수 없음");
                return "APPROVE_FAILED";
            }

            // ✅ 승인된 예약자 이름 조회
            String reserverName = userDAO.getUserNameById(userId);
            if (reserverName == null || reserverName.isEmpty()) {
                reserverName = requesterName; // fallback
            }

            System.out.println("[승인 완료] " + reserverName + "(" + userId + ") "
                    + room + " " + date + " (" + day + ") " + time + " - " + studentCount + "명");

            // ✅ 강의실 / 실습실 파일 구분
            ClassroomManager cm = ClassroomManager.getInstance();
            boolean isClass = "CLASS".equals(cm.getClassroom(room).type);

            String targetFile = isClass
                    ? BASE_DIR + "/ReserveClass.txt"
                    : BASE_DIR + "/ReserveLab.txt";
            
            // ✅ 변경 요청 시 기존 예약 삭제
            if (isChangeRequest) {
                deleteOriginalReservation(reserverName, originalRoom, originalDay, originalTime);
            }

            // ✅ 승인된 예약 저장 (일관된 포맷)
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile, true))) {
                // 이름,방,날짜,요일,시간,목적,권한,상태,학생수,아이디
                writer.write(String.join(",", reserverName, room, date, day, time,
                        purpose, role, "예약됨", String.valueOf(studentCount), userId));
                writer.newLine();
                System.out.println("[예약 추가 완료] " + reserverName + "," + room + "," + date + "," + day + "," + time);
            }

            // ✅ 백업 로그에도 동일 포맷 유지
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(BASE_DIR + "/ApprovedBackup.txt", true))) {
                writer.write(String.join(",", reserverName, room, date, day, time,
                        purpose, role, "승인", String.valueOf(studentCount), userId));
                writer.newLine();
            }

            // 🔔 Observer 패턴: 예약 승인 알림 (로그로 확인)
            String notificationType = isChangeRequest ? "CHANGE_APPROVED" : "APPROVED";
            String message = isChangeRequest 
                ? String.format("%s %s(%s) %s 예약 변경이 승인되었습니다.", room, date, day, time)
                : String.format("%s %s(%s) %s 예약이 승인되었습니다.", room, date, day, time);
            
            System.out.println("[Observer 패턴] " + userId + "에게 알림 전송: " + message);
            System.out.println("[Observer 패턴] 알림 유형: " + notificationType);
            System.out.println("[Observer 패턴] 예약자: " + reserverName + " (" + userId + ")");

            return "APPROVE_SUCCESS";
        }
    }

    /** ✅ 기존 예약 삭제 (변경 승인 시) */
    private void deleteOriginalReservation(String name, String room, String day, String time) {
        String normalizedRoom = room.replace("호", "").trim();
        
        ClassroomManager cm = ClassroomManager.getInstance();
        ClassroomManager.Classroom info = cm.getClassroom(room.endsWith("호") ? room : room + "호");
        boolean isClass = info != null && "CLASS".equals(info.type);
        String targetFile = isClass
                ? BASE_DIR + "/ReserveClass.txt"
                : BASE_DIR + "/ReserveLab.txt";

        File inputFile = new File(targetFile);
        File tempFile = new File(targetFile + ".tmp");

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length >= 10) {
                    boolean match =
                            tokens[0].trim().equals(name.trim()) &&
                            tokens[1].replace("호", "").trim().equals(normalizedRoom) &&
                            tokens[3].trim().equals(day.trim()) &&
                            tokens[4].trim().equals(time.trim());

                    if (match) {
                        System.out.println("[기존 예약 삭제] " + line);
                        continue;
                    }
                }
                writer.write(line);
                writer.newLine();
            }

        } catch (IOException e) {
            System.err.println("[ERROR] 기존 예약 삭제 실패: " + e.getMessage());
        }

        inputFile.delete();
        tempFile.renameTo(inputFile);
    }
}
