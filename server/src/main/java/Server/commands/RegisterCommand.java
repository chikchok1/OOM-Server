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
import common.model.User;
import java.io.*;

public class RegisterCommand implements Command {

    private final UserDAO userDAO;

    public RegisterCommand(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) throws IOException {
        if (params.length != 4) {
            return "INVALID_FORMAT";
        }

        String name = params[1];
        String userId = params[2];
        String password = params[3];

        if (userDAO.isUserIdExists(userId)) {
            return "DUPLICATE";
        } else {
            userDAO.registerUser(new User(userId, password, name));
            return "SUCCESS";
        }
    }
}
