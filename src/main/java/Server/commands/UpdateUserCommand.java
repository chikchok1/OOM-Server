/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Server.commands;

/**
 *
 * @author YangJinWon
 */
import Server.UserDAO;
import java.io.*;

public class UpdateUserCommand implements Command {
    private final String BASE_DIR;
    private final Object FILE_LOCK;
    private final UserDAO userDAO;
    private final String currentUserId; // 🔥 추가

    // 🔥 생성자에 currentUserId 추가
    public UpdateUserCommand(String baseDir, Object fileLock, UserDAO userDAO, String currentUserId) {
        this.BASE_DIR = baseDir;
        this.FILE_LOCK = fileLock;
        this.userDAO = userDAO;
        this.currentUserId = currentUserId;
    }

    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) throws IOException {
        if (params.length != 4) {
            System.err.println("[ERROR] UPDATE_USER 파라미터 개수 오류: " + params.length);
            return "INVALID_UPDATE_FORMAT";
        }

        // 🔥 수정: currentUserId로 권한 확인
        System.out.println("[DEBUG] UPDATE_USER - 권한 확인 userId: " + currentUserId);
        
        if (currentUserId == null || !userDAO.authorizeAccess(currentUserId)) {
            System.err.println("[ERROR] 권한 없음: " + currentUserId);
            return "ACCESS_DENIED";
        }

        String targetUserId = params[1].trim();
        String newName = params[2].trim();
        String newPw = params[3].trim();

        System.out.println("[DEBUG] 수정 대상: " + targetUserId + ", 새 이름: " + newName);

        synchronized (FILE_LOCK) {
            String oldName = userDAO.getUserNameById(targetUserId);
            boolean result = updateUserById(targetUserId, newName, newPw);

            if (result) {
                updateReservationFilesName(oldName, newName);
                return "UPDATE_SUCCESS";
            } else {
                return "UPDATE_FAILED";
            }
        }
    }

    private boolean updateUserById(String userId, String newName, String newPw) {
        String filePath = getUserFileById(userId);
        if (filePath == null) return false;

        File inputFile = new File(filePath);
        File tempFile = new File(filePath + ".tmp");

        boolean updated = false;
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 3 && parts[1].trim().equals(userId)) {
                    writer.write(newName + "," + userId + "," + newPw);
                    updated = true;
                    System.out.println("[DEBUG] 사용자 수정: " + line + " -> " + newName);
                } else {
                    writer.write(line);
                }
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        if (updated) {
            inputFile.delete();
            return tempFile.renameTo(inputFile);
        } else {
            tempFile.delete();
            return false;
        }
    }

    private void updateReservationFilesName(String oldName, String newName) {
        String[] reservationFiles = {
            BASE_DIR + "/ReserveClass.txt",
            BASE_DIR + "/ReserveLab.txt",
            BASE_DIR + "/ReservationRequest.txt",
            BASE_DIR + "/ApprovedBackUp.txt"
        };

        for (String filePath : reservationFiles) {
            File file = new File(filePath);
            if (!file.exists()) continue;

            File tempFile = new File(filePath + ".tmp");

            try (BufferedReader reader = new BufferedReader(new FileReader(file));
                 BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] tokens = line.split(",");
                    if (tokens.length >= 7 && tokens[0].trim().equals(oldName.trim())) {
                        tokens[0] = newName;
                        writer.write(String.join(",", tokens));
                    } else {
                        writer.write(line);
                    }
                    writer.newLine();
                }
            } catch (IOException e) {
                System.out.println("예약 파일 이름 업데이트 오류: " + e.getMessage());
                continue;
            }

            if (!file.delete() || !tempFile.renameTo(file)) {
                System.out.println("파일 교체 실패: " + filePath);
            }
        }
    }

    private String getUserFileById(String userId) {
        if (userId == null) return null;
        if (userId.startsWith("S")) return BASE_DIR + "/users.txt";
        if (userId.startsWith("P")) return BASE_DIR + "/prof.txt";
        if (userId.startsWith("A")) return BASE_DIR + "/assistant.txt";
        return null;
    }
}
