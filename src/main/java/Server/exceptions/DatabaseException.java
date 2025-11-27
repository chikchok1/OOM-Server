package Server.exceptions;

/**
 * 파일 시스템(데이터베이스 역할) 관련 예외
 * 파일 읽기/쓰기 실패 시 발생
 */
public class DatabaseException extends Exception {
    private final String fileName;
    private final OperationType operation;

    public enum OperationType {
        READ, WRITE, DELETE, UPDATE
    }

    public DatabaseException(String message) {
        super(message);
        this.fileName = null;
        this.operation = null;
    }

    public DatabaseException(String fileName, OperationType operation, String message) {
        super(String.format("파일 '%s'에서 %s 작업 실패: %s", fileName, operation, message));
        this.fileName = fileName;
        this.operation = operation;
    }

    public DatabaseException(String fileName, OperationType operation, String message, Throwable cause) {
        super(String.format("파일 '%s'에서 %s 작업 실패: %s", fileName, operation, message), cause);
        this.fileName = fileName;
        this.operation = operation;
    }

    public String getFileName() {
        return fileName;
    }

    public OperationType getOperation() {
        return operation;
    }
}
