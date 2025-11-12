package Server.commands;

import java.io.*;

/**
 * ✅ 특정 강의실, 날짜, 시간에 예약된 총 인원 수를 계산
 */
public class GetReservedCountByDateCommand implements Command {

    private final String BASE_DIR;

    public GetReservedCountByDateCommand(String baseDir) {
        this.BASE_DIR = baseDir;
    }

    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) throws IOException {
        // 형식: GET_RESERVED_COUNT_BY_DATE,room,dateString,time
        if (params.length != 4) {
            out.println("INVALID_FORMAT");
            out.flush();
            return null;
        }

        String room = params[1].trim();
        String dateString = params[2].trim();  // "2025-11-12" 형식
        String time = params[3].trim();

        int totalReserved = calculateReservedCountByDate(room, dateString, time);

        out.println("RESERVED_COUNT:" + totalReserved);
        out.flush();

        System.out.printf("[GetReservedCountByDateCommand] %s %s %s -> 예약인원: %d명%n",
                room, dateString, time, totalReserved);

        return null;
    }

    /**
     * 승인된 예약(ReserveClass/Lab.txt)에서 해당 날짜/시간대 예약 인원 합계 반환
     * 형식: 이름,방,날짜,요일,시간,목적,권한,상태,학생수
     */
    private int calculateReservedCountByDate(String room, String dateString, String time) {
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
                    // 형식: name,room,dateString,day,time,purpose,role,status,studentCount (9개)
                    if (parts.length >= 9) {
                        String fileRoom = parts[1].trim();
                        String fileDateString = parts[2].trim();
                        String fileTime = parts[4].trim();  // 시간은 5번째 (index 4)
                        String status = parts[7].trim();

                        if (fileRoom.equals(room)
                                && fileDateString.equals(dateString)
                                && fileTime.equals(time)
                                && (status.equals("예약됨") || status.equals("승인"))) {

                            try {
                                int count = Integer.parseInt(parts[8].trim());
                                totalCount += count;
                                System.out.printf("[누적] %s %s %s → +%d명 (현재합계=%d명)%n",
                                        room, dateString, time, count, totalCount);
                            } catch (NumberFormatException ignored) {
                                System.err.println("[WARN] 학생 수 파싱 실패: " + line);
                            }
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
