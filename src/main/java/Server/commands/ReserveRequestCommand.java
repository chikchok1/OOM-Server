package Server.commands;

import Server.exceptions.*;
import Server.manager.ServerClassroomManager;
import common.dto.ClassroomDTO;
import java.io.*;

public class ReserveRequestCommand implements Command {

    private final String BASE_DIR;
    private final Object FILE_LOCK;

    public ReserveRequestCommand(String baseDir, Object fileLock) {
        this.BASE_DIR = baseDir;
        this.FILE_LOCK = fileLock;
    }

    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) 
            throws IOException, InvalidInputException, DatabaseException, 
                   AuthenticationException, BusinessLogicException {
        
        // 파라미터 개수 체크
        System.out.println("[ReserveRequest] 받은 파라미터 개수: " + params.length);
        if (params.length != 10) {
            throw new InvalidInputException(
                    "예약 명령은 10개의 매개변수가 필요합니다 (현재: " + params.length + "개)"
            );
        }

        String reserveName = params[1];
        String room = params[2];
        String dateString = params[3];
        String day = params[4];
        String time = params[5];
        String purpose = params[6];
        String role = params[7];
        int studentCount;
        String userId = params[9];
        
        // 학생 수 파싱
        try {
            studentCount = Integer.parseInt(params[8]);
            System.out.println(String.format("[ReserveRequest] 예약 요청: %s, %s, %s(%s), %s, 인원: %d명",
                reserveName, room, dateString, day, time, studentCount));
        } catch (NumberFormatException e) {
            throw new InvalidInputException("studentCount", params[8], 
                    "사용 인원은 숫자여야 합니다");
        }
        
        // 최소 하루 전 예약 검증
        try {
            java.time.LocalDate selectedDate = java.time.LocalDate.parse(dateString);
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate tomorrow = today.plusDays(1);
            
            if (selectedDate.isBefore(tomorrow)) {
                throw new BusinessLogicException(
                        BusinessLogicException.BusinessRuleViolation.INVALID_DATE,
                        "최소 하루 전에 예약해야 합니다"
                );
            }
        } catch (java.time.format.DateTimeParseException e) {
            throw new InvalidInputException("date", dateString, 
                    "올바른 날짜 형식이 아닙니다 (yyyy-MM-dd)");
        }

        ServerClassroomManager manager = ServerClassroomManager.getInstance();
        
        // 강의실 존재 여부 확인
        if (!manager.exists(room)) {
            throw new BusinessLogicException(
                    BusinessLogicException.BusinessRuleViolation.ROOM_UNAVAILABLE,
                    "존재하지 않는 강의실입니다: " + room
            );
        }

        synchronized (FILE_LOCK) {
            // 시간대 충돌 체크
            boolean isAlreadyReserved = isTimeSlotReserved(room, dateString, time);
            
            if (isAlreadyReserved) {
                throw new BusinessLogicException(
                        BusinessLogicException.BusinessRuleViolation.RESERVATION_CONFLICT,
                        String.format("%s %s %s는 이미 예약되어 있습니다", room, dateString, time)
                );
            }
            
            System.out.println(String.format(
                "[ReserveRequest] 예약 가능: %s %s %s - 요청 인원: %d명",
                room, dateString, time, studentCount
            ));

            File file = new File(BASE_DIR + "/ReservationRequest.txt");
            file.getParentFile().mkdirs();

            // 예약 저장
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                String data = String.format("%s,%s,%s,%s,%s,%s,%s,대기중,%d,%s",
                    reserveName, room, dateString, day, time, purpose, role, studentCount, userId);
                writer.write(data);
                writer.newLine();
                
                System.out.println("[ReserveRequest] 예약 저장 완료: " + data);
                return "RESERVE_SUCCESS";
            } catch (IOException e) {
                throw new DatabaseException(
                        "ReservationRequest.txt",
                        DatabaseException.OperationType.WRITE,
                        "예약 저장 중 오류가 발생했습니다",
                        e
                );
            }
        }
    }
    
    /**
     * 특정 날짜/시간대에 이미 예약이 존재하는지 체크
     */
    private boolean isTimeSlotReserved(String room, String dateString, String time) throws DatabaseException {
        // 1. 대기 중인 예약 체크
        File pendingFile = new File(BASE_DIR + "/ReservationRequest.txt");
        if (pendingFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(pendingFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 9 &&
                        parts[1].trim().equals(room.trim()) &&
                        parts[2].trim().equals(dateString.trim()) &&
                        parts[4].trim().equals(time.trim())) {
                        System.out.println("[예약체크] 대기 중 예약 발견: " + line);
                        return true;
                    }
                }
            } catch (IOException e) {
                throw new DatabaseException(
                        "ReservationRequest.txt",
                        DatabaseException.OperationType.READ,
                        "예약 정보를 읽는 중 오류가 발생했습니다",
                        e
                );
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
                    if (parts.length >= 9 &&
                        parts[1].trim().equals(room.trim()) &&
                        parts[2].trim().equals(dateString.trim()) &&
                        parts[4].trim().equals(time.trim())) {
                        
                        String status = parts[7].trim();
                        if (status.equals("예약됨") || status.equals("승인")) {
                            System.out.println("[예약체크] 승인된 예약 발견: " + line);
                            return true;
                        }
                    }
                }
            } catch (IOException e) {
                throw new DatabaseException(
                        new File(filePath).getName(),
                        DatabaseException.OperationType.READ,
                        "예약 정보를 읽는 중 오류가 발생했습니다",
                        e
                );
            }
        }

        return false;
    }
}
