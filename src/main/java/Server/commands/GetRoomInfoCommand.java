package Server.commands;

import Server.manager.ServerClassroomManager;
import common.dto.ClassroomDTO;
import java.io.*;

/**
 * 특정 강의실 정보 반환
 */
public class GetRoomInfoCommand implements Command {
    
    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) throws IOException {
        if (params.length < 2) {
            return "ERROR:INVALID_FORMAT";
        }
        
        String roomName = params[1].trim();
        
        ServerClassroomManager manager = ServerClassroomManager.getInstance();
        ClassroomDTO dto = manager.getClassroom(roomName);
        
        if (dto == null) {
            System.err.println("[서버] 존재하지 않는 강의실: " + roomName);
            return "ERROR:ROOM_NOT_FOUND";
        }
        
        // 프로토콜: ROOM_INFO:capacity,type
        String response = String.format("ROOM_INFO:%d,%s", dto.capacity, dto.type);
        
        System.out.println("[서버] GET_ROOM_INFO: " + roomName + " → " + response);
        return response;
    }
}
