package Server;

import Server.commands.*;
import common.utils.ConfigLoader;
import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class LoginServer {

    private static boolean running = true;
    private static ServerSocket serverSocket;
    private static final int MAX_CLIENTS = 3;
    private static final AtomicInteger currentClients = new AtomicInteger(0);
    private static final ConcurrentHashMap<String, Socket> loggedInUsers = new ConcurrentHashMap<>();
    private static final Object FILE_LOCK = new Object();
    private static final String BASE_DIR = System.getProperty("user.dir") + File.separator + "data";
    private static final String ROOM_STATUS_FILE = BASE_DIR + File.separator + "RoomStatus.txt";
    
    private static CommandFactory commandFactory;
    private static UserDAO userDAO;

    public static void main(String[] args) {
        // 초기화
        new File(BASE_DIR).mkdirs();
        createFileIfNotExists(ROOM_STATUS_FILE);
        System.out.println("RoomStatus 파일 경로: " + ROOM_STATUS_FILE);

        userDAO = new UserDAO();
        
        // CommandFactory 초기화
        commandFactory = new CommandFactory(
            userDAO, 
            BASE_DIR, 
            FILE_LOCK,
            loggedInUsers,
            currentClients,
            MAX_CLIENTS
        );

        try {
            logServerStart();

            String portStr = ConfigLoader.getProperty("server.port");
            if (portStr == null) {
                throw new RuntimeException("config.properties에서 'server.port'를 찾을 수 없습니다.");
            }

            int serverPort = Integer.parseInt(portStr);
            serverSocket = new ServerSocket(serverPort, 50, InetAddress.getByName("0.0.0.0"));
            System.out.println("서버 시작: 포트 " + serverPort);

            while (running) {
                Socket socket = serverSocket.accept();
                System.out.println("새 클라이언트 접속: " + socket.getInetAddress());

                new Thread(() -> {
                    String userId = null;
                    try {
                        userId = handleClient(socket);
                    } catch (IOException e) {
                        System.err.println("클라이언트 처리 중 IOException: " + e.getMessage());
                    } finally {
                        if (userId != null) {
                            synchronized (LoginServer.class) {
                                loggedInUsers.remove(userId);
                                currentClients.decrementAndGet();
                                System.out.println(userId + " 로그아웃 처리됨");
                                System.out.println("현재 로그인 중인 사용자: " + loggedInUsers.keySet());
                                System.out.println("현재 접속자 수: " + currentClients.get());
                            }
                        }
                        try {
                            if (socket != null && !socket.isClosed()) {
                                socket.close();
                            }
                        } catch (IOException ignored) {}
                    }
                }).start();
            }
        } catch (IOException e) {
            System.err.println("서버 오류: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("설정 파일에서 포트 번호를 파싱하는 중 오류: " + e.getMessage());
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException ignored) {}
            }
        }
    }

    /**
     * 클라이언트 연결 처리 (Command 패턴 적용)
     */
    private static String handleClient(Socket socket) throws IOException {
        String loggedInUserId = null;

        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String request = in.readLine();
            System.out.println("수신된 요청: " + request);

            if (request == null) {
                return null;
            }

            // SHUTDOWN 명령 처리
            if (request.equalsIgnoreCase("SHUTDOWN")) {
                out.println("SERVER_SHUTTING_DOWN");
                out.flush();
                running = false;
                serverSocket.close();
                return null;
            }

            //  Command 패턴 적용: 요청 문자열 → Command 객체 생성
            Command command = commandFactory.createCommand(request);
            
            if (command == null) {
                out.println("UNKNOWN_COMMAND");
                out.flush();
                return null;
            }

            //  Command 실행
            String[] params = request.split(",");
            String response = command.execute(params, in, out);
            
            // 일부 Command는 직접 출력하므로 null 반환 (ViewReservation 등)
            if (response != null) {
                out.println(response);
                out.flush();
            }

            // 로그인 성공 시 세션 등록 및 후속 메시지 처리
            if (request.startsWith("LOGIN") && response != null && response.startsWith("SUCCESS")) {
                loggedInUserId = params[1];
                loggedInUsers.put(loggedInUserId, socket);
                currentClients.incrementAndGet();
                
                System.out.println(loggedInUserId + " 로그인 성공");
                System.out.println("현재 로그인 중인 사용자: " + loggedInUsers.keySet());
                System.out.println("현재 접속자 수: " + currentClients.get());
                
                // 로그인 후 후속 메시지 처리
                handleSubsequentMessages(in, out, loggedInUserId);
            }

            // REGISTER 명령 처리 (로그인과 별도)
            if (request.startsWith("REGISTER") && response != null) {
                // 회원가입은 별도 세션 없이 응답만 전송
                System.out.println("회원가입 처리 완료: " + response);
            }

        } catch (IOException e) {
            System.err.println("클라이언트 처리 중 오류: " + e.getMessage());
            throw e;
        }

        return loggedInUserId;
    }

    /**
     * 로그인 후 후속 메시지 처리 (Command 패턴 적용)
     */
    private static void handleSubsequentMessages(BufferedReader in, PrintWriter out, String userId) 
            throws IOException {
        while (true) {
            String request = in.readLine();
            
            // 연결 종료 또는 로그아웃
            if (request == null || request.equalsIgnoreCase("EXIT")) {
                out.println("LOGOUT_SUCCESS");
                out.flush();
                System.out.println("[" + userId + "] 로그아웃 요청 수신 또는 연결 끊김.");
                break;
            }

            // INIT 메시지 무시 (연결 확인용)
            if (request.equals("INIT")) {
                System.out.println("[" + userId + "] INIT 메시지 수신 (연결 확인)");
                continue;
            }

            System.out.println("[" + userId + "] 후속 메시지 수신: " + request);

            //  Command 패턴으로 모든 후속 메시지 처리
            Command command = commandFactory.createCommand(request, userId);
            
            if (command == null) {
                out.println("UNKNOWN_COMMAND_AFTER_LOGIN");
                out.flush();
                continue;
            }

            // Command 실행
            String[] params = request.split(",");
            String response = command.execute(params, in, out);
            
            // 일부 Command는 직접 출력하므로 null 반환
            if (response != null) {
                out.println(response);
                out.flush();
            }
        }
    }
    
    /**
     * 파일이 없으면 생성
     */
    private static void createFileIfNotExists(String fileName) {
        try {
            File file = new File(fileName);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
                System.out.println("파일 생성: " + fileName);
            }
        } catch (IOException e) {
            System.err.println("파일 생성 오류: " + e.getMessage());
        }
    }

    /**
     * 서버 시작 로그 기록
     */
    private static void logServerStart() {
        try (PrintWriter logWriter = new PrintWriter(new FileWriter("server_debug.log", true))) {
            logWriter.println("=================================");
            logWriter.println("서버가 실행되었습니다.");
            logWriter.println("시작 시간: " + new java.util.Date());
            logWriter.println("현재 작업 디렉토리: " + System.getProperty("user.dir"));
            logWriter.println("=================================");
            logWriter.flush();
        } catch (IOException e) {
            System.err.println("로그 파일 작성 실패: " + e.getMessage());
        }
    }
}

