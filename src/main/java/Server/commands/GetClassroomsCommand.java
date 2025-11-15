package Server.commands;

import common.manager.ClassroomManager;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 강의실 목록 조회 명령어
 * 형식: GET_CLASSROOMS
 * 응답: CLASSROOMS:908호:CLASS,912호:CLASS,913호:CLASS,914호:CLASS,911호:LAB,915호:LAB,916호:LAB,918호:LAB
 */
public class GetClassroomsCommand implements Command {
    
    @Override
    public String execute(String[] parts, BufferedReader in, PrintWriter out) throws IOException {
        try {
            ClassroomManager manager = ClassroomManager.getInstance();
            
            // ✅ 강의실과 실습실 모두 가져오기
            String[] classrooms = manager.getClassroomNames();
            String[] labs = manager.getLabNames();
            
            // 타입 정보와 함께 전송 (room:type 형식)
            List<String> roomsWithType = new ArrayList<>();
            
            for (String room : classrooms) {
                roomsWithType.add(room + ":CLASS");
            }
            
            for (String lab : labs) {
                roomsWithType.add(lab + ":LAB");
            }
            
            // 콤마로 구분된 문자열로 변환
            String roomList = String.join(",", roomsWithType);
            
            out.println("CLASSROOMS:" + roomList);
            out.flush();
            
            System.out.println(String.format(
                "[GetClassroomsCommand] 강의실 목록 전송: 강의실 %d개 + 실습실 %d개 = 총 %d개",
                classrooms.length, labs.length, roomsWithType.size()
            ));
            
            return null;  // 이미 응답을 전송했으므로 null 반환
            
        } catch (Exception e) {
            System.err.println("[GetClassroomsCommand] 오류: " + e.getMessage());
            e.printStackTrace();
            out.println("ERROR:강의실 목록 조회 실패");
            out.flush();
            return "ERROR";
        }
    }
}
