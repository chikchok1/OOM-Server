package Server.commands;

import Server.UserDAO;
import Server.exceptions.*;
import java.io.*;

/**
 * 사용자 삭제 명령 (Exception Handling 적용)
 */
public class DeleteUserCommand implements Command {

    private final String BASE_DIR;
    private final Object FILE_LOCK;
    private final UserDAO userDAO;
    private final String currentUserId;

    public DeleteUserCommand(String baseDir, Object fileLock, UserDAO userDAO, String currentUserId) {
        this.BASE_DIR = baseDir;
        this.FILE_LOCK = fileLock;
        this.userDAO = userDAO;
        this.currentUserId = currentUserId;
    }

    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) 
            throws IOException, InvalidInputException, DatabaseException, 
                   AuthenticationException, BusinessLogicException {
        
        // 1. 입력 검증
        validateInput(params);

        System.out.println("DELETE_USER - 권한 확인 userId: " + currentUserId);

        // 2. 권한 확인
        checkPermission();

        String targetUserId = params[1].trim();
        System.out.println("삭제 대상: " + targetUserId);

        // 3. 사용자 삭제
        synchronized (FILE_LOCK) {
            deleteUserById(targetUserId);
            return "DELETE_SUCCESS";
        }
    }

    /**
     * 입력 검증
     */
    private void validateInput(String[] params) throws InvalidInputException {
        if (params.length != 2) {
            throw new InvalidInputException(
                    "DELETE_USER 명령은 2개의 매개변수가 필요합니다 (현재: " + params.length + "개)"
            );
        }

        String targetUserId = params[1];
        if (targetUserId == null || targetUserId.trim().isEmpty()) {
            throw new InvalidInputException("targetUserId", targetUserId, 
                    "삭제할 사용자 ID를 입력해주세요");
        }
    }

    /**
     * 권한 확인
     */
    private void checkPermission() throws AuthenticationException {
        if (currentUserId == null || !userDAO.authorizeAccess(currentUserId)) {
            throw new AuthenticationException(
                    AuthenticationException.AuthFailureReason.INSUFFICIENT_PERMISSION,
                    "관리자 권한이 필요합니다"
            );
        }
    }

    /**
     * 사용자 삭제
     */
    private void deleteUserById(String userId) throws DatabaseException, BusinessLogicException, InvalidInputException {
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

        boolean found = false;
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 2 && parts[1].trim().equals(userId)) {
                    found = true;
                    System.out.println("사용자 삭제: " + line);
                    continue; // 이 줄은 쓰지 않음 (삭제)
                }
                writer.write(line);
                writer.newLine();
            }

        } catch (IOException e) {
            // 임시 파일 정리
            if (tempFile.exists()) {
                tempFile.delete();
            }
            throw new DatabaseException(
                    inputFile.getName(),
                    DatabaseException.OperationType.DELETE,
                    "사용자 삭제 중 오류가 발생했습니다",
                    e
            );
        }

        if (!found) {
            // 사용자를 찾지 못함
            tempFile.delete();
            throw new BusinessLogicException(
                    BusinessLogicException.BusinessRuleViolation.RESERVATION_NOT_FOUND,
                    "삭제할 사용자를 찾을 수 없습니다: " + userId
            );
        }

        // 원본 파일 삭제 및 임시 파일을 원본으로 변경
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

        System.out.println("사용자 삭제 완료: " + userId);
    }

    /**
     * 사용자 ID로 파일 경로 결정
     */
    private String getUserFileById(String userId) {
        if (userId == null) {
            return null;
        }
        if (userId.startsWith("S")) {
            return BASE_DIR + "/users.txt";
        }
        if (userId.startsWith("P")) {
            return BASE_DIR + "/prof.txt";
        }
        if (userId.startsWith("A")) {
            return BASE_DIR + "/assistant.txt";
        }
        return null;
    }
}
