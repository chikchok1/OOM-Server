package Server.commands;

import Server.UserDAO;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 기본 구현: 각 커맨드 타입별로 실제 Command 인스턴스를 생성한다.
 */
public class DefaultCommandFactory extends CommandFactory {

    public DefaultCommandFactory(UserDAO userDAO,
            String baseDir,
            Object fileLock,
            ConcurrentHashMap<String, Socket> loggedInUsers,
            AtomicInteger currentClients,
            int maxClients) {
        super(userDAO, baseDir, fileLock, loggedInUsers, currentClients, maxClients);
    }

    @Override
    protected Command createCommandInstance(String commandType, String request, String currentUserId) {
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
            default ->
                null;
        };
    }
}
