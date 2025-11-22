/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Server.commands;

/**
 *
 * @author YangJinWon
 */
import Server.UserDAO;
import Server.manager.ServerClassroomManager;
import java.io.*;
import Server.exceptions.*;

public class UpdateRoomCapacityCommand implements Command {
    private final UserDAO userDAO;
    private final String currentUserId;

    public UpdateRoomCapacityCommand(UserDAO userDAO, String currentUserId) {
        this.userDAO = userDAO;
        this.currentUserId = currentUserId;
    }

    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) throws IOException, InvalidInputException, DatabaseException, AuthenticationException, BusinessLogicException {
        // 형식: UPDATE_ROOM_CAPACITY,강의실명,새용량
        if (params.length != 3) {
            return "INVALID_FORMAT";
        }

        // 조교 권한 확인
        if (currentUserId == null || !userDAO.authorizeAccess(currentUserId)) {
            return "ACCESS_DENIED";
        }

        String roomName = params[1].trim();
        int newCapacity;
        
        try {
            newCapacity = Integer.parseInt(params[2].trim());
            if (newCapacity <= 0) {
                return "INVALID_CAPACITY";
            }
        } catch (NumberFormatException e) {
            return "INVALID_CAPACITY";
        }

        // ServerClassroomManager를 통해 수용인원 변경
        ServerClassroomManager manager = ServerClassroomManager.getInstance();
        
        if (!manager.exists(roomName)) {
            return "ROOM_NOT_FOUND";
        }
        
        boolean success = manager.updateCapacity(roomName, newCapacity);
        
        if (success) {
            System.out.println("[서버] " + roomName + " 수용인원 변경: " + newCapacity);
            return "CAPACITY_UPDATED";
        } else {
            return "UPDATE_FAILED";
        }
    }
}
