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

                System.out.println("[ChangeReservationFull] 요청: oldType=" + oldFileType + ", newType=" + newFileType + ", userId=" + userId + ", userName=" + userName + ", 기존=" + oldRoom + " " + oldDate + " " + oldTime);

                // 1단계: 기존 예약 찾아서 삭제 (기존 타입의 파일에서)
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

                boolean deletedFromReservations = deleteMatchingReservation(oldReservationFile, userId, userName, oldRoom, oldDate, oldDay, oldTime);
                boolean deletedFromRequests = deleteMatchingReservation(requestFile, userId, userName, oldRoom, oldDate, oldDay, oldTime);

                if (!deletedFromReservations && !deletedFromRequests) {
                    System.err.println("[ChangeReservationFull] 기존 예약을 찾을 수 없음");
                    out.println("CHANGE_FAILED");
                    out.flush();
                    return "CHANGE_FAILED";
                }

                System.out.println("[ChangeReservationFull] 기존 예약 삭제 완료");

                // 2단계: 새 예약 추가 (대기 상태로)
                String[] newReservations = newReservationsStr.split(";");
                int addedCount = 0;

                for (String newRes : newReservations) {
                    if (newRes.trim().isEmpty()) continue;

                    String[] parts = newRes.split("\\|");
                    if (parts.length < 7) {
                        System.err.println("[ChangeReservationFull] 잘못된 예약 형식: " + newRes);
                        continue;
                    }

                    String newRoom = parts[0].trim();
                    String newDate = parts[1].trim();
                    String newDay = parts[2].trim();
                    String newTime = parts[3].trim();
                    String purpose = parts[4].trim();
                    String role = parts[5].trim();
                    String studentCount = parts[6].trim();

                    // 새 예약을 ReservationRequest.txt에 대기 상태로 추가
                    // (새 예약은 조교 승인 후 ReserveClass.txt 또는 ReserveLab.txt로 이동)
                    // ✅ 형식: name,room,date,day,time,purpose,role,status,studentCount,userId
                    String newReservationLine = String.join(",",
                            userName, newRoom, newDate, newDay, newTime, purpose, role, "대기", studentCount, userId
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
                    out.println("CHANGE_FAILED");
                    out.flush();
                    return "CHANGE_FAILED";
                }

            } catch (IOException e) {
                System.err.println("[ChangeReservationFull] 오류: " + e.getMessage());
                e.printStackTrace();
                out.println("CHANGE_FAILED");
                out.flush();
                return "CHANGE_FAILED";
            }
        }
    }

    /**
     * 해당 예약을 파일에서 찾아서 삭제
     * @param file 예약 파일 (ReserveClass.txt, ReserveLab.txt, 또는 ReservationRequest.txt)
     */
    private boolean deleteMatchingReservation(File file, String userId, String userName, String room, String date, String day, String time) throws IOException {
        if (!file.exists()) {
            return false;
        }

        List<String> lines = new ArrayList<>();
        boolean found = false;

        // 파일 읽기
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                // ✅ 형식: name,room,date,day,time,purpose,role,status,studentCount,userId
                if (parts.length >= 10) {
                    String lineName = parts[0].trim();
                    String lineRoom = parts[1].trim();
                    String lineDate = parts[2].trim();
                    String lineDay = parts[3].trim();
                    String lineTime = parts[4].trim();
                    String lineUserId = parts[9].trim();
                    
                    // 시간 비교 시 교시 부분만 추출
                    String timeToCompare = time;
                    String lineTimeToCompare = lineTime;
                    if (lineTime.contains("(")) {
                        lineTimeToCompare = lineTime.substring(0, lineTime.indexOf("(")).trim();
                    }
                    if (time.contains("(")) {
                        timeToCompare = time.substring(0, time.indexOf("(")).trim();
                    }

                    // 일치하는 예약 찾기 (userId와 name 모두 확인)
                    if ((lineUserId.equals(userId) || lineName.equals(userName)) &&
                        lineRoom.equals(room) &&
                        lineDate.equals(date) &&
                        lineDay.equals(day) &&
                        lineTimeToCompare.equals(timeToCompare)) {
                        found = true;
                        System.out.println("[deleteMatchingReservation] 찾음: " + line);
                        continue; // 이 줄은 추가하지 않음 (삭제)
                    }
                }
                // ✅ 구 형식 (9개 필드) 지원
                else if (parts.length >= 9) {
                    String lineName = parts[0].trim();
                    String lineRoom = parts[1].trim();
                    String lineDate = parts[2].trim();
                    String lineDay = parts[3].trim();
                    String lineTime = parts[4].trim();
                    
                    String timeToCompare = time;
                    String lineTimeToCompare = lineTime;
                    if (lineTime.contains("(")) {
                        lineTimeToCompare = lineTime.substring(0, lineTime.indexOf("(")).trim();
                    }
                    if (time.contains("(")) {
                        timeToCompare = time.substring(0, time.indexOf("(")).trim();
                    }
                    
                    if (lineName.equals(userName) &&
                        lineRoom.equals(room) &&
                        lineDate.equals(date) &&
                        lineDay.equals(day) &&
                        lineTimeToCompare.equals(timeToCompare)) {
                        found = true;
                        System.out.println("[deleteMatchingReservation] 찾음 (구 형식): " + line);
                        continue;
                    }
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
            System.out.println("[deleteMatchingReservation] " + file.getName() + "에서 삭제 완료");
        }

        return found;
    }
}
