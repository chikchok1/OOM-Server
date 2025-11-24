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
import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;
import Server.exceptions.*;

public class UpdateRoomStatusCommand implements Command {
    private final String BASE_DIR;
    private final String ROOM_STATUS_FILE;
    private final UserDAO userDAO;
    private final String currentUserId;

    public UpdateRoomStatusCommand(String baseDir, UserDAO userDAO, String currentUserId) {
        this.BASE_DIR = baseDir;
        this.ROOM_STATUS_FILE = BASE_DIR + File.separator + "RoomStatus.txt";
        this.userDAO = userDAO;
        this.currentUserId = currentUserId;
    }

    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) throws IOException, InvalidInputException, DatabaseException, AuthenticationException, BusinessLogicException {
        if (params.length != 3) {
            return "INVALID_UPDATE_FORMAT";
        }

           if (currentUserId == null || !userDAO.authorizeAccess(currentUserId)) {
            return "ACCESS_DENIED";
        }

        String roomNumber = params[1].trim();
        String status = params[2].trim();

        synchronized (UpdateRoomStatusCommand.class) {
            try {
                updateRoomStatusFile(roomNumber, status);
                return "ROOM_STATUS_UPDATED";
            } catch (IOException e) {
                return "UPDATE_FAILED_SERVER_ERROR";
            }
        }
    }

    private void updateRoomStatusFile(String roomNumber, String newStatus) throws IOException, InvalidInputException, DatabaseException, AuthenticationException, BusinessLogicException {
        Map<String, String> roomStatusMap = new LinkedHashMap<>();
        File statusFile = new File(ROOM_STATUS_FILE);

        System.out.println("[서버] updateRoomStatusFile 호출됨 - room: " + roomNumber + ", status: " + newStatus);

        if (statusFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(statusFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] tokens = line.split(",");
                    if (tokens.length >= 2) {
                        String room = tokens[0].trim();
                        String status = tokens[1].trim();
                        roomStatusMap.put(room, status);
                    }
                }
            } catch (IOException e) {
                System.err.println("[서버 오류] RoomStatus 읽기 실패: " + e.getMessage());
                throw e;
            }
        } else {
            statusFile.createNewFile();
            System.out.println("[서버] RoomStatus.txt 파일 생성됨");
        }

        if ("사용가능".equals(newStatus)) {
            roomStatusMap.remove(roomNumber);
            System.out.println("[서버] '" + roomNumber + "' 제거됨 (사용가능)");
        } else if ("사용불가".equals(newStatus)) {
            roomStatusMap.put(roomNumber, "사용불가");
            System.out.println("[서버] '" + roomNumber + "' 추가/업데이트됨 (사용불가)");
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(statusFile, false))) {
            for (Map.Entry<String, String> entry : roomStatusMap.entrySet()) {
                writer.write(entry.getKey() + "," + entry.getValue());
                writer.newLine();
            }
            writer.flush();
            System.out.println("[서버] RoomStatus.txt 저장 완료.");
        } catch (IOException e) {
            System.err.println("[서버 오류] RoomStatus 저장 실패: " + e.getMessage());
            throw e;
        }
    }
}
