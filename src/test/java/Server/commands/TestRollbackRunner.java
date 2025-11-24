package Server.commands;

/**
 * TestRollbackCommand 간단 실행 클래스
 * CommandInvoker를 통해 실행하여 error_log.txt에 로그 기록
 * 
 * 실행 방법:
 * mvn exec:java -Dexec.mainClass="Server.commands.TestRollbackRunner"
 */
public class TestRollbackRunner {
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════╗");
        System.out.println("║     TestRollbackCommand 시연 프로그램          ║");
        System.out.println("║     (CommandInvoker를 통한 로그 기록)          ║");
        System.out.println("╚════════════════════════════════════════════════╝\n");
        
        // 테스트 디렉토리 설정
        String testDir = "test_data";
        java.io.File dir = new java.io.File(testDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // CommandInvoker 생성
        CommandInvoker invoker = new CommandInvoker();
        
        // 명령 생성
        Object fileLock = new Object();
        TestRollbackCommand command = new TestRollbackCommand(testDir, fileLock);
        
        // CommandInvoker에 명령 설정
        invoker.setCommand(command, "TEST_ROLLBACK");
        
        // 명령 실행 (예외 처리는 CommandInvoker가 담당)
        String result = invoker.execute(new String[]{"TEST_ROLLBACK"}, null, null);
        
        System.out.println("\n==========================================================");
        System.out.println(" 실행 결과                                     ");
        System.out.println("==========================================================");
        System.out.println("응답: " + result);
        
        if (result != null && result.startsWith("ERROR:")) {
            System.out.println("\n 예외 처리 완료!");
            System.out.println(" 에러 로그가 다음 위치에 저장되었습니다:");
            System.out.println("   → data/error_log.txt (프로젝트 루트)\n");
            
            // 로그 파일 내용 출력 (실제 경로)
            printErrorLog("data/error_log.txt");
        }
        
        // 정리
        deleteDirectory(dir);
        System.out.println("️  테스트 파일 정리 완료");
    }
    
    /**
     * 에러 로그 파일 내용 출력
     */
    private static void printErrorLog(String logPath) {
        java.io.File logFile = new java.io.File(logPath);
        if (!logFile.exists()) {
            System.out.println("️  로그 파일이 생성되지 않았습니다: " + logPath);
            return;
        }
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println(" ERROR_LOG.TXT 내용:");
        System.out.println("=".repeat(60));
        
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (java.io.IOException e) {
            System.out.println("️  로그 파일 읽기 실패: " + e.getMessage());
        }
        
        System.out.println("=".repeat(60) + "\n");
    }
    
    private static void deleteDirectory(java.io.File directory) {
        java.io.File[] files = directory.listFiles();
        if (files != null) {
            for (java.io.File file : files) {
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
