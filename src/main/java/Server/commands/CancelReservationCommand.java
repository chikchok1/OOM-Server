package Server.commands;

import common.observer.ReservationNotification;
import common.observer.ReservationSubject;
import java.io.*;
import Server.exceptions.*;
public class CancelReservationCommand implements Command {

    private final String BASE_DIR;
    private final Object FILE_LOCK;

    public CancelReservationCommand(String baseDir, Object fileLock) {
        this.BASE_DIR = baseDir;
        this.FILE_LOCK = fileLock;
    }

    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) throws IOException, InvalidInputException, DatabaseException, AuthenticationException, BusinessLogicException {
        // ✅ 8개 파라미터로 변경: CANCEL_RESERVATION,requesterId,userId,day,date,time,room,userName
        if (params.length != 8) {
            return "INVALID_CANCEL_FORMAT";
        }

        String requesterId = params[1].trim();  // ✅ 취소 요청한 사람
        String cancelUserId = params[2].trim(); // 예약 당사자
        String day = params[3].trim();          // 요일
        String date = params[4].trim();         // 날짜
        String time = params[5].trim();         // 교시
        String room = params[6].trim();
        String userName = params[7].trim();

        synchronized (FILE_LOCK) {
            String targetFile = (room.equals("908호") || room.equals("912호")
                    || room.equals("913호") || room.equals("914호"))
                    ? BASE_DIR + "/ReserveClass.txt"
                    : BASE_DIR + "/ReserveLab.txt";

            File inputFile = new File(targetFile);
            File tempFile = new File(targetFile + ".tmp");
            boolean deleted = false;
            int canceledStudentCount = 0;

            try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
                 BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] lineParts = line.split(",");
                    // ✅ 새 형식 (10개 필드) 또는 구 형식 (9개 필드) 모두 지원
                    boolean isNewFormat = lineParts.length >= 10;
                    boolean isMatch = false;
                    
                    if (isNewFormat) {
                        // 새 형식: 이름,방,날짜,요일,시간,목적,권한,상태,학생수,userId
                        String fileUserId = lineParts[9].trim();
                        // ✅ 날짜가 비어있으면 날짜 비교 생략
                        boolean dateMatch = date.isEmpty() || lineParts[2].trim().equals(date);
                        isMatch = lineParts.length >= 10
                                && (lineParts[0].trim().equals(userName) || fileUserId.equals(cancelUserId))
                                && lineParts[1].trim().equals(room)
                                && dateMatch
                                && lineParts[3].trim().equals(day)
                                && lineParts[4].trim().equals(time);
                    } else if (lineParts.length >= 9) {
                        // 구 형식: 이름,방,날짜,요일,시간,목적,권한,상태,학생수
                        // ✅ 날짜가 비어있으면 날짜 비교 생략
                        boolean dateMatch = date.isEmpty() || lineParts[2].trim().equals(date);
                        isMatch = lineParts[0].trim().equals(userName)
                                && lineParts[1].trim().equals(room)
                                && dateMatch
                                && lineParts[3].trim().equals(day)
                                && lineParts[4].trim().equals(time);
                    }
                    
                    if (isMatch) {
                        // 학생 수 추출
                        try {
                            canceledStudentCount = Integer.parseInt(lineParts[8].trim());
                        } catch (NumberFormatException e) {
                            canceledStudentCount = 1;
                        }
                        deleted = true;
                        System.out.println("[DEBUG] 예약 취소: " + line);
                        continue;
                    }
                    writer.write(line);
                    writer.newLine();
                }
            }

            if (deleted) {
                inputFile.delete();
                tempFile.renameTo(inputFile);
                System.out.println("[취소] " + room + " " + day + " " + time + " - 학생 수: " + canceledStudentCount + "명");
                
                // ✅ 자기 자신의 예약을 취소한 경우는 알림 X, 조교가 취소한 경우에만 알림 O
                if (!requesterId.equals(cancelUserId)) {
                    try {
                        ReservationSubject subject = ReservationSubject.getInstance();
                        ReservationNotification notification = new ReservationNotification(
                            cancelUserId,
                            userName,
                            room,
                            date,
                            day,
                            time,
                            ReservationNotification.NotificationType.CANCELLED,
                            String.format("예약이 조교에 의해 취소되었습니다 - %s %s(%s) %s", room, date, day, time)
                        );
                        subject.notifyUser(notification);
                        System.out.println("[취소 알림] " + cancelUserId + "에게 알림 전송 완료 (요청자: " + requesterId + ")");
                    } catch (Exception e) {
                        System.err.println("[취소 알림] 전송 실패: " + e.getMessage());
                    }
                } else {
                    System.out.println("[취소] 본인이 직접 취소함 - 알림 전송 안함");
                }
                
                return "CANCEL_SUCCESS";
            } else {
                tempFile.delete();
                return "CANCEL_FAILED_NOT_FOUND";
            }
        }
    }
}
