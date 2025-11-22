package Server.commands;

import Server.manager.ServerClassroomManager;

import java.io.*;
import Server.exceptions.*;

public class DeleteClassroomCommand implements Command {
    @Override
    public String execute(String[] args, BufferedReader in, PrintWriter out) throws IOException, InvalidInputException, DatabaseException, AuthenticationException, BusinessLogicException {
        try {
            // args: [DELETE_CLASSROOM, roomName]
            if (args.length < 2) {
                return "FAIL|잘못된 요청 형식입니다.";
            }

            String roomName = args[1];

            // 검증
            if (roomName == null || roomName.trim().isEmpty()) {
                return "FAIL|삭제할 강의실 이름을 선택해주세요.";
            }

            ServerClassroomManager manager = ServerClassroomManager.getInstance();

            // 존재 여부 확인
            if (!manager.classroomExists(roomName)) {
                return "FAIL|존재하지 않는 강의실/실습실입니다.";
            }

            // 예약 건 확인
            if (hasReservations(roomName)) {
                return "FAIL|해당 강의실에 예약 건이 존재합니다. 예약을 먼저 취소해주세요.";
            }

            // 삭제
            boolean success = manager.deleteClassroom(roomName);

            if (success) {
                System.out.println("[서버] 강의실 삭제 성공: " + roomName);
                return "SUCCESS|강의실이 삭제되었습니다.";
            } else {
                return "FAIL|강의실 삭제에 실패했습니다.";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "FAIL|서버 오류가 발생했습니다: " + e.getMessage();
        }
    }

    // 예약 건 확인 메서드
    private boolean hasReservations(String roomName) {
        // ReserveClass.txt와 ReserveLab.txt 파일에서 해당 강의실 예약 확인
        return checkFileForReservations("data/ReserveClass.txt", roomName) ||
               checkFileForReservations("data/ReserveLab.txt", roomName);
    }

    private boolean checkFileForReservations(String filePath, String roomName) {
        File file = new File(filePath);
        if (!file.exists()) {
            return false;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length > 0 && parts[0].equals(roomName)) {
                    return true; // 예약 건 존재
                }
            }
        } catch (IOException e) {
            System.err.println("예약 파일 확인 중 오류: " + e.getMessage());
        }
        return false;
    }
}
