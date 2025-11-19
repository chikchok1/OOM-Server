package Server.commands;

import Server.manager.ServerClassroomManager;
import common.dto.ClassroomDTO;
import java.io.*;
import java.util.List;

/**
 * 모든 실습실 목록 반환
 */
public class GetLabsCommand implements Command {
    
    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) throws IOException {
        ServerClassroomManager manager = ServerClassroomManager.getInstance();
        List<ClassroomDTO> labs = manager.getAllLabs();
        
        // 프로토콜: LABS,name1,type1,capacity1,name2,type2,capacity2,...
        StringBuilder response = new StringBuilder("LABS");
        
        for (ClassroomDTO dto : labs) {
            response.append(",").append(dto.name);
            response.append(",").append(dto.type);
            response.append(",").append(dto.capacity);
        }
        
        System.out.println("[서버] GET_LABS: " + labs.size() + "개 반환");
        return response.toString();
    }
}
