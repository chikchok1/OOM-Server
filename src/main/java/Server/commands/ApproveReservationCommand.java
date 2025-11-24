package Server.commands;

import Server.UserDAO;
import Server.manager.ServerClassroomManager;
import common.dto.ClassroomDTO;
import common.observer.ReservationNotification;
import common.observer.ReservationSubject;
import java.io.*;
import Server.exceptions.*;
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
    public String execute(String[] params, BufferedReader in, PrintWriter out) throws IOException, InvalidInputException, DatabaseException, AuthenticationException, BusinessLogicException {
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
            String originalTime = "", originalDate = "", originalDay = "", originalRoom = "";
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

                        if (file.getName().equals("ChangeRequest.txt") && tokens.length < 13) {
                            System.out.println("[WARN] 구버전 ChangeRequest 무시 (" + tokens.length + "개 필드): " + line);
                            continue;
                        }

                        //  ReservationRequest 처리
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

                        //  ChangeRequest 처리
                        // ChangeRequest 포맷: ID,시간,날짜,요일,방,이름,목적,권한,원시간,원날짜,원요일,원방,인원 (13개)
                        if (file.getName().equals("ChangeRequest.txt") && tokens.length >= 13 &&
                            tokens[0].trim().equals(userId.trim()) &&
                            tokens[1].trim().equals(time.trim()) &&
                            tokens[2].trim().equals(date.trim()) &&
                            tokens[3].trim().equals(day.trim()) &&
                            tokens[4].trim().equals(room.trim()) &&
                            tokens[5].trim().equals(requesterName.trim())) {

                            purpose = tokens[6].trim();
                            role = tokens[7].trim();
                            originalTime = tokens[8].trim();
                            originalDate = tokens[9].trim();
                            originalDay = tokens[10].trim();
                            originalRoom = tokens[11].trim();

                            try {
                                studentCount = Integer.parseInt(tokens[12].trim());
                            } catch (NumberFormatException e) {
                                studentCount = 1;
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

            //  승인된 예약자 이름 조회
            String reserverName = userDAO.getUserNameById(userId);
            if (reserverName == null || reserverName.isEmpty()) {
                reserverName = requesterName; // fallback
            }

            System.out.println("[승인 완료] " + reserverName + "(" + userId + ") "
                    + room + " " + date + " (" + day + ") " + time + " - " + studentCount + "명");

            //  강의실 / 실습실 파일 구분
            ServerClassroomManager cm = ServerClassroomManager.getInstance();
            ClassroomDTO roomDto = cm.getClassroom(room);
            boolean isClass = roomDto != null && roomDto.isClassroom();

            String targetFile = isClass
                    ? BASE_DIR + "/ReserveClass.txt"
                    : BASE_DIR + "/ReserveLab.txt";
            
            //  변경 요청 시 기존 예약 삭제
            if (isChangeRequest) {
                deleteOriginalReservation(reserverName, originalRoom, originalDate, originalDay, originalTime);
            }

            //  승인된 예약 저장 (일관된 포맷)
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile, true))) {
                // 이름,방,날짜,요일,시간,목적,권한,상태,학생수,아이디
                writer.write(String.join(",", reserverName, room, date, day, time,
                        purpose, role, "예약됨", String.valueOf(studentCount), userId));
                writer.newLine();
                System.out.println("[예약 추가 완료] " + reserverName + "," + room + "," + date + "," + day + "," + time);
            }

            // 백업 로그에도 동일 포맷 유지
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(BASE_DIR + "/ApprovedBackup.txt", true))) {
                writer.write(String.join(",", reserverName, room, date, day, time,
                        purpose, role, "승인", String.valueOf(studentCount), userId));
                writer.newLine();
            }

            //  Observer 패턴: 클라이언트에게 실시간 알림 전송
            ReservationSubject subject = ReservationSubject.getInstance();
            ReservationNotification.NotificationType notificationType = 
                isChangeRequest ? ReservationNotification.NotificationType.CHANGE_APPROVED 
                                : ReservationNotification.NotificationType.APPROVED;
            
            String message = isChangeRequest 
                ? String.format("%s %s(%s) %s 예약 변경이 승인되었습니다.", room, date, day, time)
                : String.format("%s %s(%s) %s 예약이 승인되었습니다.", room, date, day, time);
            
            ReservationNotification notification = 
                new ReservationNotification(
                    userId, reserverName, room, date, day, time, notificationType, message
                );
            
            // 클라이언트에게 실제 알림 전송
            subject.notifyUser(notification);
            System.out.println("[Observer 패턴] " + userId + "에게 알림 전송 완료");

            return "APPROVE_SUCCESS";
        }
    }

    /**  기존 예약 삭제 (변경 승인 시) */
    private void deleteOriginalReservation(String name, String room, String date, String day, String time) {
        String normalizedRoom = room.replace("호", "").trim();
        
        ServerClassroomManager cm = ServerClassroomManager.getInstance();
        ClassroomDTO info = cm.getClassroom(room.endsWith("호") ? room : room + "호");
        boolean isClass = info != null && info.isClassroom();
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
                // 이름,방,날짜,요일,시간,목적,권한,상태,학생수,아이디 (10개 필드)
                if (tokens.length >= 10) {
                    boolean match =
                            tokens[0].trim().equals(name.trim()) &&
                            tokens[1].replace("호", "").trim().equals(normalizedRoom) &&
                            tokens[2].trim().equals(date.trim()) &&
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