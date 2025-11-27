package Server.commands;

import java.io.*;
import Server.exceptions.*;
/**
 * 특정 강의실, 요일, 시간에 예약된 총 인원 수를 계산
 */
public class GetReservedCountCommand implements Command {

    private final String BASE_DIR;

    public GetReservedCountCommand(String baseDir) {
        this.BASE_DIR = baseDir;
    }

    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) throws IOException, InvalidInputException, DatabaseException, AuthenticationException, BusinessLogicException {
        // 형식: GET_RESERVED_COUNT,room,day,time
        if (params.length != 4) {
            out.println("INVALID_FORMAT");
            out.flush();
            return null;
        }

        String room = params[1].trim();
        String day = params[2].trim();
        String time = params[3].trim();

        int totalReserved = calculateReservedCount(room, day, time);

        out.println("RESERVED_COUNT:" + totalReserved);
        out.flush();

        System.out.printf("[GetReservedCountCommand] %s %s %s -> 예약인원: %d명%n",
                room, day, time, totalReserved);

        return null;
    }

    /**
     * 승인된 예약(ReserveClass/Lab.txt)에서 해당 시간대 예약 인원 합계 반환
     */
    private int calculateReservedCount(String room, String day, String time) {
        String[] files = {
                BASE_DIR + "/ReserveClass.txt",
                BASE_DIR + "/ReserveLab.txt"
        };

        int totalCount = 0;

        for (String filePath : files) {
            File file = new File(filePath);
            if (!file.exists()) continue;

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    // 형식: name,room,day,time,purpose,role,status,studentCount
                    if (parts.length >= 8) {
                        String fileRoom = parts[1].trim();
                        String fileDay = parts[2].trim();
                        String fileTime = parts[3].trim();
                        String status = parts[6].trim();

                        if (fileRoom.equals(room)
                                && fileDay.equals(day)
                                && fileTime.equals(time)
                                && (status.equals("예약됨") || status.equals("승인"))) {

                            try {
                                int count = Integer.parseInt(parts[7].trim());
                                totalCount += count;
                                System.out.printf("[누적] %s %s %s → +%d명 (현재합계=%d명)%n",
                                        room, day, time, count, totalCount);
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("[ERROR] 파일 읽기 오류: " + filePath + " - " + e.getMessage());
            }
        }

        return totalCount;
    }
}
