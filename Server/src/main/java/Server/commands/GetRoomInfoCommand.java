package Server.commands;

import common.manager.ClassroomManager;
import java.io.*;

/**
 * 강의실 정보 조회 명령어 (수용인원, 허용인원)
 * 형식: GET_ROOM_INFO,강의실명
 * 응답: ROOM_INFO:수용인원,허용인원 또는 ERROR
 */
public class GetRoomInfoCommand implements Command {
    
    @Override
    public String execute(String[] parts, BufferedReader in, PrintWriter out) throws IOException {
        try {
            if (parts.length < 2) {
                out.println("ERROR:잘못된 요청 형식");
                out.flush();
                return "ERROR";
            }
            
            String roomName = parts[1].trim();
            ClassroomManager manager = ClassroomManager.getInstance();
            
            if (!manager.exists(roomName)) {
                out.println("ERROR:존재하지 않는 강의실");
                out.flush();
                System.err.println("[GetRoomInfoCommand] 존재하지 않는 강의실: " + roomName);
                return "ERROR";
            }
            
            ClassroomManager.Classroom classroom = manager.getClassroom(roomName);
            int capacity = classroom.capacity;
            int allowedCapacity = classroom.getAllowedCapacity();
            
            out.println(String.format("ROOM_INFO:%d,%d", capacity, allowedCapacity));
            out.flush();
            
            System.out.println(String.format("[GetRoomInfoCommand] %s 정보 전송: 수용=%d, 허용=%d", 
                roomName, capacity, allowedCapacity));
            
            return "SUCCESS";
            
        } catch (Exception e) {
            out.println("ERROR:강의실 정보 조회 실패");
            out.flush();
            System.err.println("[GetRoomInfoCommand] 오류: " + e.getMessage());
            return "ERROR";
        }
    }
}
