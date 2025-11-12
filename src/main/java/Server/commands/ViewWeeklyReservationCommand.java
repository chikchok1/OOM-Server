package Server.commands;

import java.io.*;

public class ViewWeeklyReservationCommand implements Command {
    private final String BASE_DIR;
    private final Object FILE_LOCK;

    public ViewWeeklyReservationCommand(String baseDir, Object fileLock) {
        this.BASE_DIR = baseDir;
        this.FILE_LOCK = fileLock;
    }

    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) throws IOException {
        if (params.length != 4) {
            out.println("INVALID_VIEW_FORMAT");
            out.println("END_OF_RESERVATION");
            out.flush();
            return null;
        }

        String roomName = params[1].trim();
        String weekStart = params[2].trim();
        String weekEnd = params[3].trim();

        System.out.printf("[ViewWeeklyReservationCommand] %s: %s ~ %s%n", 
            roomName, weekStart, weekEnd);

        synchronized (FILE_LOCK) {
            String[] files = {
                BASE_DIR + "/ReserveClass.txt",
                BASE_DIR + "/ReserveLab.txt",
                BASE_DIR + "/ReservationRequest.txt"
            };

            int count = 0;

            try {
                for (String filePath : files) {
                    File file = new File(filePath);
                    if (!file.exists()) continue;

                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String[] tokens = line.split(",");
                            // ✅ 9개 필드로 수정 (name,room,date,day,time,purpose,role,status,studentCount)
                            if (tokens.length < 9) continue;

                            String fileRoom = tokens[1].trim();
                            String fileDateString = tokens[2].trim();
                            
                            boolean roomMatch = fileRoom.equals(roomName)
                                    || fileRoom.equals(roomName + "호")
                                    || (fileRoom + "호").equals(roomName);
                            
                            if (!roomMatch) continue;

                            if (fileDateString.compareTo(weekStart) >= 0 
                                    && fileDateString.compareTo(weekEnd) <= 0) {
                                out.println(line);
                                count++;
                            }
                        }
                    }
                }

                System.out.printf("[ViewWeeklyReservationCommand] %s - %d개 전송%n", 
                    roomName, count);

            } catch (IOException e) {
                System.err.println("[ERROR] 파일 읽기 오류: " + e.getMessage());
                out.println("ERROR_READING_RESERVATION_FILE");
            } finally {
                out.println("END_OF_RESERVATION");
                out.flush();
            }
        }
        return null;
    }
}
