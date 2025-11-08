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
        // 파라미터 개수 체크 (8개: 학생수 추가)
        System.out.println("[ReserveRequest] 받은 파라미터 개수: " + params.length);
        if (params.length != 8) {
            System.err.println("[ReserveRequest] 파라미터 개수 오류: " + params.length + ", 기대값: 8");
            return "INVALID_RESERVE_FORMAT";
        }

        String reserveName = params[1];
        String room = params[2];
        String day = params[3];
        String time = params[4];
        String purpose = params[5];
        String role = params[6];
        int studentCount;
        
        // 학생 수 파싱
        try {
            studentCount = Integer.parseInt(params[7]);
            System.out.println(String.format("[ReserveRequest] 예약 요청: %s, %s, %s, %s, 인원: %d명",
                reserveName, room, day, time, studentCount));
        } catch (NumberFormatException e) {
            System.err.println("[ReserveRequest] 학생 수 파싱 오류: " + params[7]);
            return "INVALID_STUDENT_COUNT";
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
            
            //  해당 시간대에 이미 예약이 존재하는지 체크 
            boolean isAlreadyReserved = isTimeSlotReserved(room, day, time);
            
            if (isAlreadyReserved) {
                System.out.println(String.format(
                    "[ReserveRequest] 이미 예약된 시간: %s %s %s",
                    room, day, time
                ));
                return "RESERVE_CONFLICT";
            }
            
            System.out.println(String.format(
                "[ReserveRequest] 예약 가능: %s %s %s - 요청 인원: %d명",
                room, day, time, studentCount
            ));

            File file = new File(BASE_DIR + "/ReservationRequest.txt");
            file.getParentFile().mkdirs();

            // 예약 저장
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                // 학생 수 포함하여 저장
                String data = String.format("%s,%s,%s,%s,%s,%s,대기,%d",
                    reserveName, room, day, time, purpose, role, studentCount);
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
     * 특정 시간대에 이미 예약이 존재하는지 체크 (ON/OFF 방식)
     * 대기 중이거나 승인된 예약이 있으면 true 반환
     */
    private boolean isTimeSlotReserved(String room, String day, String time) {
        // 1. 대기 중인 예약 체크
        File pendingFile = new File(BASE_DIR + "/ReservationRequest.txt");
        if (pendingFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(pendingFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 4 &&
                        parts[1].trim().equals(room.trim()) &&
                        parts[2].trim().equals(day.trim()) &&
                        parts[3].trim().equals(time.trim())) {
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
                    if (parts.length >= 7 &&
                        parts[1].trim().equals(room.trim()) &&
                        parts[2].trim().equals(day.trim()) &&
                        parts[3].trim().equals(time.trim())) {
                        
                        String status = parts[6].trim();
                        if (status.equals("예약됨") || status.equals("승인")) {
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