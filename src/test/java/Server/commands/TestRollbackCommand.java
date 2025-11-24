package Server.commands;

import Server.exceptions.*;
import java.io.*;

/**
 * 롤백 시연용 명령 - 간소화 버전
 * 
 * 사용법: TEST_ROLLBACK
 */
public class TestRollbackCommand implements Command {
    
    private final String BASE_DIR;
    private final Object FILE_LOCK;
    
    public TestRollbackCommand(String baseDir, Object fileLock) {
        this.BASE_DIR = baseDir;
        this.FILE_LOCK = fileLock;
    }

    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) 
            throws IOException, InvalidInputException, DatabaseException, 
                   AuthenticationException, BusinessLogicException {
        
        System.out.println("\n========================================");
        System.out.println("? 롤백 시연 시작");
        System.out.println("========================================\n");
        
        return testRollback();
    }
    
    /**
     * 롤백 시연: 파일 쓰기 중 오류 발생
     */
    private String testRollback() throws DatabaseException, IOException {
        String testFile = BASE_DIR + "/TestRollbackData.txt";
        File originalFile = new File(testFile);
        File tempFile = new File(testFile + ".tmp");
        
        System.out.println("시나리오: 파일 쓰기 중 오류 발생");
        System.out.println("-----------------------------------");
        
        try {
            // 1. 원본 파일 생성
            System.out.println("1️  원본 파일 생성 중...");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(originalFile))) {
                writer.write("원본 데이터 라인 1\n");
                writer.write("원본 데이터 라인 2\n");
                writer.write("원본 데이터 라인 3\n");
            }
            System.out.println("   원본 파일 생성 완료");
            printFileContent("원본", originalFile);
            
            // 2. 데이터 수정 시도
            System.out.println("\n2️  데이터 수정 시도 중...");
            performUpdateWithError(tempFile);
            
        } finally {
            // 테스트 파일 정리
            System.out.println("\n5️  테스트 파일 정리 중...");
            if (originalFile.exists()) {
                originalFile.delete();
                System.out.println("   ️   원본 파일 삭제 완료");
            }
            if (tempFile.exists()) {
                tempFile.delete();
                System.out.println("   ️   임시 파일 삭제 완료");
            }
            System.out.println("\n========================================");
            System.out.println(" 롤백 시연 완료");
            System.out.println("========================================\n");
        }
        
        return null; // 이미 예외 던짐
    }
    
    /**
     * 수정 작업 중 오류 발생 시뮬레이션
     */
    private void performUpdateWithError(File tempFile) throws DatabaseException {
        BufferedWriter tempWriter = null;
        
        try {
            tempWriter = new BufferedWriter(new FileWriter(tempFile));
            
            // 일부 데이터 쓰기 성공
            tempWriter.write("수정된 데이터 라인 1\n");
            System.out.println("    50% 완료...");
            
            tempWriter.write("수정된 데이터 라인 2\n");
            System.out.println("    75% 완료...");
            
            // 🔥 오류 발생!
            System.out.println("   ️   디스크 오류 시뮬레이션!");
            throw new IOException(" Simulated disk full error!");
            
        } catch (IOException e) {
            System.out.println("    오류 발생: " + e.getMessage());
            
            // 🔄 롤백 시작
            System.out.println("\n3️  롤백 수행 중...");
            
            // Writer 닫기
            if (tempWriter != null) {
                try {
                    tempWriter.close();
                } catch (IOException ignored) {}
            }
            
            // 임시 파일 삭제
            if (tempFile.exists()) {
                boolean deleted = tempFile.delete();
                System.out.println("      임시 파일 삭제: " + (deleted ? "성공" : "실패"));
            }
            
            System.out.println("    롤백 완료!");
            
            // 결과 확인
            System.out.println("\n4️  결과 확인:");
            printFileContent("원본 (롤백 후)", new File(BASE_DIR + "/TestRollbackData.txt"));
            
            if (tempFile.exists()) {
                System.out.println("   ️   임시 파일이 여전히 존재합니다!");
            } else {
                System.out.println("    임시 파일 없음 (정상)");
            }
            
            // 예외 전파
            throw new DatabaseException(
                    "TestRollbackData.txt",
                    DatabaseException.OperationType.WRITE,
                    "파일 쓰기 중 오류 발생 (롤백 시연)",
                    e
            );
        }
    }
    
    /**
     * 파일 내용 출력
     */
    private void printFileContent(String label, File file) {
        System.out.println("\n    [" + label + "] 내용:");
        if (!file.exists()) {
            System.out.println("       파일이 존재하지 않습니다");
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("      " + line);
            }
        } catch (IOException e) {
            System.out.println("       읽기 실패: " + e.getMessage());
        }
    }
}
