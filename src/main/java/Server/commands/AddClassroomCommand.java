package Server.commands;

import Server.manager.ServerClassroomManager;
import common.dto.ClassroomDTO;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class AddClassroomCommand implements Command {
    @Override
    public String execute(String[] args, BufferedReader in, PrintWriter out) throws IOException {
        try {
            // args: [ADD_CLASSROOM, roomName, type, capacity]
            if (args.length < 4) {
                return "FAIL|잘못된 요청 형식입니다.";
            }

            String roomName = args[1];
            String type = args[2]; // CLASS or LAB
            int capacity = Integer.parseInt(args[3]);

            // 검증
            if (roomName == null || roomName.trim().isEmpty()) {
                return "FAIL|강의실 이름을 입력해주세요.";
            }

            if (!type.equals("CLASS") && !type.equals("LAB")) {
                return "FAIL|올바른 타입을 선택해주세요. (CLASS 또는 LAB)";
            }

            if (capacity <= 0) {
                return "FAIL|수용인원은 1명 이상이어야 합니다.";
            }

            // 중복 체크
            ServerClassroomManager manager = ServerClassroomManager.getInstance();
            if (manager.classroomExists(roomName)) {
                return "FAIL|이미 존재하는 강의실/실습실입니다.";
            }

            // ClassroomDTO 생성
            ClassroomDTO classroom = new ClassroomDTO(roomName, type, capacity);

            // 추가
            boolean success = manager.addClassroom(classroom);

            if (success) {
                System.out.println("[서버] 강의실 추가 성공: " + roomName + " (" + type + ", " + capacity + "명)");
                return "SUCCESS|강의실이 추가되었습니다.";
            } else {
                return "FAIL|강의실 추가에 실패했습니다.";
            }

        } catch (NumberFormatException e) {
            return "FAIL|수용인원은 숫자여야 합니다.";
        } catch (Exception e) {
            e.printStackTrace();
            return "FAIL|서버 오류가 발생했습니다: " + e.getMessage();
        }
    }
}
