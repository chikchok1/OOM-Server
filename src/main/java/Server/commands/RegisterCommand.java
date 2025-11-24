package Server.commands;

import Server.UserDAO;
import Server.exceptions.*;
import common.model.User;
import java.io.*;

public class RegisterCommand implements Command {

    private final UserDAO userDAO;

    public RegisterCommand(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Override
    public String execute(String[] params, BufferedReader in, PrintWriter out) 
            throws IOException, InvalidInputException, DatabaseException, 
                   AuthenticationException, BusinessLogicException {
        
        // 입력 검증
        if (params.length != 4) {
            throw new InvalidInputException("회원가입 명령은 4개의 매개변수가 필요합니다");
        }

        String name = params[1];
        String userId = params[2];
        String password = params[3];

        // 입력 유효성 검사
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidInputException("name", name, "이름을 입력해주세요");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new InvalidInputException("userId", userId, "아이디를 입력해주세요");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new InvalidInputException("password", "", "비밀번호를 입력해주세요");
        }

        // 중복 확인
        if (userDAO.isUserIdExists(userId)) {
            throw new BusinessLogicException(
                    BusinessLogicException.BusinessRuleViolation.DUPLICATE_REGISTRATION,
                    "이미 존재하는 아이디입니다: " + userId
            );
        }

        // 사용자 등록
        try {
            userDAO.registerUser(new User(userId, password, name));
            return "SUCCESS";
        } catch (Exception e) {
            throw new DatabaseException(
                    "users.txt",
                    DatabaseException.OperationType.WRITE,
                    "사용자 등록 중 오류가 발생했습니다",
                    e
            );
        }
    }
}
