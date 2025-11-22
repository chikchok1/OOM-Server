package Server.commands;

import Server.UserDAO;
import Server.exceptions.*;
import java.io.*;

public class UpdateUserCommand implements Command {
    private final String BASE_DIR;
    private final Object FILE_LOCK;
    private final UserDAO userDAO;
    private final String currentUserId;

    public UpdateUserCommand(String baseDir, Object fileLock, UserDAO userDAO, String currentUserId) {
        this.BASE_DIR = baseDir;
        this.FILE_LOCK = fileLock;
        this.userDAO = userDAO;
        this.currentUserId = currentUserId;
    }

    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) 
            throws IOException, InvalidInputException, DatabaseException, 
                   AuthenticationException, BusinessLogicException {
        
        // 입력 검증
        if (params.length != 4) {
            throw new InvalidInputException(
                    "UPDATE_USER 명령은 4개의 매개변수가 필요합니다 (현재: " + params.length + "개)"
            );
        }

        System.out.println("UPDATE_USER - 권한 확인 userId: " + currentUserId);
        
        // 권한 확인
        if (currentUserId == null || !userDAO.authorizeAccess(currentUserId)) {
            throw new AuthenticationException(
                    AuthenticationException.AuthFailureReason.INSUFFICIENT_PERMISSION,
                    "관리자 권한이 필요합니다"
            );
        }

        String targetUserId = params[1].trim();
        String newName = params[2].trim();
        String newPw = params[3].trim();

        // 입력 유효성 검사
        if (targetUserId.isEmpty()) {
            throw new InvalidInputException("targetUserId", targetUserId, 
                    "수정할 사용자 ID를 입력해주세요");
        }
        if (newName.isEmpty()) {
            throw new InvalidInputException("newName", newName, 
                    "새 이름을 입력해주세요");
        }
        if (newPw.isEmpty()) {
            throw new InvalidInputException("newPw", "", 
                    "새 비밀번호를 입력해주세요");
        }

        System.out.println("수정 대상: " + targetUserId + ", 새 이름: " + newName);

        synchronized (FILE_LOCK) {
            String oldName = userDAO.getUserNameById(targetUserId);
            if (oldName == null || oldName.isEmpty()) {
                throw new BusinessLogicException(
                        BusinessLogicException.BusinessRuleViolation.RESERVATION_NOT_FOUND,
                        "사용자를 찾을 수 없습니다: " + targetUserId
                );
            }

            // 사용자 정보 업데이트
            updateUserById(targetUserId, newName, newPw);
            
            // 예약 파일의 이름도 업데이트
            updateReservationFilesName(oldName, newName);
            
            System.out.println("사용자 수정 완료: " + targetUserId);
            return "UPDATE_SUCCESS";
        }
    }

    private void updateUserById(String userId, String newName, String newPw) 
            throws DatabaseException, BusinessLogicException, InvalidInputException {
        String filePath = getUserFileById(userId);
        if (filePath == null) {
            throw new InvalidInputException("userId", userId, 
                    "유효하지 않은 사용자 ID 형식입니다");
        }

        File inputFile = new File(filePath);
        if (!inputFile.exists()) {
            throw new DatabaseException(
                    inputFile.getName(),
                    DatabaseException.OperationType.READ,
                    "사용자 파일을 찾을 수 없습니다"
            );
        }

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
                    System.out.println("사용자 수정: " + line + " -> " + newName);
                } else {
                    writer.write(line);
                }
                writer.newLine();
            }
        } catch (IOException e) {
            // 임시 파일 정리
            if (tempFile.exists()) {
                tempFile.delete();
            }
            throw new DatabaseException(
                    inputFile.getName(),
                    DatabaseException.OperationType.UPDATE,
                    "사용자 정보 수정 중 오류가 발생했습니다",
                    e
            );
        }

        if (!updated) {
            tempFile.delete();
            throw new BusinessLogicException(
                    BusinessLogicException.BusinessRuleViolation.RESERVATION_NOT_FOUND,
                    "수정할 사용자를 찾을 수 없습니다: " + userId
            );
        }

        // 파일 교체
        if (!inputFile.delete()) {
            tempFile.delete();
            throw new DatabaseException(
                    inputFile.getName(),
                    DatabaseException.OperationType.DELETE,
                    "기존 파일을 삭제할 수 없습니다"
            );
        }

        if (!tempFile.renameTo(inputFile)) {
            throw new DatabaseException(
                    tempFile.getName(),
                    DatabaseException.OperationType.UPDATE,
                    "파일 업데이트에 실패했습니다"
            );
        }
    }

    private void updateReservationFilesName(String oldName, String newName) 
            throws DatabaseException {
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
                System.err.println("예약 파일 이름 업데이트 오류: " + e.getMessage());
                if (tempFile.exists()) {
                    tempFile.delete();
                }
                // 예약 파일 업데이트 실패는 치명적이지 않으므로 계속 진행
                continue;
            }

            // 파일 교체
            if (!file.delete() || !tempFile.renameTo(file)) {
                System.err.println("파일 교체 실패: " + filePath);
                if (tempFile.exists()) {
                    tempFile.delete();
                }
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
