package Server.exceptions;

/**
 * 잘못된 입력 데이터에 대한 예외
 * 클라이언트가 잘못된 형식이나 값을 전송했을 때 발생
 */
public class InvalidInputException extends Exception {
    private final String field;
    private final String invalidValue;

    public InvalidInputException(String message) {
        super(message);
        this.field = null;
        this.invalidValue = null;
    }

    public InvalidInputException(String field, String invalidValue, String message) {
        super(String.format("%s 필드에 잘못된 값 '%s': %s", field, invalidValue, message));
        this.field = field;
        this.invalidValue = invalidValue;
    }

    public String getField() {
        return field;
    }

    public String getInvalidValue() {
        return invalidValue;
    }
}
