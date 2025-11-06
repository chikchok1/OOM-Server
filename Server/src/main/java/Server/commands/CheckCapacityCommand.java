package Server.commands;

import common.manager.ClassroomManager;
import java.io.*;

/**
 * 수용 인원 체크 명령어 (50% 제한 적용)
 * 형식: CHECK_CAPACITY,강의실명,요청인원
 * 응답: CAPACITY_OK 또는 CAPACITY_EXCEEDED:최대허용인원
 */
public class CheckCapacityCommand implements Command {
    
    @Override
    public String execute(String[] parts, BufferedReader in, PrintWriter out) throws IOException {
        try {
            if (parts.length < 3) {
                out.println("ERROR:잘못된 요청 형식");
                out.flush();
                return "ERROR";
            }
            
            String roomName = parts[1].trim();
            int requestedCount = Integer.parseInt(parts[2].trim());
            
            ClassroomManager manager = ClassroomManager.getInstance();
            
            // 강의실 존재 여부 확인
            if (!manager.exists(roomName)) {
                out.println("ERROR:존재하지 않는 강의실");
                out.flush();
                System.err.println("[CheckCapacityCommand] 존재하지 않는 강의실: " + roomName);
                return "ERROR";
            }
            
            // 수용 인원 체크
            boolean isAllowed = manager.checkCapacity(roomName, requestedCount);
            
            if (isAllowed) {
                out.println("CAPACITY_OK");
                out.flush();
                System.out.println(String.format("[CheckCapacityCommand] %s: %d명 예약 가능", roomName, requestedCount));
            } else {
                ClassroomManager.Classroom classroom = manager.getClassroom(roomName);
                int allowedCapacity = classroom.getAllowedCapacity();
                out.println("CAPACITY_EXCEEDED:" + allowedCapacity);
                out.flush();
                System.out.println(String.format("[CheckCapacityCommand] %s: %d명 예약 불가 (최대 %d명)", 
                    roomName, requestedCount, allowedCapacity));
            }
            
            return "SUCCESS";
            
        } catch (NumberFormatException e) {
            out.println("ERROR:잘못된 인원 수");
            out.flush();
            System.err.println("[CheckCapacityCommand] 잘못된 인원 수: " + e.getMessage());
            return "ERROR";
        } catch (Exception e) {
            out.println("ERROR:수용 인원 체크 실패");
            out.flush();
            System.err.println("[CheckCapacityCommand] 오류: " + e.getMessage());
            return "ERROR";
        }
    }
}
