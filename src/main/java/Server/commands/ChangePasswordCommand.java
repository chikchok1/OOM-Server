/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Server.commands;

/**
 *
 * @author YangJinWon
 */
import Server.UserDAO;
import java.io.*;

public class ChangePasswordCommand implements Command {
    private final UserDAO userDAO;

    public ChangePasswordCommand(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) throws IOException {
        if (params.length != 4) {
            return "INVALID_PASSWORD_CHANGE_FORMAT";
        }

        String userId = params[1].trim();
        String currentPassword = params[2].trim();
        String newPassword = params[3].trim();

        boolean isValid = userDAO.validateUser(userId, currentPassword);

        if (!isValid) {
            return "INVALID_CURRENT_PASSWORD";
        } else {
            boolean isChanged = userDAO.updatePassword(userId, newPassword);
            return isChanged ? "PASSWORD_CHANGED" : "USER_NOT_FOUND";
        }
    }
}