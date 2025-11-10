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
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class LoginCommand implements Command {

    private final UserDAO userDAO;
    private final ConcurrentHashMap<String, Socket> loggedInUsers;
    private final AtomicInteger currentClients;
    private final int MAX_CLIENTS;

    public LoginCommand(UserDAO userDAO,
            ConcurrentHashMap<String, Socket> loggedInUsers,
            AtomicInteger currentClients,
            int maxClients) {
        this.userDAO = userDAO;
        this.loggedInUsers = loggedInUsers;
        this.currentClients = currentClients;
        this.MAX_CLIENTS = maxClients;
    }

    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) throws IOException {
        if (params.length != 3) {
            return "INVALID_FORMAT";
        }

        String userId = params[1];
        String password = params[2];

        System.out.println("로그인 시도: " + userId);

        synchronized (LoginCommand.class) {
            if (currentClients.get() >= MAX_CLIENTS) {
                System.out.println("접속 초과로 로그인 실패: " + userId);
                return "SERVER_BUSY";
            }

            if (loggedInUsers.containsKey(userId)) {
                System.out.println("중복 로그인 시도: " + userId);
                Socket oldSocket = loggedInUsers.get(userId);
                try {
                    if (oldSocket != null && !oldSocket.isClosed()) {
                        oldSocket.close();
                    }
                } catch (IOException ignored) {
                }
                return "ALREADY_LOGGED_IN";
            }

            boolean valid = userDAO.validateUser(userId, password);
            System.out.println("유효성 검사 결과: " + valid);

            if (!valid) {
                return "FAIL";
            }

            String name = userDAO.getUserNameById(userId);
            System.out.println(userId + " 로그인 성공");

            return "SUCCESS," + name;
        }
    }
}
