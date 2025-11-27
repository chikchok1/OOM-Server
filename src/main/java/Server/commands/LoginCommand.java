package Server.commands;

import Server.UserDAO;
import Server.exceptions.*;
import common.model.User;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 로그인 명령 처리 (Exception Handling 적용)
 * 
 * 예외 처리 전략:
 * - 입력 검증 실패 → InvalidInputException
 * - 사용자 인증 실패 → AuthenticationException
 * - 파일 읽기 실패 → DatabaseException
 */
public class LoginCommand implements Command {
    private final UserDAO userDAO;
    private final ConcurrentHashMap<String, Socket> loggedInUsers;
    private final AtomicInteger currentClients;
    private final int MAX_CLIENTS;

    public LoginCommand(UserDAO userDAO,
                        ConcurrentHashMap<String, Socket> loggedInUsers,
                        AtomicInteger currentClients,
                        int maxClients) {
        this.userDAO = userDAO;
        this.loggedInUsers = loggedInUsers;
        this.currentClients = currentClients;
        this.MAX_CLIENTS = maxClients;
    }

    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out)
            throws InvalidInputException, AuthenticationException, DatabaseException {
        
        // 1. 입력 검증
        validateInput(params);
        
        String userId = params[1];
        String password = params[2];

        System.out.println("로그인 시도: " + userId);

        synchronized (LoginCommand.class) {
            // 2. 동시 접속자 수 제한 확인
            checkConnectionLimit();

            // 3. 중복 로그인 확인
            checkDuplicateLogin(userId);

            // 4. 사용자 인증
            authenticateUser(userId, password);

            // 5. 사용자 정보 조회
            String name = getUserName(userId);

            System.out.println(userId + " 로그인 성공");

            // 6. 성공 응답
            return "SUCCESS," + name;
        }
    }

    /**
     * 입력 매개변수 검증
     * @throws InvalidInputException 입력이 유효하지 않을 때
     */
    private void validateInput(String[] params) throws InvalidInputException {
        if (params.length != 3) {
            throw new InvalidInputException("로그인 명령은 3개의 매개변수가 필요합니다 (현재: " + params.length + "개)");
        }

        String userId = params[1];
        String password = params[2];

        if (userId == null || userId.trim().isEmpty()) {
            throw new InvalidInputException("userId", userId, "아이디를 입력해주세요");
        }

        if (password == null || password.trim().isEmpty()) {
            throw new InvalidInputException("password", "", "비밀번호를 입력해주세요");
        }

        // 아이디 형식 검증
        if (userId.length() < 3 || userId.length() > 20) {
            throw new InvalidInputException("userId", userId, 
                    "아이디는 3~20자 사이여야 합니다");
        }
    }

    /**
     * 동시 접속자 수 제한 확인
     * @throws AuthenticationException 접속자 수 초과 시
     */
    private void checkConnectionLimit() throws AuthenticationException {
        if (currentClients.get() >= MAX_CLIENTS) {
            System.out.println("접속 초과로 로그인 실패");
            throw new AuthenticationException(
                    AuthenticationException.AuthFailureReason.ACCOUNT_LOCKED,
                    "서버 접속자 수가 최대치에 도달했습니다 (최대 " + MAX_CLIENTS + "명)"
            );
        }
    }

    /**
     * 중복 로그인 확인
     * @throws AuthenticationException 이미 로그인한 사용자일 때
     */
    private void checkDuplicateLogin(String userId) throws AuthenticationException {
        if (loggedInUsers.containsKey(userId)) {
            System.out.println("중복 로그인 시도: " + userId);
            
            // 기존 소켓 정리
            Socket oldSocket = loggedInUsers.get(userId);
            try {
                if (oldSocket != null && !oldSocket.isClosed()) {
                    oldSocket.close();
                }
            } catch (IOException ignored) {
            }
            
            throw new AuthenticationException(
                    AuthenticationException.AuthFailureReason.ALREADY_LOGGED_IN,
                    "이미 접속 중인 계정입니다. 잠시 후 다시 시도해주세요."
            );
        }
    }

    /**
     * 사용자 인증
     * @throws AuthenticationException 인증 실패 시
     * @throws DatabaseException 데이터 조회 실패 시
     */
    private void authenticateUser(String userId, String password) 
            throws AuthenticationException, DatabaseException {
        try {
            boolean valid = userDAO.validateUser(userId, password);
            System.out.println("유효성 검사 결과: " + valid);

            if (!valid) {
                throw new AuthenticationException(
                        AuthenticationException.AuthFailureReason.INVALID_CREDENTIALS
                );
            }
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            throw new DatabaseException(
                    "users.txt", 
                    DatabaseException.OperationType.READ,
                    "사용자 정보를 읽는 중 오류가 발생했습니다",
                    e
            );
        }
    }

    /**
     * 사용자 이름 조회
     * @throws DatabaseException 데이터 조회 실패 시
     */
    private String getUserName(String userId) throws DatabaseException {
        try {
            String name = userDAO.getUserNameById(userId);
            if (name == null || name.isEmpty()) {
                throw new DatabaseException(
                        "users.txt",
                        DatabaseException.OperationType.READ,
                        "사용자 이름을 찾을 수 없습니다"
                );
            }
            return name;
        } catch (DatabaseException e) {
            throw e;
        } catch (Exception e) {
            throw new DatabaseException(
                    "users.txt", 
                    DatabaseException.OperationType.READ,
                    "사용자 정보를 읽는 중 오류가 발생했습니다",
                    e
            );
        }
    }
}
