package Server.commands;

import Server.exceptions.*;
import java.io.*;

/**
 * Command 패턴의 핵심 인터페이스
 * 모든 명령 클래스가 구현해야 하는 메서드 정의
 * 
 * Exception Handling 전략:
 * - IOException: 네트워크/파일 I/O 오류
 * - InvalidInputException: 잘못된 입력 데이터
 * - DatabaseException: 데이터 저장소 오류
 * - AuthenticationException: 인증/권한 오류
 * - BusinessLogicException: 비즈니스 규칙 위반
 */
public interface Command {
    /**
     * 명령을 실행하고 결과를 반환
     * 
     * @param params 명령 매개변수 배열
     * @param in 클라이언트로부터의 입력 스트림
     * @param out 클라이언트로의 출력 스트림
     * @return 실행 결과 문자열 (null이면 이미 out으로 응답함)
     * @throws IOException 네트워크/파일 I/O 오류
     * @throws InvalidInputException 잘못된 입력 데이터
     * @throws DatabaseException 데이터 저장소 오류
     * @throws AuthenticationException 인증/권한 오류
     * @throws BusinessLogicException 비즈니스 규칙 위반
     */
    String execute(String[] params, BufferedReader in, PrintWriter out) 
            throws IOException, InvalidInputException, DatabaseException, 
                   AuthenticationException, BusinessLogicException;
}
