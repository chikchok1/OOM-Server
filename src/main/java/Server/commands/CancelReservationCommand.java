package Server.commands;

import java.io.*;

public class CancelReservationCommand implements Command {

    private final String BASE_DIR;
    private final Object FILE_LOCK;

    public CancelReservationCommand(String baseDir, Object fileLock) {
        this.BASE_DIR = baseDir;
        this.FILE_LOCK = fileLock;
    }

    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) throws IOException {
        //  날짜 포함해서 총 7개 파라미터로 변경
        if (params.length != 7) {
            return "INVALID_CANCEL_FORMAT";
        }

        String cancelUserId = params[1].trim();
        String day = params[2].trim();      // 요일
        String date = params[3].trim();     // 날짜
        String time = params[4].trim();     // 교시
        String room = params[5].trim();
        String userName = params[6].trim();

        synchronized (FILE_LOCK) {
            String targetFile = (room.equals("908호") || room.equals("912호")
                    || room.equals("913호") || room.equals("914호"))
                    ? BASE_DIR + "/ReserveClass.txt"
                    : BASE_DIR + "/ReserveLab.txt";

            File inputFile = new File(targetFile);
            File tempFile = new File(targetFile + ".tmp");
            boolean deleted = false;
            int canceledStudentCount = 0;

            try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
                 BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] lineParts = line.split(",");
                    if (lineParts.length >= 9
                            && lineParts[0].trim().equals(userName)
                            && lineParts[1].trim().equals(room)
                            && lineParts[2].trim().equals(date)
                            && lineParts[3].trim().equals(day)
                            && lineParts[4].trim().equals(time)) {

                        // 학생 수 추출
                        try {
                            canceledStudentCount = Integer.parseInt(lineParts[8].trim());
                        } catch (NumberFormatException e) {
                            canceledStudentCount = 1;
                        }
                        deleted = true;
                        System.out.println("[DEBUG] 예약 취소: " + line);
                        continue;
                    }
                    writer.write(line);
                    writer.newLine();
                }
            }

            if (deleted) {
                inputFile.delete();
                tempFile.renameTo(inputFile);
                System.out.println("[취소] " + room + " " + day + " " + time + " - 학생 수: " + canceledStudentCount + "명");
                return "CANCEL_SUCCESS";
            } else {
                tempFile.delete();
                return "CANCEL_FAILED_NOT_FOUND";
            }
        }
    }
}
