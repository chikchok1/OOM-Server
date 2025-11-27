package Server.exceptions;

/**
 * 인증 및 권한 관련 예외
 * 로그인 실패, 권한 부족 등의 상황에서 발생
 */
public class AuthenticationException extends Exception {
    private final AuthFailureReason reason;

    public enum AuthFailureReason {
        INVALID_CREDENTIALS("아이디 또는 비밀번호가 일치하지 않습니다"),
        USER_NOT_FOUND("존재하지 않는 사용자입니다"),
        ACCOUNT_LOCKED("계정이 잠겨있습니다"),
        INSUFFICIENT_PERMISSION("권한이 부족합니다"),
        SESSION_EXPIRED("세션이 만료되었습니다"),
        ALREADY_LOGGED_IN("이미 로그인된 사용자입니다");

        private final String message;

        AuthFailureReason(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public AuthenticationException(AuthFailureReason reason) {
        super(reason.getMessage());
        this.reason = reason;
    }

    public AuthenticationException(AuthFailureReason reason, String additionalInfo) {
        super(reason.getMessage() + ": " + additionalInfo);
        this.reason = reason;
    }

    public AuthFailureReason getReason() {
        return reason;
    }
}
