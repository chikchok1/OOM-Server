package Server.commands;

import java.io.*;
import java.util.*;

public class ChangeReservationFullCommand implements Command {

    private final String BASE_DIR;
    private final Object FILE_LOCK;

    public ChangeReservationFullCommand(String baseDir, Object fileLock) {
        this.BASE_DIR = baseDir;
        this.FILE_LOCK = fileLock;
    }

    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) throws IOException {
        synchronized (FILE_LOCK) {
            // 백업용 변수 (롤백을 위해)
            String deletedReservation = null;
            File deletedFromFile = null;
            
            try {
                // 요청 파싱
                // FORMAT: CHANGE_RESERVATION_FULL,oldFileType,newFileType,userId,userName,oldRoom,oldDate,oldDay,oldTime,newRoom1|newDate1|...
                String[] mainParts = params;
                if (mainParts.length < 10) {
                    System.err.println("[ChangeReservationFull] 잘못된 요청 형식: " + String.join(",", params));
                    out.println("CHANGE_FAILED");
                    out.flush();
                    return "CHANGE_FAILED";
                }

                String oldFileType = params[1].trim(); // 기존 예약 파일 타입 ("CLASS" 또는 "LAB")
                String newFileType = params[2].trim(); // 새 예약 파일 타입 ("CLASS" 또는 "LAB")
                String userId = params[3].trim();
                String userName = params[4].trim();
                String oldRoom = params[5].trim();
                String oldDate = params[6].trim();
                String oldDay = params[7].trim();
                String oldTime = params[8].trim();
                String newReservationsStr = params[9].trim();

                System.out.println("[ChangeReservationFull] 요청: oldType=" + oldFileType + ", newType=" + newFileType + 
                                   ", userId=" + userId + ", userName=" + userName + 
                                   ", 기존=" + oldRoom + " " + oldDate + " " + oldTime);

                // ===== 1단계: 새 예약 검증 (중복 체크) =====
                String[] newReservations = newReservationsStr.split(";");
                List<NewReservationInfo> validatedReservations = new ArrayList<>();
                
                for (String newRes : newReservations) {
                    if (newRes.trim().isEmpty()) continue;

                    String[] parts = newRes.split("\\|");
                    if (parts.length < 7) {
                        System.err.println("[ChangeReservationFull] 잘못된 예약 형식: " + newRes);
                        out.println("CHANGE_FAILED_INVALID_FORMAT");
                        out.flush();
                        return "CHANGE_FAILED_INVALID_FORMAT";
                    }

                    String newRoom = parts[0].trim();
                    String newDate = parts[1].trim();
                    String newDay = parts[2].trim();
                    String newTime = parts[3].trim();
                    String purpose = parts[4].trim();
                    String role = parts[5].trim();
                    String studentCount = parts[6].trim();

                    // ✅ 중복 예약 체크
                    if (isTimeSlotReserved(newRoom, newDate, newTime)) {
                        System.err.println("[ChangeReservationFull] 중복 예약 발견: " + newRoom + " " + newDate + " " + newTime);
                        out.println("CHANGE_FAILED_CONFLICT:" + newTime);
                        out.flush();
                        return "CHANGE_FAILED_CONFLICT:" + newTime;
                    }

                    // 검증 통과한 예약 저장
                    validatedReservations.add(new NewReservationInfo(
                        newRoom, newDate, newDay, newTime, purpose, role, studentCount
                    ));
                }

                if (validatedReservations.isEmpty()) {
                    System.err.println("[ChangeReservationFull] 유효한 새 예약이 없음");
                    out.println("CHANGE_FAILED_NO_VALID_RESERVATION");
                    out.flush();
                    return "CHANGE_FAILED_NO_VALID_RESERVATION";
                }

                System.out.println("[ChangeReservationFull] 새 예약 검증 완료: " + validatedReservations.size() + "개");

                // ===== 2단계: 기존 예약 찾기 및 백업 =====
                File oldReservationFile;
                if ("CLASS".equals(oldFileType)) {
                    oldReservationFile = new File(BASE_DIR + "/ReserveClass.txt");
                } else if ("LAB".equals(oldFileType)) {
                    oldReservationFile = new File(BASE_DIR + "/ReserveLab.txt");
                } else {
                    System.err.println("[ChangeReservationFull] 잘못된 기존 파일 타입: " + oldFileType);
                    out.println("CHANGE_FAILED");
                    out.flush();
                    return "CHANGE_FAILED";
                }
                
                File requestFile = new File(BASE_DIR + "/ReservationRequest.txt");

                // 기존 예약 찾기 및 백업
                ReservationBackup backup = findAndBackupReservation(
                    oldReservationFile, requestFile, userId, userName, oldRoom, oldDate, oldDay, oldTime
                );

                if (backup.deletedLine == null) {
                    System.err.println("[ChangeReservationFull] 기존 예약을 찾을 수 없음");
                    out.println("CHANGE_FAILED_NOT_FOUND");
                    out.flush();
                    return "CHANGE_FAILED_NOT_FOUND";
                }

                deletedReservation = backup.deletedLine;
                deletedFromFile = backup.sourceFile;

                System.out.println("[ChangeReservationFull] 기존 예약 백업 완료: " + deletedReservation);

                // ===== 3단계: 기존 예약 삭제 =====
                boolean deleteSuccess = deleteReservationFromFile(
                    backup.sourceFile, userId, userName, oldRoom, oldDate, oldDay, oldTime
                );

                if (!deleteSuccess) {
                    System.err.println("[ChangeReservationFull] 기존 예약 삭제 실패");
                    out.println("CHANGE_FAILED");
                    out.flush();
                    return "CHANGE_FAILED";
                }

                System.out.println("[ChangeReservationFull] 기존 예약 삭제 완료");

                // ===== 4단계: 새 예약 추가 (대기 상태로) =====
                try {
                    int addedCount = 0;
                    for (NewReservationInfo newResInfo : validatedReservations) {
                        // ✅ 형식: name,room,date,day,time,purpose,role,status,studentCount,userId
                        String newReservationLine = String.join(",",
                                userName, 
                                newResInfo.room, 
                                newResInfo.date, 
                                newResInfo.day, 
                                newResInfo.time, 
                                newResInfo.purpose, 
                                newResInfo.role, 
                                "대기중", 
                                newResInfo.studentCount, 
                                userId
                        );

                        try (FileWriter fw = new FileWriter(requestFile, true);
                             PrintWriter pw = new PrintWriter(fw)) {
                            pw.println(newReservationLine);
                            addedCount++;
                            System.out.println("[ChangeReservationFull] 새 예약 추가: " + newReservationLine);
                        }
                    }

                    if (addedCount > 0) {
                        out.println("CHANGE_SUCCESS");
                        out.flush();
                        System.out.println("[ChangeReservationFull] 변경 완료: " + addedCount + "개 새 예약 생성");
                        return "CHANGE_SUCCESS";
                    } else {
                        // 새 예약 추가 실패 - 롤백 필요
                        throw new IOException("새 예약 추가 실패");
                    }
                    
                } catch (IOException e) {
                    // ===== 롤백: 삭제된 예약 복구 =====
                    System.err.println("[ChangeReservationFull] 새 예약 추가 실패, 롤백 시작: " + e.getMessage());
                    
                    if (deletedReservation != null && deletedFromFile != null) {
                        try {
                            restoreReservation(deletedFromFile, deletedReservation);
                            System.out.println("[ChangeReservationFull] 롤백 완료: 기존 예약 복구됨");
                        } catch (IOException rollbackEx) {
                            System.err.println("[ChangeReservationFull] 롤백 실패: " + rollbackEx.getMessage());
                            // 심각한 오류 - 관리자 개입 필요
                        }
                    }
                    
                    out.println("CHANGE_FAILED");
                    out.flush();
                    return "CHANGE_FAILED";
                }

            } catch (Exception e) {
                System.err.println("[ChangeReservationFull] 예외 발생: " + e.getMessage());
                e.printStackTrace();
                
                // ===== 예외 발생 시 롤백 =====
                if (deletedReservation != null && deletedFromFile != null) {
                    try {
                        restoreReservation(deletedFromFile, deletedReservation);
                        System.out.println("[ChangeReservationFull] 예외 후 롤백 완료");
                    } catch (IOException rollbackEx) {
                        System.err.println("[ChangeReservationFull] 예외 후 롤백 실패: " + rollbackEx.getMessage());
                    }
                }
                
                out.println("CHANGE_FAILED");
                out.flush();
                return "CHANGE_FAILED";
            }
        }
    }

    /**
     * ✅ 특정 날짜/시간대에 이미 예약이 존재하는지 체크 (중복 예약 방지)
     */
    private boolean isTimeSlotReserved(String room, String date, String time) {
        // 시간 정규화 (괄호 제거)
        String normalizedTime = time;
        if (time.contains("(")) {
            normalizedTime = time.substring(0, time.indexOf("(")).trim();
        }

        // 1. 대기 중인 예약 체크
        File pendingFile = new File(BASE_DIR + "/ReservationRequest.txt");
        if (pendingFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(pendingFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 9) {
                        String lineRoom = parts[1].trim();
                        String lineDate = parts[2].trim();
                        String lineTime = parts[4].trim();
                        
                        // 시간 정규화
                        if (lineTime.contains("(")) {
                            lineTime = lineTime.substring(0, lineTime.indexOf("(")).trim();
                        }
                        
                        if (lineRoom.equals(room) && lineDate.equals(date) && lineTime.equals(normalizedTime)) {
                            System.out.println("[중복체크] 대기 중 예약 발견: " + line);
                            return true;
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("[중복체크] 파일 읽기 오류: " + e.getMessage());
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
                    if (parts.length >= 9) {
                        String lineRoom = parts[1].trim();
                        String lineDate = parts[2].trim();
                        String lineTime = parts[4].trim();
                        String status = parts[7].trim();
                        
                        // 시간 정규화
                        if (lineTime.contains("(")) {
                            lineTime = lineTime.substring(0, lineTime.indexOf("(")).trim();
                        }
                        
                        if (lineRoom.equals(room) && lineDate.equals(date) && lineTime.equals(normalizedTime)) {
                            if (status.equals("예약됨") || status.equals("승인")) {
                                System.out.println("[중복체크] 승인된 예약 발견: " + line);
                                return true;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("[중복체크] 파일 읽기 오류: " + filePath);
            }
        }

        return false;
    }

    /**
     * ✅ 기존 예약 찾기 및 백업
     */
    private ReservationBackup findAndBackupReservation(
            File reservationFile, File requestFile, 
            String userId, String userName, String room, String date, String day, String time) throws IOException {
        
        // 먼저 ReservationFile에서 찾기
        String backup = findReservationLine(reservationFile, userId, userName, room, date, day, time);
        if (backup != null) {
            return new ReservationBackup(backup, reservationFile);
        }
        
        // 없으면 RequestFile에서 찾기
        backup = findReservationLine(requestFile, userId, userName, room, date, day, time);
        if (backup != null) {
            return new ReservationBackup(backup, requestFile);
        }
        
        return new ReservationBackup(null, null);
    }

    /**
     * ✅ 파일에서 예약 라인 찾기 (백업용)
     */
    private String findReservationLine(File file, String userId, String userName, 
                                      String room, String date, String day, String time) throws IOException {
        if (!file.exists()) {
            return null;
        }

        // 시간 정규화
        String normalizedTime = time;
        if (time.contains("(")) {
            normalizedTime = time.substring(0, time.indexOf("(")).trim();
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                
                // 새 형식 (10개 필드)
                if (parts.length >= 10) {
                    String lineName = parts[0].trim();
                    String lineRoom = parts[1].trim();
                    String lineDate = parts[2].trim();
                    String lineDay = parts[3].trim();
                    String lineTime = parts[4].trim();
                    String lineUserId = parts[9].trim();
                    
                    if (lineTime.contains("(")) {
                        lineTime = lineTime.substring(0, lineTime.indexOf("(")).trim();
                    }

                    if ((lineUserId.equals(userId) || lineName.equals(userName)) &&
                        lineRoom.equals(room) && lineDate.equals(date) && 
                        lineDay.equals(day) && lineTime.equals(normalizedTime)) {
                        return line;
                    }
                }
                // 구 형식 (9개 필드)
                else if (parts.length >= 9) {
                    String lineName = parts[0].trim();
                    String lineRoom = parts[1].trim();
                    String lineDate = parts[2].trim();
                    String lineDay = parts[3].trim();
                    String lineTime = parts[4].trim();
                    
                    if (lineTime.contains("(")) {
                        lineTime = lineTime.substring(0, lineTime.indexOf("(")).trim();
                    }
                    
                    if (lineName.equals(userName) && lineRoom.equals(room) && 
                        lineDate.equals(date) && lineDay.equals(day) && lineTime.equals(normalizedTime)) {
                        return line;
                    }
                }
            }
        }

        return null;
    }

    /**
     * ✅ 파일에서 예약 삭제
     */
    private boolean deleteReservationFromFile(File file, String userId, String userName, 
                                             String room, String date, String day, String time) throws IOException {
        if (!file.exists()) {
            return false;
        }

        List<String> lines = new ArrayList<>();
        boolean found = false;

        // 시간 정규화
        String normalizedTime = time;
        if (time.contains("(")) {
            normalizedTime = time.substring(0, time.indexOf("(")).trim();
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                boolean isMatch = false;
                
                // 새 형식 (10개 필드)
                if (parts.length >= 10) {
                    String lineName = parts[0].trim();
                    String lineRoom = parts[1].trim();
                    String lineDate = parts[2].trim();
                    String lineDay = parts[3].trim();
                    String lineTime = parts[4].trim();
                    String lineUserId = parts[9].trim();
                    
                    if (lineTime.contains("(")) {
                        lineTime = lineTime.substring(0, lineTime.indexOf("(")).trim();
                    }

                    isMatch = (lineUserId.equals(userId) || lineName.equals(userName)) &&
                              lineRoom.equals(room) && lineDate.equals(date) && 
                              lineDay.equals(day) && lineTime.equals(normalizedTime);
                }
                // 구 형식 (9개 필드)
                else if (parts.length >= 9) {
                    String lineName = parts[0].trim();
                    String lineRoom = parts[1].trim();
                    String lineDate = parts[2].trim();
                    String lineDay = parts[3].trim();
                    String lineTime = parts[4].trim();
                    
                    if (lineTime.contains("(")) {
                        lineTime = lineTime.substring(0, lineTime.indexOf("(")).trim();
                    }
                    
                    isMatch = lineName.equals(userName) && lineRoom.equals(room) && 
                              lineDate.equals(date) && lineDay.equals(day) && lineTime.equals(normalizedTime);
                }
                
                if (isMatch) {
                    found = true;
                    System.out.println("[deleteReservationFromFile] 삭제: " + line);
                    continue; // 삭제 (추가하지 않음)
                }
                
                lines.add(line);
            }
        }

        // 파일 쓰기
        if (found) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                for (String line : lines) {
                    writer.println(line);
                }
            }
            System.out.println("[deleteReservationFromFile] " + file.getName() + "에서 삭제 완료");
        }

        return found;
    }

    /**
     * ✅ 삭제된 예약 복구 (롤백)
     */
    private void restoreReservation(File file, String reservationLine) throws IOException {
        try (FileWriter fw = new FileWriter(file, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(reservationLine);
            System.out.println("[restoreReservation] 예약 복구: " + reservationLine);
        }
    }

    /**
     * 새 예약 정보 저장용 내부 클래스
     */
    private static class NewReservationInfo {
        String room;
        String date;
        String day;
        String time;
        String purpose;
        String role;
        String studentCount;

        NewReservationInfo(String room, String date, String day, String time, 
                          String purpose, String role, String studentCount) {
            this.room = room;
            this.date = date;
            this.day = day;
            this.time = time;
            this.purpose = purpose;
            this.role = role;
            this.studentCount = studentCount;
        }
    }

    /**
     * 예약 백업 정보 저장용 내부 클래스
     */
    private static class ReservationBackup {
        String deletedLine;
        File sourceFile;

        ReservationBackup(String deletedLine, File sourceFile) {
            this.deletedLine = deletedLine;
            this.sourceFile = sourceFile;
        }
    }
}
