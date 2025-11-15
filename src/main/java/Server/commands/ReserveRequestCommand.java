package Server.commands;

import common.manager.ClassroomManager;
import common.manager.ClassroomManager.Classroom;
import java.io.*;

public class ReserveRequestCommand implements Command {

    private final String BASE_DIR;
    private final Object FILE_LOCK;

    public ReserveRequestCommand(String baseDir, Object fileLock) {
        this.BASE_DIR = baseDir;
        this.FILE_LOCK = fileLock;
    }

    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) throws IOException {
        // ✅ 파라미터 개수 체크 (10개: 날짜 + userId 추가)
        System.out.println("[ReserveRequest] 받은 파라미터 개수: " + params.length);
        if (params.length != 10) {
            System.err.println("[ReserveRequest] 파라미터 개수 오류: " + params.length + ", 기대값: 10");
            return "INVALID_RESERVE_FORMAT";
        }

        String reserveName = params[1];
        String room = params[2];
        String dateString = params[3];  // ✅ 날짜 추가 (yyyy-MM-dd)
        String day = params[4];
        String time = params[5];
        String purpose = params[6];
        String role = params[7];
        int studentCount;
        String userId = params[9];  // ✅ 사용자 ID 추가
        
        // 학생 수 파싱
        try {
            studentCount = Integer.parseInt(params[8]);  // ✅ 인덱스 8로 변경
            System.out.println(String.format("[ReserveRequest] 예약 요청: %s, %s, %s(%s), %s, 인원: %d명",
                reserveName, room, dateString, day, time, studentCount));
        } catch (NumberFormatException e) {
            System.err.println("[ReserveRequest] 학생 수 파싱 오류: " + params[8]);
            return "INVALID_STUDENT_COUNT";
        }
        
        // ✅ 최소 하루 전 예약 검증
        try {
            java.time.LocalDate selectedDate = java.time.LocalDate.parse(dateString);
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate tomorrow = today.plusDays(1);
            
            if (selectedDate.isBefore(tomorrow)) {
                System.out.println(String.format(
                    "[ReserveRequest] 예약 날짜 오류: %s는 최소 하루 전에 예약해야 함 (오늘: %s)",
                    selectedDate, today
                ));
                return "RESERVE_DATE_TOO_EARLY";
            }
        } catch (java.time.format.DateTimeParseException e) {
            System.err.println("[ReserveRequest] 날짜 형식 오류: " + dateString);
            return "INVALID_DATE_FORMAT";
        }

        // Singleton으로 수용 인원 체크
        ClassroomManager manager = ClassroomManager.getInstance();
        
        // 강의실 존재 여부 확인
        if (!manager.exists(room)) {
            System.err.println("[ReserveRequest] 존재하지 않는 강의실: " + room);
            return "INVALID_ROOM";
        }

        synchronized (FILE_LOCK) {
            Classroom classroom = manager.getClassroom(room);
            
            //  ✅ 날짜 기반으로 해당 시간대에 이미 예약이 존재하는지 체크 
            boolean isAlreadyReserved = isTimeSlotReserved(room, dateString, time);
            
            if (isAlreadyReserved) {
                System.out.println(String.format(
                    "[ReserveRequest] 이미 예약된 시간: %s %s %s",
                    room, dateString, time
                ));
                return "RESERVE_CONFLICT";
            }
            
            System.out.println(String.format(
                "[ReserveRequest] 예약 가능: %s %s %s - 요청 인원: %d명",
                room, dateString, time, studentCount
            ));

            File file = new File(BASE_DIR + "/ReservationRequest.txt");
            file.getParentFile().mkdirs();

            // 예약 저장
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                // ✅ 날짜, 학생 수, 사용자 ID 포함하여 저장 (10개 필드)
                String data = String.format("%s,%s,%s,%s,%s,%s,%s,대기,%d,%s",
                    reserveName, room, dateString, day, time, purpose, role, studentCount, userId);
                writer.write(data);
                writer.newLine();
                
                System.out.println("[ReserveRequest] 예약 저장 완료: " + data);
                return "RESERVE_SUCCESS";
            } catch (IOException e) {
                System.err.println("[ReserveRequest] 저장 오류: " + e.getMessage());
                e.printStackTrace();
                return "RESERVE_FAILED";
            }
        }
    }
    
    /**
     * ✅ 특정 날짜/시간대에 이미 예약이 존재하는지 체크 (날짜 기반)
     * 대기 중이거나 승인된 예약이 있으면 true 반환
     */
    private boolean isTimeSlotReserved(String room, String dateString, String time) {
        // 1. 대기 중인 예약 체크
        File pendingFile = new File(BASE_DIR + "/ReservationRequest.txt");
        if (pendingFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(pendingFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    // 형식: name,room,dateString,day,time,purpose,role,status,studentCount,userId (10개)
                    if (parts.length >= 9 &&
                        parts[1].trim().equals(room.trim()) &&
                        parts[2].trim().equals(dateString.trim()) &&
                        parts[4].trim().equals(time.trim())) {
                        System.out.println("[예약체크] 대기 중 예약 발견: " + line);
                        return true; // 대기 중인 예약 존재
                    }
                }
            } catch (IOException e) {
                System.err.println("[예약체크] 파일 읽기 오류: " + e.getMessage());
            }
        }

        // 2. 승인된 예약 체크
        String[] approvedFiles = {
            BASE_DIR + "/ReserveClass.txt",
            BASE_DIR + "/ReserveLab.txt"
        };

        for (String filePath : approvedFiles) {
            File file = new File(filePath);
            if (!file.exists()) continue;

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    // 형식: name,room,dateString,day,time,purpose,role,status,studentCount,userId (10개)
                    if (parts.length >= 9 &&
                        parts[1].trim().equals(room.trim()) &&
                        parts[2].trim().equals(dateString.trim()) &&
                        parts[4].trim().equals(time.trim())) {
                        
                        String status = parts[7].trim();
                        if (status.equals("예약됨") || status.equals("승인")) {
                            System.out.println("[예약체크] 승인된 예약 발견: " + line);
                            return true; // 승인된 예약 존재
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("[예약체크] 파일 읽기 오류: " + filePath);
            }
        }

        return false; // 예약 없음
    }
}
