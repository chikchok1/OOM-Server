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

public class CountPendingRequestCommand implements Command {

    private final String BASE_DIR;

    public CountPendingRequestCommand(String baseDir) {
        this.BASE_DIR = baseDir;
    }

    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) throws IOException, InvalidInputException, DatabaseException, AuthenticationException, BusinessLogicException {
        File file = new File(BASE_DIR + "/ReservationRequest.txt");
        int count = 0;

        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        count++;
                    }
                }
            } catch (IOException e) {
                return "ERROR_COUNTING_REQUEST";
            }
        }

        return "PENDING_COUNT:" + count;
    }
}
