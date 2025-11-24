/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Server.commands;

/**
 *
 * @author YangJinWon
 */
import java.io.*;
import Server.exceptions.*;

public class ViewReservationCommand implements Command {

    private final String BASE_DIR;
    private final Object FILE_LOCK;

    public ViewReservationCommand(String baseDir, Object fileLock) {
        this.BASE_DIR = baseDir;
        this.FILE_LOCK = fileLock;
    }

    @Override

    public String execute(String[] params, BufferedReader in, PrintWriter out) throws IOException, InvalidInputException, DatabaseException, AuthenticationException, BusinessLogicException {
        boolean hasDateRange = params.length == 5;
        if (params.length != 3 && params.length != 5) {

            return "INVALID_VIEW_FORMAT";
        }

        String requesterId = params[1].trim();
        String roomName = params[2].trim();
        
        String weekStart = hasDateRange ? params[3].trim() : null;
        String weekEnd = hasDateRange ? params[4].trim() : null;
        
        System.out.println("[ViewReservation] 요청: room=" + roomName + 
            (hasDateRange ? ", 기간: " + weekStart + " ~ " + weekEnd : ""));

        synchronized (FILE_LOCK) {
            String file1 = BASE_DIR + "/ReserveClass.txt";
            String file2 = BASE_DIR + "/ReserveLab.txt";
            String file3 = BASE_DIR + "/ReservationRequest.txt";

            try {
                for (String filePath : new String[]{file1, file2}) {
                    File confirmedFile = new File(filePath);
                    if (!confirmedFile.exists()) {
                        continue;
                    }

                    try (BufferedReader reader = new BufferedReader(new FileReader(confirmedFile))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String[] tokens = line.split(",");
                            // ✅ 9개 필드로 수정 (name,room,date,day,time,purpose,role,status,studentCount)
                            if (tokens.length < 9) {
                                continue;
                            }

                            String fileRoom = tokens[1].trim();
                            if (!fileRoom.equals(roomName)
                                    && !fileRoom.equals(roomName + "호")
                                    && !(fileRoom + "호").equals(roomName)) {
                                continue;
                            }
                            
                            // ✅ 날짜 필터링 (주간 범위가 지정된 경우)
                            if (hasDateRange && tokens.length >= 3) {
                                try {
                                    String reservationDate = tokens[2].trim();
                                    if (reservationDate.compareTo(weekStart) < 0 || 
                                        reservationDate.compareTo(weekEnd) > 0) {
                                        continue; // 범위 밖 예약은 제외
                                    }
                                } catch (Exception e) {
                                    System.err.println("[날짜 파싱 오류] " + tokens[2]);
                                }
                            }
                            
                            out.println(line);
                        }
                    }
                }

                File pendingFile = new File(file3);
                if (pendingFile.exists()) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(pendingFile))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String[] tokens = line.split(",");
                            // ✅ 9개 필드로 수정
                            if (tokens.length < 9) {
                                continue;
                            }

                            String fileRoom = tokens[1].trim();
                            if (!fileRoom.equals(roomName)
                                    && !fileRoom.equals(roomName + "호")
                                    && !(fileRoom + "호").equals(roomName)) {
                                continue;
                            }
                            
                            // ✅ 날짜 필터링 (주간 범위가 지정된 경우)
                            if (hasDateRange && tokens.length >= 3) {
                                try {
                                    String reservationDate = tokens[2].trim();
                                    if (reservationDate.compareTo(weekStart) < 0 || 
                                        reservationDate.compareTo(weekEnd) > 0) {
                                        continue; // 범위 밖 예약은 제외
                                    }
                                } catch (Exception e) {
                                    System.err.println("[날짜 파싱 오류] " + tokens[2]);
                                }
                            }
                            
                            out.println(line);
                        }
                    }
                }

            } catch (IOException e) {
                out.println("ERROR_READING_RESERVATION_FILE");
                out.println("END_OF_RESERVATION");
                out.flush();
            } finally {
                System.out.println("예약 정보 전송 종료: END_OF_RESERVATION");
                out.println("END_OF_RESERVATION");
                out.flush();
            }
        }

        return null; // 이미 out으로 직접 출력했으므로 null 반환
    }
}
