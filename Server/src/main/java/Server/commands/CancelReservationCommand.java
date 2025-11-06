/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Server.commands;

/**
 *
 * @author YangJinWon
 */
import common.manager.ClassroomManager;
import java.io.*;

public class CancelReservationCommand implements Command {

    private final String BASE_DIR;
    private final Object FILE_LOCK;

    public CancelReservationCommand(String baseDir, Object fileLock) {
        this.BASE_DIR = baseDir;
        this.FILE_LOCK = fileLock;
    }

    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) throws IOException {
        if (params.length != 6) {
            return "INVALID_CANCEL_FORMAT";
        }

        String cancelUserId = params[1].trim();
        String time = params[2].trim();
        String day = params[3].trim();
        String room = params[4].trim();
        String userName = params[5].trim();

        synchronized (FILE_LOCK) {
            String targetFile = (room.equals("908호") || room.equals("912호")
                    || room.equals("913호") || room.equals("914호"))
                    ? BASE_DIR + "/ReserveClass.txt"
                    : BASE_DIR + "/ReserveLab.txt";

            File inputFile = new File(targetFile);
            File tempFile = new File(targetFile + ".tmp");
            boolean deleted = false;

            int canceledStudentCount = 0;  // 취소된 예약의 학생 수
            
            try (BufferedReader reader = new BufferedReader(new FileReader(inputFile)); BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] lineParts = line.split(",");
                    if (lineParts.length >= 7
                            && lineParts[0].trim().equals(userName.trim())
                            && lineParts[1].trim().equals(room.trim())
                            && lineParts[2].trim().equals(day.trim())
                            && lineParts[3].trim().equals(time.trim())) {
                        
                        // 학생 수 추출 (필드 7번째, 인덱스 7)
                        if (lineParts.length >= 8) {
                            try {
                                canceledStudentCount = Integer.parseInt(lineParts[7].trim());
                                System.out.println("[DEBUG] 취소할 예약의 학생 수: " + canceledStudentCount + "명");
                            } catch (NumberFormatException e) {
                                canceledStudentCount = 1;
                                System.err.println("[WARN] 학생 수 파싱 실패, 기본값 1 사용");
                            }
                        } else {
                            canceledStudentCount = 1;
                            System.out.println("[DEBUG] 구 버전 데이터, 기본값 1 사용");
                        }
                        
                        deleted = true;
                        System.out.println("[DEBUG] 예약 취소: " + line);
                        continue;
                    }
                    writer.write(line);
                    writer.newLine();
                }
            } catch (IOException e) {
                return "CANCEL_FAILED_IO_ERROR";
            }

            if (deleted) {
                inputFile.delete();
                tempFile.renameTo(inputFile);
                
                // ❌ 수용 인원 복구 제거 - 실시간 계산 방식 사용
                System.out.println("[취소] " + room + " " + day + " " + time + " - 학생 수: " + canceledStudentCount + "명");
                
                return "CANCEL_SUCCESS";
            } else {
                tempFile.delete();
                return "CANCEL_FAILED_NOT_FOUND";
            }
        }
    }
}
