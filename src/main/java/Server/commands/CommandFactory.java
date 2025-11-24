package Server.commands;

import Server.UserDAO;
import Server.commands.*;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class CommandFactory {

    private final UserDAO userDAO;
    private final String BASE_DIR;
    private final Object FILE_LOCK;
    private final ConcurrentHashMap<String, Socket> loggedInUsers;
    private final AtomicInteger currentClients;
    private final int MAX_CLIENTS;

    public CommandFactory(UserDAO userDAO,
            String baseDir,
            Object fileLock,
            ConcurrentHashMap<String, Socket> loggedInUsers,
            AtomicInteger currentClients,
            int maxClients) {
        this.userDAO = userDAO;
        this.BASE_DIR = baseDir;
        this.FILE_LOCK = fileLock;
        this.loggedInUsers = loggedInUsers;
        this.currentClients = currentClients;
        this.MAX_CLIENTS = maxClients;

    }

    public Command createCommand(String request, String currentUserId) {
        if (request == null || request.isEmpty()) {
            return null;
        }

        String commandType = request.split(",")[0];

        return switch (commandType) {
            case "LOGIN" ->
                new LoginCommand(userDAO, loggedInUsers, currentClients, MAX_CLIENTS);
            case "REGISTER" ->
                new RegisterCommand(userDAO);
            case "RESERVE_REQUEST" ->
                new ReserveRequestCommand(BASE_DIR, FILE_LOCK);
            case "VIEW_RESERVATION" ->
                new ViewReservationCommand(BASE_DIR, FILE_LOCK);
            case "CANCEL_RESERVATION" ->
                new CancelReservationCommand(BASE_DIR, FILE_LOCK);
            case "CHANGE_RESERVATION" ->
                new ChangeReservationCommand(BASE_DIR, FILE_LOCK);
            case "APPROVE_RESERVATION" ->
                new ApproveReservationCommand(BASE_DIR, FILE_LOCK, userDAO, currentUserId);
            case "REJECT_RESERVATION" ->
                new RejectReservationCommand(BASE_DIR, FILE_LOCK, userDAO, currentUserId);
            case "COUNT_PENDING_REQUEST" ->
                new CountPendingRequestCommand(BASE_DIR);
            case "GET_ALL_USERS" ->
                new GetAllUsersCommand(BASE_DIR, userDAO, currentUserId);
            case "DELETE_USER" ->
                new DeleteUserCommand(BASE_DIR, FILE_LOCK, userDAO, currentUserId);
            case "UPDATE_USER" ->
                new UpdateUserCommand(BASE_DIR, FILE_LOCK, userDAO, currentUserId);
            case "VIEW_ALL_RESERVATIONS" ->
                new ViewAllReservationsCommand(BASE_DIR, userDAO, currentUserId);
            case "VIEW_MY_RESERVATIONS" ->
                new ViewMyReservationsCommand(BASE_DIR, userDAO, currentUserId);
            case "CHECK_ROOM_TIME" ->
                new CheckRoomTimeCommand(BASE_DIR, FILE_LOCK);
            case "CHANGE_PASSWORD" ->
                new ChangePasswordCommand(userDAO);
            case "GET_RESERVATION_REQUESTS" ->
                new GetReservationRequestsCommand(BASE_DIR, userDAO, currentUserId);
            case "UPDATE_ROOM_STATUS" ->
                new UpdateRoomStatusCommand(BASE_DIR, userDAO, currentUserId);
            case "CHECK_ROOM_STATUS" ->
                new CheckRoomStatusCommand(BASE_DIR);
            case "GET_RESERVED_COUNT" ->
                new GetReservedCountCommand(BASE_DIR);
            case "GET_RESERVED_COUNT_BY_DATE" ->
                new GetReservedCountByDateCommand(BASE_DIR);
            case "VIEW_WEEKLY_RESERVATION" ->
                new ViewWeeklyReservationCommand(BASE_DIR, FILE_LOCK);
            case "GET_ROOM_INFO" ->
                new GetRoomInfoCommand();
            case "GET_CLASSROOMS" ->
                new GetClassroomsCommand();
            case "GET_LABS" ->
                new GetLabsCommand();
            case "UPDATE_ROOM_CAPACITY" ->
                new UpdateRoomCapacityCommand(userDAO, currentUserId);
            case "VIEW_APPROVED_RESERVATIONS" ->
                new ViewApprovedReservationsCommand(BASE_DIR, userDAO, currentUserId);
            case "CHANGE_RESERVATION_FULL" ->
                new ChangeReservationFullCommand(BASE_DIR, FILE_LOCK);
            case "ADD_CLASSROOM" ->
                new AddClassroomCommand();
            case "DELETE_CLASSROOM" ->
                new DeleteClassroomCommand();
            // TEST_ROLLBACK 명령은 테스트용으로 프로덕션에서 제거됨
            // 테스트는 src/test/java에서 직접 실행
            default ->
                null;
        };
    }
    // 기존 메서드 (currentUserId 없이 호출하면 null 전달)

    public Command createCommand(String request) {
        return createCommand(request, null);
    }
}
