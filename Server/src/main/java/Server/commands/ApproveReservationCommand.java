package Server.commands;

import Server.UserDAO;
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
        if (params.length != 6) {
            System.err.println("[ERROR] APPROVE_RESERVATION 파라미터 개수 오류: " + params.length);
            return "INVALID_APPROVE_FORMAT";
        }

        System.out.println("[DEBUG] APPROVE_RESERVATION - 권한 확인 userId: " + currentUserId);
        
        if (currentUserId == null || !userDAO.authorizeAccess(currentUserId)) {
            System.err.println("[ERROR] 권한 없음: " + currentUserId);
            return "ACCESS_DENIED";
        }

        String id = params[1].trim();      
        String time = params[2].trim();
        String day = params[3].trim();
        String room = params[4].trim();
        String name2 = params[5].trim();

        System.out.println("[DEBUG] 승인 처리: 요청자=" + name2 + ", 방=" + room + ", 시간=" + time);

        synchronized (FILE_LOCK) {
            String 목적 = "", 권한 = "";
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

                        // ReservationRequest 처리
                        if (file.getName().equals("ReservationRequest.txt") && tokens.length >= 6 &&
                            tokens[0].trim().equals(name2.trim()) &&
                            tokens[1].trim().equals(room.trim()) &&
                            tokens[2].trim().equals(day.trim()) &&
                            tokens[3].trim().equals(time.trim())) {
                            목적 = tokens[4].trim();
                            권한 = tokens[5].trim();
                            
                            // 학생 수 추출
                            if (tokens.length >= 8) {
                                try {
                                    studentCount = Integer.parseInt(tokens[7].trim());
                                    System.out.println("[DEBUG] 학생 수 추출: " + studentCount + "명");
                                } catch (NumberFormatException e) {
                                    studentCount = 1;
                                    System.err.println("[WARN] 학생 수 파싱 실패, 기본값 1 사용");
                                }
                            } else {
                                studentCount = 1;
                                System.out.println("[DEBUG] 구 버전 데이터 (학생 수 없음), 기본값 1 사용");
                            }
                            found = true;
                            System.out.println("[DEBUG] ReservationRequest 찾음: " + line);
                            continue; 
                        }

                        // ChangeRequest 처리
                        if (file.getName().equals("ChangeRequest.txt") && tokens.length >= 10 &&
                            tokens[0].trim().equals(id.trim()) &&
                            tokens[1].trim().equals(time.trim()) &&
                            tokens[2].trim().equals(day.trim()) &&
                            tokens[3].trim().equals(room.trim()) &&
                            tokens[4].trim().equals(name2.trim())) {
                            목적 = tokens[5].trim();
                            권한 = tokens[6].trim();
                            originalTime = tokens[7].trim();
                            originalDay = tokens[8].trim();
                            originalRoom = tokens[9].trim();
                            
                            // ChangeRequest에도 학생 수 필드 추가 (필요시)
                            if (tokens.length >= 11) {
                                try {
                                    studentCount = Integer.parseInt(tokens[10].trim());
                                } catch (NumberFormatException e) {
                                    studentCount = 1;
                                }
                            } else {
                                studentCount = 1;
                            }
                            
                            isChangeRequest = true;
                            found = true;
                            System.out.println("[DEBUG] ChangeRequest 찾음: " + line);
                            continue;
                        }

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
                System.err.println("[ERROR] 승인할 요청을 찾을 수 없음");
                return "APPROVE_FAILED";
            }

            // ❌ 수용 인원 업데이트 제거 - 실시간 계산 방식 사용
            System.out.println("[승인] " + room + " " + day + " " + time + " - 학생 수: " + studentCount + "명");

            // 예약 파일에 추가
            int roomNumber = Integer.parseInt(room.replaceAll("[^0-9]", ""));
            String targetFile = (roomNumber == 908 || roomNumber == 912 || 
                                roomNumber == 913 || roomNumber == 914)
                    ? BASE_DIR + "/ReserveClass.txt" : BASE_DIR + "/ReserveLab.txt";

            // 변경 요청인 경우 기존 예약 삭제
            if (isChangeRequest) {
                deleteOriginalReservation(name2, originalRoom, originalDay, originalTime);
            }

            // 예약 정보 저장 (학생 수 포함)
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile, true))) {
                // 학생 수를 포함하여 저장
                writer.write(String.join(",", name2, room, day, time, 목적, 권한, "예약됨", String.valueOf(studentCount)));
                writer.newLine();
                System.out.println("[DEBUG] 예약 추가 완료 (학생수: " + studentCount + "명)");
            }

            // 백업 로그
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(BASE_DIR + "/ApprovedBackup.txt", true))) {
                writer.write(String.join(",", name2, room, day, time, 목적, 권한, "승인", String.valueOf(studentCount)));
                writer.newLine();
            }

            return "APPROVE_SUCCESS";
        }
    }

    private void deleteOriginalReservation(String name, String room, String day, String time) {
        String normalizedRoom = room.replace("호", "").trim();
        String targetFile = (normalizedRoom.equals("908") || normalizedRoom.equals("912") ||
                            normalizedRoom.equals("913") || normalizedRoom.equals("914"))
                ? BASE_DIR + "/ReserveClass.txt"
                : BASE_DIR + "/ReserveLab.txt";

        File inputFile = new File(targetFile);
        File tempFile = new File(targetFile + ".tmp");

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length >= 7) {
                    boolean match = tokens[0].trim().equals(name.trim()) &&
                                   tokens[1].replace("호", "").trim().equals(normalizedRoom) &&
                                   tokens[2].trim().equals(day.trim()) &&
                                   tokens[3].trim().equals(time.trim());

                    if (match) {
                        System.out.println("[DEBUG] 기존 예약 삭제: " + line);
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