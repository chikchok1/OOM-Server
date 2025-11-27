/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Server.commands;

/**
 *
 * @author YangJinWon
 */
import java.io.*;
import Server.exceptions.*;
public class ChangeReservationCommand implements Command {
    private final String BASE_DIR;
    private final Object FILE_LOCK;

    public ChangeReservationCommand(String baseDir, Object fileLock) {
        this.BASE_DIR = baseDir;
        this.FILE_LOCK = fileLock;
    }

    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) throws IOException, InvalidInputException, DatabaseException, AuthenticationException, BusinessLogicException {
        System.out.println("[DEBUG] CHANGE_RESERVATION 요청 진입");

        // 변경 인원을 포함하면 11개 파라미터가 필요함 (COMMAND 포함)
        if (params.length != 11) {
            System.out.println("[ERROR] CHANGE_RESERVATION 파라미터 부족 (" + params.length + ")");
            return "INVALID_CHANGE_FORMAT";
        }

        String changeUserId = params[1].trim();
        String originalTime = params[2].trim();
        String originalDate = params[3].trim();  // 원래 날짜 추가
        String originalDay = params[4].trim();
        String originalRoom = params[5].trim();
        String newTime = params[6].trim();
        String newDate = params[7].trim();        // 새 날짜 추가
        String newDay = params[8].trim();
        String newRoom = params[9].trim();
        String userName = params[10].trim();
        String changeNumber = params[11].trim(); // 변경 인원

        String purpose = "";
        String role = "";
        String filePath = (originalRoom.equals("908호") || originalRoom.equals("912호") ||
                          originalRoom.equals("913호") || originalRoom.equals("914호"))
                ? BASE_DIR + "/ReserveClass.txt" : BASE_DIR + "/ReserveLab.txt";

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                // 이름,방,날짜,요일,시간,목적,권한,상태,학생수,아이디 (10개 필드)
                if (tokens.length >= 10 &&
                    tokens[0].trim().equals(userName.trim()) &&
                    tokens[1].trim().equals(originalRoom.trim()) &&
                    tokens[2].trim().equals(originalDate.trim()) &&
                    tokens[3].trim().equals(originalDay.trim()) &&
                    tokens[4].trim().equals(originalTime.trim())) {
                    purpose = tokens[5].trim();
                    role = tokens[6].trim();
                    break;
                }
            }
        } catch (IOException e) {
            return "CHANGE_FAILED_LOOKUP";
        }

        synchronized (FILE_LOCK) {
            boolean duplicate = false;
            File[] reserveFiles = {
                new File(BASE_DIR + "/ReserveClass.txt"),
                new File(BASE_DIR + "/ReserveLab.txt")
            };

            for (File file : reserveFiles) {
                if (!file.exists()) continue;

                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] tokens = line.split(",");
                        // 이름,방,날짜,요일,시간,목적,권한,상태,학생수,아이디
                        if (tokens.length >= 10 &&
                            tokens[1].trim().equals(newRoom.trim()) &&
                            tokens[2].trim().equals(newDate.trim()) &&
                            tokens[3].trim().equals(newDay.trim()) &&
                            tokens[4].trim().equals(newTime.trim()) &&
                            tokens[7].trim().equals("예약됨")) {
                            duplicate = true;
                            break;
                        }
                    }
                } catch (IOException e) {
                    return "CHANGE_FAILED_IO";
                }
                if (duplicate) break;
            }

            if (!duplicate) {
                File changeFile = new File(BASE_DIR + "/ChangeRequest.txt");
                if (changeFile.exists()) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(changeFile))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String[] tokens = line.split(",");
                            // ID,시간,날짜,요일,방,이름,목적,권한,원시간,원날짜,원요일,원방,인원
                            if (tokens.length >= 13 &&
                                tokens[0].trim().equals(changeUserId.trim()) &&
                                tokens[1].trim().equals(newTime.trim()) &&
                                tokens[2].trim().equals(newDate.trim()) &&
                                tokens[3].trim().equals(newDay.trim()) &&
                                tokens[4].trim().equals(newRoom.trim())) {
                                duplicate = true;
                                break;
                            }
                        }
                    } catch (IOException e) {
                        return "CHANGE_FAILED_IO";
                    }
                }
            }

            if (duplicate) {
                return "CHANGE_DUPLICATE_REQUEST";
            }

            File changeFile = new File(BASE_DIR + "/ChangeRequest.txt");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(changeFile, true))) {
                // ChangeRequest 포맷: ID,시간,날짜,요일,방,이름,목적,권한,원시간,원날짜,원요일,원방,인원 (13개 필드)
                writer.write(String.join(",", 
                    changeUserId,      // 0: ID
                    newTime,           // 1: 새 시간
                    newDate,           // 2: 새 날짜
                    newDay,            // 3: 새 요일
                    newRoom,           // 4: 새 방
                    userName,          // 5: 이름
                    purpose,           // 6: 목적
                    role,              // 7: 권한
                    originalTime,      // 8: 원래 시간
                    originalDate,      // 9: 원래 날짜
                    originalDay,       // 10: 원래 요일
                    originalRoom,      // 11: 원래 방
                    changeNumber       // 12: 인원
                ));
                writer.newLine();
                System.out.println("[DEBUG] CHANGE_RESERVATION 저장 완료 (인원: " + changeNumber + ")");
            } catch (IOException e) {
                System.out.println("[ERROR] CHANGE_RESERVATION 저장 실패: " + e.getMessage());
                return "CHANGE_FAILED_WRITE";
            }

            return "CHANGE_SUCCESS";
        }
    }
}

