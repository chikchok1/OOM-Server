package Server.exceptions;

/**
 * 비즈니스 로직 규칙 위반 예외
 * 예약 시간 충돌, 정원 초과 등 비즈니스 규칙 위반 시 발생
 */
public class BusinessLogicException extends Exception {
    private final BusinessRuleViolation violation;

    public enum BusinessRuleViolation {
        RESERVATION_CONFLICT("예약 시간이 중복됩니다"),
        CAPACITY_EXCEEDED("수용 인원을 초과했습니다"),
        INVALID_TIME_RANGE("유효하지 않은 시간 범위입니다"),
        ROOM_UNAVAILABLE("강의실을 사용할 수 없습니다"),
        DUPLICATE_REGISTRATION("이미 등록된 데이터입니다"),
        RESERVATION_NOT_FOUND("예약 정보를 찾을 수 없습니다"),
        INVALID_DATE("유효하지 않은 날짜입니다");

        private final String message;

        BusinessRuleViolation(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public BusinessLogicException(BusinessRuleViolation violation) {
        super(violation.getMessage());
        this.violation = violation;
    }

    public BusinessLogicException(BusinessRuleViolation violation, String details) {
        super(violation.getMessage() + " - " + details);
        this.violation = violation;
    }

    public BusinessRuleViolation getViolation() {
        return violation;
    }
}
