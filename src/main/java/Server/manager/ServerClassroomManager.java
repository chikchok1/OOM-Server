package Server.manager;

import common.dto.ClassroomDTO;
import java.io.*;
import java.util.*;

/**
 * 서버 전용 강의실/실습실 관리자 (Singleton Pattern)
 * 파일 I/O와 데이터 관리를 담당
 */
public class ServerClassroomManager {
    
    private static volatile ServerClassroomManager instance;
    
    private Map<String, ClassroomDTO> classrooms;
    private List<String> classroomNames;
    private List<String> labNames;
    
    private static final String CLASSROOM_FILE = System.getProperty("user.dir") 
            + File.separator + "data" + File.separator + "Classrooms.txt";
    
    private static final int DEFAULT_CAPACITY = 30;
    
    private ServerClassroomManager() {
        classrooms = new HashMap<>();
        classroomNames = new ArrayList<>();
        labNames = new ArrayList<>();
        loadClassroomData();
    }
    
    public static ServerClassroomManager getInstance() {
        if (instance == null) {
            synchronized (ServerClassroomManager.class) {
                if (instance == null) {
                    instance = new ServerClassroomManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 파일에서 강의실 데이터 로드
     */
    private void loadClassroomData() {
        File file = new File(CLASSROOM_FILE);
        
        if (!file.exists()) {
            createDefaultClassroomFile();
            file = new File(CLASSROOM_FILE);
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;
                
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    String name = parts[0].trim();
                    String type = parts[1].trim();
                    int capacity = Integer.parseInt(parts[2].trim());
                    
                    ClassroomDTO dto = new ClassroomDTO(name, type, capacity);
                    classrooms.put(name, dto);
                    
                    if ("CLASS".equals(type)) {
                        classroomNames.add(name);
                    } else if ("LAB".equals(type)) {
                        labNames.add(name);
                    }
                }
            }
            
            Collections.sort(classroomNames);
            Collections.sort(labNames);
            
            System.out.println("[서버] 강의실 로드 완료: " + classroomNames.size() + "개");
            System.out.println("[서버] 실습실 로드 완료: " + labNames.size() + "개");
            
        } catch (IOException e) {
            System.err.println("[서버] 강의실 데이터 로드 실패: " + e.getMessage());
        }
    }
    
    /**
     * 기본 강의실 파일 생성
     */
    private void createDefaultClassroomFile() {
        try {
            File file = new File(CLASSROOM_FILE);
            file.getParentFile().mkdirs();
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                writer.println("# 강의실/실습실 정보");
                writer.println("# 형식: 이름,타입(CLASS/LAB),수용인원");
                writer.println();
                
                // 기본 강의실
                writer.println("908호,CLASS," + DEFAULT_CAPACITY);
                writer.println("912호,CLASS," + DEFAULT_CAPACITY);
                writer.println("913호,CLASS," + DEFAULT_CAPACITY);
                writer.println("914호,CLASS," + DEFAULT_CAPACITY);
                
                writer.println();
                
                // 기본 실습실
                writer.println("911호,LAB," + DEFAULT_CAPACITY);
                writer.println("915호,LAB," + DEFAULT_CAPACITY);
                writer.println("916호,LAB," + DEFAULT_CAPACITY);
                writer.println("918호,LAB," + DEFAULT_CAPACITY);
            }
            
            System.out.println("[서버] 기본 강의실 파일 생성: " + CLASSROOM_FILE);
            
        } catch (IOException e) {
            System.err.println("[서버] 기본 파일 생성 실패: " + e.getMessage());
        }
    }
    
    /**
     * 모든 강의실 DTO 목록 반환
     */
    public List<ClassroomDTO> getAllClassrooms() {
        List<ClassroomDTO> result = new ArrayList<>();
        for (String name : classroomNames) {
            result.add(classrooms.get(name));
        }
        return result;
    }
    
    /**
     * 모든 실습실 DTO 목록 반환
     */
    public List<ClassroomDTO> getAllLabs() {
        List<ClassroomDTO> result = new ArrayList<>();
        for (String name : labNames) {
            result.add(classrooms.get(name));
        }
        return result;
    }
    
    /**
     * 특정 강의실 DTO 조회
     */
    public ClassroomDTO getClassroom(String name) {
        return classrooms.get(name);
    }
    
    /**
     * 수용 인원 체크 (50% 제한)
     */
    public boolean checkCapacity(String roomName, int requestedCount) {
        ClassroomDTO dto = classrooms.get(roomName);
        if (dto == null) {
            System.err.println("[서버] 알 수 없는 강의실: " + roomName);
            return false;
        }
        
        int allowedCapacity = dto.getAllowedCapacity();
        boolean isAllowed = requestedCount <= allowedCapacity;
        
        System.out.println(String.format(
            "[서버 수용인원체크] %s: 최대 %d명, 허용 %d명(50%%), 요청 %d명 → %s",
            roomName, dto.capacity, allowedCapacity, requestedCount,
            isAllowed ? "승인" : "거부"
        ));
        
        return isAllowed;
    }
    
    /**
     * 강의실 존재 여부 확인
     */
    public boolean exists(String roomName) {
        return classrooms.containsKey(roomName);
    }
    
    /**
     * 수용 인원 수정
     */
    public synchronized boolean updateCapacity(String name, int newCapacity) {
        ClassroomDTO oldDto = classrooms.get(name);
        if (oldDto == null) {
            System.err.println("[서버] 존재하지 않는 강의실: " + name);
            return false;
        }
        
        // 새로운 DTO 생성 (불변 객체)
        ClassroomDTO newDto = new ClassroomDTO(name, oldDto.type, newCapacity);
        classrooms.put(name, newDto);
        
        saveClassroomData();
        System.out.println("[서버] " + name + " 수용 인원 변경: " + newCapacity + "명");
        return true;
    }
    
    /**
     * 변경사항을 파일에 저장
     */
    private void saveClassroomData() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(CLASSROOM_FILE))) {
            writer.println("# 강의실/실습실 정보");
            writer.println("# 형식: 이름,타입,수용인원");
            writer.println();
            
            // 강의실 먼저 저장
            for (String name : classroomNames) {
                ClassroomDTO dto = classrooms.get(name);
                writer.println(dto.toProtocol());
            }
            
            writer.println();
            
            // 실습실 저장
            for (String name : labNames) {
                ClassroomDTO dto = classrooms.get(name);
                writer.println(dto.toProtocol());
            }
            
            System.out.println("[서버] 강의실 데이터 저장 완료");
            
        } catch (IOException e) {
            System.err.println("[서버] 데이터 저장 실패: " + e.getMessage());
        }
    }
    
    /**
     * 데이터 새로고침 (파일에서 재로드)
     */
    public void refresh() {
        classrooms.clear();
        classroomNames.clear();
        labNames.clear();
        loadClassroomData();
        System.out.println("[서버] 강의실 데이터 새로고침 완료");
    }
}
