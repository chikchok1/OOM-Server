package Server.commands;

import Server.UserDAO;
import java.io.*;
import Server.exceptions.*;

public class ViewApprovedReservationsCommand implements Command {

    private final String BASE_DIR;
    private final UserDAO userDAO;
    private final String currentUserId;

    public ViewApprovedReservationsCommand(String baseDir, UserDAO userDAO, String currentUserId) {
        this.BASE_DIR = baseDir;
        this.userDAO = userDAO;
        this.currentUserId = currentUserId;
    }

    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) throws IOException, InvalidInputException, DatabaseException, AuthenticationException, BusinessLogicException {
        // ✅ currentUserId 사용 (클라이언트가 보낸 userId 무시)
        String userId = currentUserId;
        
        if (userId == null || userId.isEmpty()) {
            out.println("ERROR: User not authenticated");
            out.flush();
            return "VIEW_APPROVED_RESERVATIONS_FAILED";
        }

        try {
            String userName = userDAO.getUserNameById(userId);
            System.out.println("[ViewApprovedReservationsCommand] userId=" + userId + ", userName=" + userName);
            if (userName == null) {
                System.err.println("[ViewApprovedReservationsCommand] 사용자 이름을 찾을 수 없음");
                out.println("ERROR: User not found");
                out.flush();
                return "VIEW_APPROVED_RESERVATIONS_FAILED";
            }

            File classFile = new File(BASE_DIR + "/ReserveClass.txt");
            File labFile = new File(BASE_DIR + "/ReserveLab.txt");

            System.out.println("[ViewApprovedReservationsCommand] 파일 확인:");
            System.out.println("  - ReserveClass.txt exists: " + classFile.exists() + ", path: " + classFile.getAbsolutePath());
            System.out.println("  - ReserveLab.txt exists: " + labFile.exists() + ", path: " + labFile.getAbsolutePath());

            int count = 0;

            // ✅ 강의실 예약 읽기
            if (classFile.exists()) {
                System.out.println("[ViewApprovedReservationsCommand] ReserveClass.txt 읽기 시작...");
                try (BufferedReader reader = new BufferedReader(new FileReader(classFile))) {
                    String line;
                    int lineNum = 0;
                    while ((line = reader.readLine()) != null) {
                        lineNum++;
                        System.out.println("[ViewApprovedReservationsCommand] 라인 " + lineNum + ": " + line);

                        String[] parts = line.split(",");
                        // ✅ 파일 구조: 이름,방,날짜,요일,시간,목적,역할,상태,학생수,아이디
                        if (parts.length >= 10) {
                            String reservedUserName = parts[0].trim();
                            String reservedRoom = parts[1].trim();
                            String reservedDate = parts[2].trim();
                            String reservedDay = parts[3].trim();
                            String reservedTime = parts[4].trim();
                            String purpose = parts[5].trim();
                            String role = parts[6].trim();
                            String status = parts[7].trim();
                            String studentCount = parts[8].trim();
                            String reservedUserId = (parts.length > 9) ? parts[9].trim() : "";

                            System.out.println("  - 예약자 이름: " + reservedUserName + ", ID: " + reservedUserId + ", 상태: " + status);
                            System.out.println("  - 비교: '" + reservedUserId + "' == '" + userId + "' ? " + reservedUserId.equals(userId));

                            // 본인 + (승인된 예약 또는 대기중인 예약) 출력
                            if (reservedUserId.equals(userId) && (status.equals("예약됨") || status.equals("대기중"))) {
                                String output = "CLASS," + line;
                                out.println(output);
                                count++;
                            }
                        } else {
                            System.err.println("  ⚠️ 잘못된 형식 (parts.length=" + parts.length + "): " + line);
                        }
                    }
                }
                System.out.println("[ViewApprovedReservationsCommand] ReserveClass.txt 읽기 완료. 총 " + count + "개 발견");
            }

            // ✅ 실습실 예약 읽기
            if (labFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(labFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split(",");
                        if (parts.length >= 10) {
                            String status = parts[7].trim();
                            String reservedUserId = (parts.length > 9) ? parts[9].trim() : "";
                            if (reservedUserId.equals(userId) && (status.equals("예약됨") || status.equals("대기중"))) {
                                out.println("LAB," + line);
                                count++;
                            }
                        }
                    }
                }
            }

            out.println("END_OF_APPROVED_RESERVATIONS");
            out.flush();

            System.out.println("[ViewApprovedReservationsCommand] " + userName + "의 승인된 예약 " + count + "개 전송");
            return "VIEW_APPROVED_RESERVATIONS_SUCCESS";

        } catch (IOException e) {
            System.err.println("[ViewApprovedReservationsCommand] 오류: " + e.getMessage());
            out.println("ERROR: " + e.getMessage());
            out.flush();
            return "VIEW_APPROVED_RESERVATIONS_FAILED";
        }
    }
}
