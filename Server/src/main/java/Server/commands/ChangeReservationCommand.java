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

public class ChangeReservationCommand implements Command {
    private final String BASE_DIR;
    private final Object FILE_LOCK;

    public ChangeReservationCommand(String baseDir, Object fileLock) {
        this.BASE_DIR = baseDir;
        this.FILE_LOCK = fileLock;
    }

    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) throws IOException {
        System.out.println("[DEBUG] CHANGE_RESERVATION 요청 진입");

        if (params.length != 9) {
            System.out.println("[ERROR] CHANGE_RESERVATION 파라미터 부족 (" + params.length + ")");
            return "INVALID_CHANGE_FORMAT";
        }

        String changeUserId = params[1].trim();
        String originalTime = params[2].trim();
        String originalDay = params[3].trim();
        String originalRoom = params[4].trim();
        String newTime = params[5].trim();
        String newDay = params[6].trim();
        String newRoom = params[7].trim();
        String userName = params[8].trim();

        String purpose = "";
        String role = "";
        String filePath = (originalRoom.equals("908호") || originalRoom.equals("912호") ||
                          originalRoom.equals("913호") || originalRoom.equals("914호"))
                ? BASE_DIR + "/ReserveClass.txt" : BASE_DIR + "/ReserveLab.txt";

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length >= 7 &&
                    tokens[0].trim().equals(userName.trim()) &&
                    tokens[1].trim().equals(originalRoom.trim()) &&
                    tokens[2].trim().equals(originalDay.trim()) &&
                    tokens[3].trim().equals(originalTime.trim())) {
                    purpose = tokens[4].trim();
                    role = tokens[5].trim();
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
                        if (tokens.length >= 7 &&
                            tokens[1].trim().equals(newRoom.trim()) &&
                            tokens[2].trim().equals(newDay.trim()) &&
                            tokens[3].trim().equals(newTime.trim()) &&
                            tokens[6].trim().equals("예약됨")) {
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
                            if (tokens.length >= 5 &&
                                tokens[0].trim().equals(changeUserId.trim()) &&
                                tokens[1].trim().equals(newTime.trim()) &&
                                tokens[2].trim().equals(newDay.trim()) &&
                                tokens[3].trim().equals(newRoom.trim())) {
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
                writer.write(String.join(",", changeUserId, newTime, newDay, newRoom, userName, 
                            purpose, role, originalTime, originalDay, originalRoom));
                writer.newLine();
                System.out.println("[DEBUG] CHANGE_RESERVATION 저장 완료");
            } catch (IOException e) {
                System.out.println("[ERROR] CHANGE_RESERVATION 저장 실패: " + e.getMessage());
                return "CHANGE_FAILED_WRITE";
            }

            return "CHANGE_SUCCESS";
        }
    }
}

