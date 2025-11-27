package Server.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

/**
 * TestRollbackCommand 테스트 클래스
 * 
 * 실행 방법:
 * 1. Maven 사용: mvn test -Dtest=TestRollbackCommandTest
 * 2. IDE에서: 이 클래스에서 마우스 우클릭 > Run Test
 */
public class TestRollbackCommandTest {
    
    private static final String TEST_DIR = "test_data";
    private Object fileLock;
    private TestRollbackCommand command;
    
    @BeforeEach
    public void setUp() {
        // 테스트용 디렉토리 생성
        File testDir = new File(TEST_DIR);
        if (!testDir.exists()) {
            testDir.mkdirs();
        }
        
        fileLock = new Object();
        command = new TestRollbackCommand(TEST_DIR, fileLock);
    }
    
    @AfterEach
    public void tearDown() {
        // 테스트 데이터 정리
        File testDir = new File(TEST_DIR);
        if (testDir.exists()) {
            deleteDirectory(testDir);
        }
    }
    
    @Test
    @DisplayName("롤백 시연 테스트 - 정상 실행 확인")
    public void testRollbackDemo() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("롤백 테스트 시작");
        System.out.println("=".repeat(50));
        
        try {
            // TestRollbackCommand는 의도적으로 DatabaseException을 던짐
            command.execute(new String[]{}, null, null);
            fail("DatabaseException이 발생해야 합니다");
        } catch (Exception e) {
            // 예외가 발생하는 것이 정상 동작
            System.out.println("\n 예상된 예외 발생: " + e.getClass().getSimpleName());
            System.out.println("   메시지: " + e.getMessage());
            
            // 롤백 후 임시 파일이 삭제되었는지 확인
            File tempFile = new File(TEST_DIR + "/TestRollbackData.txt.tmp");
            assertFalse(tempFile.exists(), 
                "롤백 후 임시 파일은 존재하지 않아야 합니다");
            
            System.out.println(" 롤백 검증 완료: 임시 파일이 정상적으로 삭제됨");
        }
        
        System.out.println("=".repeat(50) + "\n");
    }
    
    /**
     * 디렉토리 삭제 헬퍼 메서드
     */
    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }
}
