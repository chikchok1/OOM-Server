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

public class CheckRoomTimeCommand implements Command {
    private final String BASE_DIR;
    private final Object FILE_LOCK;

    public CheckRoomTimeCommand(String baseDir, Object fileLock) {
        this.BASE_DIR = baseDir;
        this.FILE_LOCK = fileLock;
    }

    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) throws IOException, InvalidInputException, DatabaseException, AuthenticationException, BusinessLogicException {
        if (params.length != 4) {
            return "INVALID_CHECK_FORMAT";
        }

        String room = params[1].trim();
        String day = params[2].trim();
        String time = params[3].trim();

        synchronized (FILE_LOCK) {
            File[] files = {
                new File(BASE_DIR + "/ReserveClass.txt"),
                new File(BASE_DIR + "/ReserveLab.txt")
            };

            boolean conflict = false;

            for (File file : files) {
                if (!file.exists()) continue;

                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] tokens = line.split(",");
                        if (tokens.length >= 7 &&
                            tokens[1].trim().equals(room.trim()) &&
                            tokens[2].trim().equals(day.trim()) &&
                            tokens[3].trim().equals(time.trim()) &&
                            tokens[6].trim().equals("예약됨")) {
                            conflict = true;
                            break;
                        }
                    }
                } catch (IOException e) {
                    return "CHECK_ROOM_TIME_ERROR";
                }

                if (conflict) break;
            }

            return conflict ? "CONFLICT" : "NO_CONFLICT";
        }
    }
}