package Server.commands;

import Server.exceptions.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Command 패턴의 Invoker 역할
 * 모든 명령 실행을 중앙에서 관리하고 예외를 일관되게 처리
 * 
 * Exception Handling 전략을 구현:
 * 1. 각 예외 타입별로 적절한 에러 메시지 생성
 * 2. 심각한 오류는 로그 파일에 기록
 * 3. 클라이언트에게는 친절한 메시지 반환
 */
public class CommandInvoker {
    private static final String ERROR_LOG_FILE = "data/error_log.txt";
    private static final DateTimeFormatter DATE_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private Command command;
    private String commandName;

    /**
     * 실행할 명령 설정
     * @param command 실행할 Command 객체
     * @param commandName 명령 이름 (로깅용)
     */
    public void setCommand(Command command, String commandName) {
        this.command = command;
        this.commandName = commandName;
    }

    /**
     * 명령 실행 및 예외 처리
     * 
     * @param params 명령 매개변수
     * @param in 입력 스트림
     * @param out 출력 스트림
     * @return 실행 결과 또는 에러 메시지
     */
    public String execute(String[] params, BufferedReader in, PrintWriter out) {
        if (command == null) {
            return "ERROR:NO_COMMAND:명령이 설정되지 않았습니다";
        }

        try {
            // 명령 실행
            String result = command.execute(params, in, out);
            return result;

        } catch (InvalidInputException e) {
            // 잘못된 입력 - 클라이언트 오류 (4xx)
            logError(ErrorLevel.WARNING, e, params);
            return formatErrorResponse("INVALID_INPUT", e.getMessage());

        } catch (AuthenticationException e) {
            // 인증 실패 - 클라이언트 오류 (401, 403)
            logError(ErrorLevel.WARNING, e, params);
            return formatErrorResponse("AUTH_FAILED", e.getMessage());

        } catch (BusinessLogicException e) {
            // 비즈니스 규칙 위반 - 클라이언트 오류 (400, 409)
            logError(ErrorLevel.INFO, e, params);
            return formatErrorResponse("BUSINESS_RULE_VIOLATION", e.getMessage());

        } catch (DatabaseException e) {
            // 데이터베이스 오류 - 서버 오류 (5xx)
            logError(ErrorLevel.ERROR, e, params);
            return formatErrorResponse("DATABASE_ERROR", 
                    "서버에 일시적인 문제가 발생했습니다. 잠시 후 다시 시도해주세요.");

        } catch (IOException e) {
            // 네트워크/파일 I/O 오류 - 서버 오류 (5xx)
            logError(ErrorLevel.ERROR, e, params);
            return formatErrorResponse("IO_ERROR", 
                    "통신 오류가 발생했습니다. 연결을 확인해주세요.");

        } catch (Exception e) {
            // 예상치 못한 오류 - 서버 오류 (500)
            logError(ErrorLevel.CRITICAL, e, params);
            return formatErrorResponse("UNKNOWN_ERROR", 
                    "예상치 못한 오류가 발생했습니다. 관리자에게 문의해주세요.");
        }
    }

    /**
     * 에러 응답 형식 생성
     * 형식: ERROR:타입:메시지
     */
    private String formatErrorResponse(String errorType, String message) {
        return String.format("ERROR:%s:%s", errorType, message);
    }

    /**
     * 에러 로그 기록
     * @param level 에러 심각도
     * @param e 발생한 예외
     * @param params 명령 매개변수
     */
    private void logError(ErrorLevel level, Exception e, String[] params) {
        try {
            File logFile = new File(ERROR_LOG_FILE);
            logFile.getParentFile().mkdirs();

            try (PrintWriter logWriter = new PrintWriter(
                    new FileWriter(logFile, true))) {
                
                logWriter.println("=".repeat(80));
                logWriter.println("시간: " + LocalDateTime.now().format(DATE_FORMATTER));
                logWriter.println("레벨: " + level);
                logWriter.println("명령: " + commandName);
                logWriter.println("매개변수: " + String.join(", ", params));
                logWriter.println("예외 타입: " + e.getClass().getSimpleName());
                logWriter.println("메시지: " + e.getMessage());
                
                if (level.ordinal() >= ErrorLevel.ERROR.ordinal()) {
                    logWriter.println("스택 트레이스:");
                    e.printStackTrace(logWriter);
                }
                
                logWriter.println();
                logWriter.flush();
            }

            // 콘솔에도 출력
            System.err.printf("[%s] [%s] %s: %s%n", 
                    level, commandName, e.getClass().getSimpleName(), e.getMessage());

        } catch (IOException ioEx) {
            System.err.println("로그 파일 작성 실패: " + ioEx.getMessage());
        }
    }

    /**
     * 에러 심각도 레벨
     */
    private enum ErrorLevel {
        INFO,       // 정보성 (비즈니스 규칙 위반 등)
        WARNING,    // 경고 (잘못된 입력, 인증 실패)
        ERROR,      // 오류 (데이터베이스, I/O 오류)
        CRITICAL    // 심각 (예상치 못한 오류)
    }
}
