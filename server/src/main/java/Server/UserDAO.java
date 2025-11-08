package Server;

import common.model.User;
import java.io.*;

public class UserDAO {

    private static final String BASE_DIR = System.getProperty("user.dir") + File.separator + "data";

    private static final String USER_FILE = BASE_DIR + "/users.txt";
    private static final String PROF_FILE = BASE_DIR + "/prof.txt";
    private static final String ASSISTANT_FILE = BASE_DIR + "/assistant.txt";
    private static final String RESERVE_CLASS_FILE = BASE_DIR + "/ReserveClass.txt";
    private static final String RESERVE_LAB_FILE = BASE_DIR + "/ReserveLab.txt";
    private static final String CHANGE_REQUEST = BASE_DIR + "/ChangeRequest.txt";
    private static final String ROOM_STATUS = BASE_DIR + "/RoomStatus.txt";
    private static final String RESERVATION_REQUEST = BASE_DIR + "/ReservationRequest.txt";
    private static final String APPROVED_BACKUP = BASE_DIR + "/ApprovedBackUp.txt";
    private static final String TEMP1 = BASE_DIR + "/temp1.txt";
    private static final String TEMP2 = BASE_DIR + "/temp2.txt";
    private static final String CLASSROOMS_FILE = BASE_DIR + "/Classrooms.txt";

    public UserDAO() {
System.out.println("현재 작업 디렉토리: " + System.getProperty("user.dir"));
System.out.println("읽고 있는 사용자 파일 경로: " + new File(USER_FILE).getAbsolutePath());

        new File(BASE_DIR).mkdirs();
        createFileIfNotExists(USER_FILE);
        createFileIfNotExists(PROF_FILE);
        createFileIfNotExists(ASSISTANT_FILE);
        createFileIfNotExists(RESERVE_CLASS_FILE);
        createFileIfNotExists(RESERVE_LAB_FILE);
        createFileIfNotExists(CHANGE_REQUEST);
        createFileIfNotExists(ROOM_STATUS);
        createFileIfNotExists(RESERVATION_REQUEST);
        createFileIfNotExists(APPROVED_BACKUP);
        createFileIfNotExists(TEMP1);
        createFileIfNotExists(TEMP2);
        createFileIfNotExists(CLASSROOMS_FILE);
        initDefaultClassrooms();   // ✅ 기본 강의실 자동 생성
//initDefaultLabs();         // ✅ 기본 실습실 자동 생성
    }

    private void createFileIfNotExists(String fileName) {
        try {
            File file = new File(fileName);
            if (!file.exists()) {
                file.getParentFile().mkdirs();  // 상위 폴더도 확인
                file.createNewFile();
            }
        } catch (IOException e) {
            System.out.println("파일 생성 오류: " + e.getMessage());
        }
    }

    public synchronized boolean validateUser(String userId, String password) {
        String fileName = getFileNameByUserId(userId);
        System.out.println("[validateUser] 검사 대상: ID=" + userId + ", PW=" + password);
    System.out.println("[validateUser] 파일 경로: " + fileName);
        if (fileName == null) {
            return false;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length >= 3) {
                System.out.println("[validateUser] 비교 중: " + tokens[1] + ", " + tokens[2]);
            }
                if (tokens.length >= 3 && tokens[1].equals(userId) && tokens[2].equals(password)) {
                                    System.out.println("[validateUser] 유효한 사용자 확인됨!");

                    return true;
                }
            }
        } catch (IOException e) {
            System.out.println("읽기 오류: " + e.getMessage());
        }
            System.out.println("[validateUser] 사용자 인증 실패");

        return false;
    }

    public synchronized boolean isUserIdExists(String userId) {
        String fileName = getFileNameByUserId(userId);
        if (fileName == null) {
            return false;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length >= 2 && tokens[1].equals(userId)) {
                    return true;
                }
            }
        } catch (IOException e) {
            System.out.println("읽기 오류: " + e.getMessage());
        }
        return false;
    }

    public synchronized void registerUser(User user) {
        String fileName = getFileNameByUserId(user.getUserId());
        if (fileName == null) {
            System.out.println("잘못된 형식의 ID입니다: " + user.getUserId());
            return;
        }

        if (isUserIdExists(user.getUserId())) {
            System.out.println("이미 존재하는 ID입니다: " + user.getUserId());
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
            writer.write(user.getName() + "," + user.getUserId() + "," + user.getPassword());
            writer.newLine();
        } catch (IOException e) {
            System.out.println("쓰기 오류: " + e.getMessage());
        }
    }

    public synchronized String getUserNameById(String userId) {
        String fileName = getFileNameByUserId(userId);
        System.out.println("[getUserNameById] 조회 대상 ID: " + userId);
    System.out.println("[getUserNameById] 파일 경로: " + fileName);
        if (fileName == null) {
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length >= 3 && tokens[1].equals(userId)) {
                                    System.out.println("[getUserNameById] 이름 찾음: " + tokens[0]);

                    return tokens[0];
                }
            }
        } catch (IOException e) {
            System.out.println("읽기 오류: " + e.getMessage());
        }
            System.out.println("[getUserNameById] 해당 ID 없음");

        return null;
    }

    public boolean authorizeAccess(String userId) {
        if (userId == null || userId.isEmpty()) {
            System.out.println("유효하지 않은 사용자 ID입니다.");
            return false;
        }

        char role = userId.charAt(0);
        return role == 'P' || role == 'A'; // 교수 또는 조교만 true 반환
    }

    public synchronized boolean updatePassword(String userId, String newPassword) {
    String fileName = getFileNameByUserId(userId);
    
    // 로그: 어떤 파일을 열려고 하는지 확인
    System.out.println("[updatePassword] userId: " + userId);
    System.out.println("[updatePassword] fileName: " + fileName);
    
    if (fileName == null) return false;

    File originalFile = new File(fileName);
    File tempFile = new File(fileName + ".tmp");

    boolean updated = false;

    try (
        BufferedReader reader = new BufferedReader(new FileReader(originalFile));
        BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))
    ) {
        String line;
        while ((line = reader.readLine()) != null) {
            
            // 로그: 파일에서 읽은 줄 확인
            System.out.println("[updatePassword] read line: " + line);
            
            String[] tokens = line.split(",");
            
            // 로그: ID 비교 로그
            if (tokens.length >= 3) {
                System.out.println("[updatePassword] 비교 대상: " + tokens[1].trim() + " == " + userId.trim());
            }
            
            if (tokens.length >= 3 && tokens[1].equals(userId)) {
                // 이름, 아이디는 유지하고 비밀번호만 수정
                writer.write(tokens[0] + "," + tokens[1] + "," + newPassword);
                updated = true;
            } else {
                writer.write(line);
            }
            writer.newLine();
        }
    } catch (IOException e) {
        System.out.println("비밀번호 변경 중 오류: " + e.getMessage());
        return false;
    }
    // 로그: 변경 결과 출력
    System.out.println("[updatePassword] updated: " + updated);
    

    // 파일 교체
    if (updated) {
        if (!originalFile.delete() || !tempFile.renameTo(originalFile)) {
            System.out.println("파일 덮어쓰기 실패");
            return false;
        }
    } else {
        tempFile.delete();
    }

    return updated;
}
   private String getFileNameByUserId(String userId) {
    if (userId == null) return null;

    userId = userId.trim().toUpperCase();

    if (userId.startsWith("S")) {
        return USER_FILE;
    }
    if (userId.startsWith("P")) {
        return PROF_FILE;
    }
    if (userId.startsWith("A")) {
        return ASSISTANT_FILE;
    }
    return null;
}


    public synchronized boolean login(String userId, String password) {
        return validateUser(userId, password);
    }

    public synchronized String getUserIdByName(String name) {
        String[] files = {USER_FILE, PROF_FILE, ASSISTANT_FILE};

        for (String fileName : files) {
            try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] tokens = line.split(",");
                    if (tokens.length >= 2 && tokens[0].trim().equals(name)) {
                        return tokens[1].trim();
                    }
                }
            } catch (IOException e) {
                System.out.println("getUserIdByName 읽기 오류: " + e.getMessage());
            }
        }

        return name;
    }
    /**
 * ✅ 서버 시작 시 ReserveClass.txt 자동 초기화
 *    - 기본 강의실 4개
 *    - 수용 인원은 변수 한 줄로 설정 가능
 */
/**
 * ✅ 서버 시작 시 Classrooms.txt 자동 생성
 * 강의실 + 실습실 기본 목록과 수용 인원 지정
 */
private void initDefaultClassrooms() {
    File file = new File(CLASSROOMS_FILE);

    // ⚙️ 기본 수용 인원 (이 한 줄만 수정하면 전체 변경 가능)
    int defaultCapacity = 30;

    // 파일이 비어 있을 때만 생성
    if (file.length() == 0) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write("# 강의실/실습실 기본 정보\n");
            bw.write("# 형식: 이름,타입(CLASS/LAB),수용인원\n\n");

            // ✅ 강의실
            bw.write("908호,CLASS," + defaultCapacity + "\n");
            bw.write("912호,CLASS," + defaultCapacity + "\n");
            bw.write("913호,CLASS," + defaultCapacity + "\n");
            bw.write("914호,CLASS," + defaultCapacity + "\n\n");

            // ✅ 실습실
            bw.write("911호,LAB," + defaultCapacity + "\n");
            bw.write("915호,LAB," + defaultCapacity + "\n");
            bw.write("916호,LAB," + defaultCapacity + "\n");
            bw.write("918호,LAB," + defaultCapacity + "\n");

            bw.flush();
            System.out.println("✅ 기본 Classrooms.txt 생성 완료 (" + defaultCapacity + "명 기준)");

        } catch (IOException e) {
            System.err.println("⚠️ Classrooms.txt 초기화 실패: " + e.getMessage());
        }
    } else {
        System.out.println("✅ Classrooms.txt 이미 존재 → 기본값 생성 생략");
    }
}

}