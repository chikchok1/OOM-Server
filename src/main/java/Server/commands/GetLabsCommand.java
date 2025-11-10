package Server.commands;

import common.manager.ClassroomManager;
import java.io.*;

/**
 * 실습실 목록 조회 명령어
 * 형식: GET_LABS
 * 응답: LABS:911호,915호,916호,918호,901호
 */
public class GetLabsCommand implements Command {
    
    @Override
    public String execute(String[] parts, BufferedReader in, PrintWriter out) throws IOException {
        try {
            ClassroomManager manager = ClassroomManager.getInstance();
            String[] labs = manager.getLabNames();
            
            // 콤마로 구분된 문자열로 변환
            String labList = String.join(",", labs);
            
            out.println("LABS:" + labList);
            out.flush();
            
            System.out.println("[GetLabsCommand] 실습실 목록 전송: " + labs.length + "개");
            
            return null;  // 이미 응답을 전송했으므로 null 반환
            
        } catch (Exception e) {
            System.err.println("[GetLabsCommand] 오류: " + e.getMessage());
            out.println("ERROR:실습실 목록 조회 실패");
            out.flush();
            return "ERROR";
        }
    }
}
