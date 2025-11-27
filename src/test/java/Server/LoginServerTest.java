package Server;

import org.junit.jupiter.api.*;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LoginServer 통합 테스트
 * CommandInvoker를 통한 새로운 에러 응답 형식 지원
 * 
 * 응답 형식:
 * - 성공: SUCCESS,{userName}
 * - 실패: ERROR:{타입}:{메시지}
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LoginServerTest {

    private static final String HOST = "localhost";
    private static final int PORT = 5000;
    private static final File USERS_FILE = new File("data/users.txt");

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Thread serverThread;

    @BeforeAll
    void startServerAndPrepareFile() throws Exception {
        // 서버가 이미 실행 중인지 확인
        boolean needStart = true;
        try (Socket testSocket = new Socket(HOST, PORT)) {
            System.out.println("서버가 이미 실행 중입니다. 종료 후 새로 시작합니다.");
            // 기존 서버가 있으면 종료 명령을 보내고 재시작
            try (PrintWriter pw = new PrintWriter(testSocket.getOutputStream(), true);
                 BufferedReader br = new BufferedReader(new InputStreamReader(testSocket.getInputStream()))) {
                pw.println("SHUTDOWN");
                String resp = br.readLine();
                System.out.println("기존 서버 응답(종료): " + resp);
            } catch (Exception ex) {
                System.out.println("기존 서버 종료 시도 중 오류: " + ex.getMessage());
            }
            needStart = true;
        } catch (IOException e) {
            // 서버가 실행 중이지 않음 -> 시작 필요
            needStart = true;
        }

        if (needStart) {
            // 서버 시작
            serverThread = new Thread(() -> {
                try {
                    LoginServer.main(null);
                } catch (Exception ignored) {}
            });
            serverThread.start();
            Thread.sleep(1200); // 서버 준비 대기
        }

        // 테스트용 유저 파일 생성
        USERS_FILE.getParentFile().mkdirs();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE))) {
            writer.write("홍길동,S20230001,abc123");
            writer.newLine();
        }
    }

    @AfterAll
    void stopServerAndCleanUp() throws IOException, InterruptedException {
        // 서버 종료 명령 전송
        try (
            Socket shutdownSocket = new Socket(HOST, PORT);
            PrintWriter shutdownOut = new PrintWriter(shutdownSocket.getOutputStream(), true);
            BufferedReader shutdownIn = new BufferedReader(new InputStreamReader(shutdownSocket.getInputStream()))
        ) {
            shutdownOut.println("SHUTDOWN");
            String response = shutdownIn.readLine();
            System.out.println("서버 응답: " + response);
        }

        Thread.sleep(500);
        Files.deleteIfExists(USERS_FILE.toPath());
    }

    @BeforeEach
    void connect() throws IOException {
        socket = new Socket(HOST, PORT);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    @AfterEach
    void close() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    /**
     * 로그인 성공 테스트
     * 기대: SUCCESS,{userName}
     */
    @Test
    void testLoginSuccess() throws IOException {
        out.println("LOGIN,S20230001,abc123");
        String response = in.readLine();
        
        assertNotNull(response, "응답이 null입니다");
        assertTrue(response.startsWith("SUCCESS"), 
                "로그인 성공 시 SUCCESS로 시작해야 함. 실제: " + response);
    }

    /**
     * 로그인 실패 테스트 (잘못된 비밀번호)
     * 기대: ERROR:AUTH_FAILED:{메시지}
     */
    @Test
    void testLoginFail_WrongPassword() throws IOException {
        out.println("LOGIN,S20230001,wrongpass");
        String response = in.readLine();
        
        assertNotNull(response, "응답이 null입니다");
        
        // CommandInvoker를 통한 새로운 에러 형식 검증
        assertTrue(response.startsWith("ERROR:AUTH_FAILED:"), 
                "인증 실패 시 ERROR:AUTH_FAILED:로 시작해야 함. 실제: " + response);
        assertTrue(response.contains("아이디 또는 비밀번호"), 
                "에러 메시지에 '아이디 또는 비밀번호' 문구가 포함되어야 함. 실제: " + response);
    }

    /**
     * 회원가입 성공 테스트
     * 기대: SUCCESS
     */
    @Test
    void testRegisterSuccess() throws IOException {
        String randomId = "S" + (int) (Math.random() * 90000 + 10000);
        out.println("REGISTER,테스트유저," + randomId + ",pw1234");
        String response = in.readLine();
        
        assertNotNull(response, "응답이 null입니다");
        assertEquals("SUCCESS", response, 
                "회원가입 성공 시 SUCCESS를 반환해야 함. 실제: " + response);
    }

    /**
     * 중복 회원가입 실패 테스트
     * 기대: ERROR:BUSINESS_RULE_VIOLATION:{메시지}
     */
    @Test
    void testRegisterDuplicate() throws IOException {
        out.println("REGISTER,홍길동,S20230001,abc123");
        String response = in.readLine();
        
        assertNotNull(response, "응답이 null입니다");
        
        // CommandInvoker를 통한 새로운 에러 형식 검증
        assertTrue(response.startsWith("ERROR:BUSINESS_RULE_VIOLATION:"), 
                "비즈니스 규칙 위반 시 ERROR:BUSINESS_RULE_VIOLATION:로 시작해야 함. 실제: " + response);
        assertTrue(response.contains("이미"), 
                "에러 메시지에 '이미' 문구가 포함되어야 함. 실제: " + response);
    }
    
    /**
     * 에러 응답 파싱 헬퍼 메서드
     * @param errorResponse ERROR:타입:메시지 형식의 응답
     * @return [타입, 메시지]
     */
    private String[] parseErrorResponse(String errorResponse) {
        if (errorResponse == null || !errorResponse.startsWith("ERROR:")) {
            return new String[]{"UNKNOWN", "Unknown error"};
        }
        
        String[] parts = errorResponse.substring(6).split(":", 2);
        if (parts.length < 2) {
            return new String[]{parts[0], ""};
        }
        
        return parts; // [타입, 메시지]
    }
}
