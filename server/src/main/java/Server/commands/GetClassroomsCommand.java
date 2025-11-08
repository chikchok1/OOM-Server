package Server.commands;

import common.manager.ClassroomManager;
import java.io.*;

/**
 * 강의실 목록 조회 명령어
 * 형식: GET_CLASSROOMS
 * 응답: CLASSROOMS:908호,912호,913호,914호,900호
 */
public class GetClassroomsCommand implements Command {
    
    @Override
    public String execute(String[] parts, BufferedReader in, PrintWriter out) throws IOException {
        try {
            ClassroomManager manager = ClassroomManager.getInstance();
            String[] classrooms = manager.getClassroomNames();
            
            // 콤마로 구분된 문자열로 변환
            String classroomList = String.join(",", classrooms);
            
            out.println("CLASSROOMS:" + classroomList);
            out.flush();
            
            System.out.println("[GetClassroomsCommand] 강의실 목록 전송: " + classrooms.length + "개");
            
            return null;  // 이미 응답을 전송했으므로 null 반환
            
        } catch (Exception e) {
            System.err.println("[GetClassroomsCommand] 오류: " + e.getMessage());
            out.println("ERROR:강의실 목록 조회 실패");
            out.flush();
            return "ERROR";
        }
    }
}
