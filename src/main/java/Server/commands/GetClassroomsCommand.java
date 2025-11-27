package Server.commands;

import Server.exceptions.*;
import Server.manager.ServerClassroomManager;
import common.dto.ClassroomDTO;
import java.io.*;
import java.util.List;

/**
 * 모든 강의실 목록 반환
 */
public class GetClassroomsCommand implements Command {
    
    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) 
            throws IOException, InvalidInputException, DatabaseException, 
                   AuthenticationException, BusinessLogicException {
        ServerClassroomManager manager = ServerClassroomManager.getInstance();
        List<ClassroomDTO> classrooms = manager.getAllClassrooms();
        
        // 프로토콜: CLASSROOMS,name1,type1,capacity1,name2,type2,capacity2,...
        StringBuilder response = new StringBuilder("CLASSROOMS");
        
        for (ClassroomDTO dto : classrooms) {
            response.append(",").append(dto.name);
            response.append(",").append(dto.type);
            response.append(",").append(dto.capacity);
        }
        
        System.out.println("[서버] GET_CLASSROOMS: " + classrooms.size() + "개 반환");
        return response.toString();
    }
}
