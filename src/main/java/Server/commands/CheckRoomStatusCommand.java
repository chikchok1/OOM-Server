/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Server.commands;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import Server.exceptions.*;
/**
 *
 * @author YangJinWon
 */

public class CheckRoomStatusCommand implements Command {
    private final String BASE_DIR;
    private final String ROOM_STATUS_FILE;

    public CheckRoomStatusCommand(String baseDir) {
        this.BASE_DIR = baseDir;
        this.ROOM_STATUS_FILE = BASE_DIR + File.separator + "RoomStatus.txt";
    }

    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) throws IOException, InvalidInputException, DatabaseException, AuthenticationException, BusinessLogicException {
        System.out.println("========== CheckRoomStatusCommand 실행 ==========");
        System.out.println("파라미터: " + java.util.Arrays.toString(params));
        
        if (params == null || params.length != 2) {
            System.err.println("[CheckRoomStatus] 파라미터 오류");
            return "INVALID_CHECK_FORMAT";
        }

        String roomNumber = params[1].trim();
        System.out.println("[CheckRoomStatus] 체크할 강의실: " + roomNumber);
        
        File statusFile = new File(ROOM_STATUS_FILE);
        System.out.println("[CheckRoomStatus] 파일 경로: " + ROOM_STATUS_FILE);
        System.out.println("[CheckRoomStatus] 파일 존재: " + statusFile.exists());
        
        // 파일이 없거나 비어있으면 모든 강의실 사용 가능
        if (!statusFile.exists() || statusFile.length() == 0) {
            System.out.println("[CheckRoomStatus] 파일 없음/비어있음 → " + roomNumber + " AVAILABLE");
            return "AVAILABLE";
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(statusFile))) {
            String line;
            int lineNum = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNum++;
                
                // 빈 줄이나 주석 무시
                if (line.trim().isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                System.out.println("[CheckRoomStatus] 라인 " + lineNum + ": " + line);
                
                String[] tokens = line.split(",");
                if (tokens.length >= 2) {
                    String room = tokens[0].trim();
                    String status = tokens[1].trim();
                    
                    // 강의실 번호 비교 (대소문자 무시)
                    if (room.equalsIgnoreCase(roomNumber)) {
                        if ("사용불가".equals(status)) {
                            System.out.println("[CheckRoomStatus] " + roomNumber + " → UNAVAILABLE (사용불가)");
                            return "UNAVAILABLE";
                        } else {
                            System.out.println("[CheckRoomStatus] " + roomNumber + " → AVAILABLE (상태: " + status + ")");
                            return "AVAILABLE";
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[CheckRoomStatus] 파일 읽기 오류: " + e.getMessage());
            e.printStackTrace();
            return "CHECK_FAILED";
        }
        
        // 파일에 없으면 사용 가능
        System.out.println("[CheckRoomStatus] " + roomNumber + " → AVAILABLE (파일에 없음)");
        return "AVAILABLE";
    }
}