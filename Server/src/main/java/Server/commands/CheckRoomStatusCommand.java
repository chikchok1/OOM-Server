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

public class CheckRoomStatusCommand implements Command {
    private final String BASE_DIR;
    private final String ROOM_STATUS_FILE;

    public CheckRoomStatusCommand(String baseDir) {
        this.BASE_DIR = baseDir;
        this.ROOM_STATUS_FILE = BASE_DIR + File.separator + "RoomStatus.txt";
    }

    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) throws IOException {
        if (params.length != 2) {
            return "INVALID_CHECK_FORMAT";
        }

        String roomNumber = params[1].trim();
        boolean available = true;
        boolean found = false;

        File statusFile = new File(ROOM_STATUS_FILE);

        if (statusFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(statusFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] tokens = line.split(",");
                    if (tokens.length >= 2 && tokens[0].trim().equals(roomNumber)) {
                        found = true;
                        if ("사용불가".equals(tokens[1].trim())) {
                            available = false;
                        }
                        break;
                    }
                }
            } catch (IOException e) {
                return "CHECK_FAILED";
            }
        }

        String result = found ? (available ? "AVAILABLE" : "UNAVAILABLE") : "AVAILABLE";
        return result;
    }
}
