package Server.manager;

import common.dto.ClassroomDTO;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ServerClassroomManager 싱글턴 패턴 테스트
 */
class ServerClassroomManagerTest {
    
    private ServerClassroomManager manager;
    private static final String TEST_DATA_DIR = "test_data";
    private static final String TEST_CLASSROOM_FILE = TEST_DATA_DIR + File.separator + "Classrooms.txt";
    
    @BeforeAll
    static void setUpClass() {
        // 테스트 데이터 디렉토리 생성
        new File(TEST_DATA_DIR).mkdirs();
    }
    
    @BeforeEach
    void setUp() {
        // 테스트용 강의실 파일 생성
        createTestClassroomFile();
        
        // 싱글턴 인스턴스 가져오기
        manager = ServerClassroomManager.getInstance();
    }
    
    @AfterEach
    void tearDown() {
        // 테스트 파일 삭제
        File testFile = new File(TEST_CLASSROOM_FILE);
        if (testFile.exists()) {
            testFile.delete();
        }
    }
    
    @AfterAll
    static void tearDownClass() {
        // 테스트 디렉토리 삭제
        File testDir = new File(TEST_DATA_DIR);
        if (testDir.exists()) {
            testDir.delete();
        }
    }
    
    /**
     * 테스트용 강의실 파일 생성
     */
    private void createTestClassroomFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(TEST_CLASSROOM_FILE))) {
            writer.println("# 테스트 강의실 데이터");
            writer.println("908호,CLASS,30");
            writer.println("912호,CLASS,35");
            writer.println("911호,LAB,40");
            writer.println("915호,LAB,45");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 테스트 1: 싱글턴 인스턴스가 동일한지 검증
     */
    @Test
    @DisplayName("싱글턴 패턴: 항상 같은 인스턴스 반환")
    void testSingletonInstance() {
        ServerClassroomManager instance1 = ServerClassroomManager.getInstance();
        ServerClassroomManager instance2 = ServerClassroomManager.getInstance();
        
        assertSame(instance1, instance2, "같은 인스턴스를 반환해야 함");
    }
    
    /**
     * 테스트 2: Double-Checked Locking 싱글턴 패턴 검증
     */
    @Test
    @DisplayName("멀티스레드 환경에서 싱글턴 인스턴스 일관성")
    void testSingletonInMultiThreadEnvironment() throws InterruptedException {
        final int THREAD_COUNT = 10;
        ServerClassroomManager[] instances = new ServerClassroomManager[THREAD_COUNT];
        Thread[] threads = new Thread[THREAD_COUNT];
        
        // When: 여러 스레드에서 동시에 getInstance 호출
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                instances[index] = ServerClassroomManager.getInstance();
            });
            threads[i].start();
        }
        
        // 모든 스레드 대기
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Then: 모든 인스턴스가 동일해야 함
        for (int i = 1; i < THREAD_COUNT; i++) {
            assertSame(instances[0], instances[i], 
                "멀티스레드 환경에서도 같은 인스턴스를 반환해야 함");
        }
    }
    
    /**
     * 테스트 3: 특정 강의실 조회
     */
    @Test
    @DisplayName("특정 강의실 조회 테스트")
    void testGetClassroom() {
        // When
        ClassroomDTO classroom = manager.getClassroom("908호");
        
        // Then
        assertNotNull(classroom, "908호 강의실이 존재해야 함");
        assertEquals("908호", classroom.name);
        assertEquals("CLASS", classroom.type);
        assertEquals(30, classroom.capacity);
    }
    
    /**
     * 테스트 4: 모든 강의실 목록 조회
     */
    @Test
    @DisplayName("모든 강의실 목록 조회 테스트")
    void testGetAllClassrooms() {
        // When
        List<ClassroomDTO> classrooms = manager.getAllClassrooms();
        
        // Then
        assertNotNull(classrooms, "강의실 목록은 null이 아니어야 함");
        assertTrue(classrooms.size() >= 2, "최소 2개의 강의실이 있어야 함");
        
        // 모두 강의실 타입인지 확인
        assertTrue(classrooms.stream().allMatch(c -> c.isClassroom()), 
            "모두 강의실 타입이어야 함");
    }
    
    /**
     * 테스트 5: 모든 실습실 목록 조회
     */
    @Test
    @DisplayName("모든 실습실 목록 조회 테스트")
    void testGetAllLabs() {
        // When
        List<ClassroomDTO> labs = manager.getAllLabs();
        
        // Then
        assertNotNull(labs, "실습실 목록은 null이 아니어야 함");
        assertTrue(labs.size() >= 2, "최소 2개의 실습실이 있어야 함");
        
        // 모두 실습실 타입인지 확인
        assertTrue(labs.stream().allMatch(l -> l.isLab()), 
            "모두 실습실 타입이어야 함");
    }
    
    /**
     * 테스트 6: 수용 인원 체크 (50% 제한)
     */
    @Test
    @DisplayName("수용 인원 체크 테스트 (50% 제한)")
    void testCheckCapacity() {
        // Given: 908호는 30명 수용 -> 50%는 15명
        
        // When & Then
        assertTrue(manager.checkCapacity("908호", 15), 
            "15명(50%)은 허용되어야 함");
        assertFalse(manager.checkCapacity("908호", 16), 
            "16명(50% 초과)은 거부되어야 함");
        assertTrue(manager.checkCapacity("908호", 10), 
            "10명(50% 미만)은 허용되어야 함");
    }
    
    /**
     * 테스트 7: 존재하지 않는 강의실 조회
     */
    @Test
    @DisplayName("존재하지 않는 강의실 조회 시 null 반환")
    void testGetNonExistentClassroom() {
        // When
        ClassroomDTO classroom = manager.getClassroom("존재하지않는강의실");
        
        // Then
        assertNull(classroom, "존재하지 않는 강의실은 null을 반환해야 함");
    }
    
    /**
     * 테스트 8: 강의실 존재 여부 확인
     */
    @Test
    @DisplayName("강의실 존재 여부 확인 테스트")
    void testExists() {
        // When & Then
        assertTrue(manager.exists("908호"), "908호는 존재해야 함");
        assertFalse(manager.exists("존재하지않는강의실"), "존재하지 않는 강의실은 false");
    }
    
    /**
     * 테스트 9: 수용 인원 수정
     */
    @Test
    @DisplayName("수용 인원 수정 테스트")
    void testUpdateCapacity() {
        // Given
        String roomName = "908호";
        int newCapacity = 40;
        
        // When
        boolean result = manager.updateCapacity(roomName, newCapacity);
        ClassroomDTO updatedRoom = manager.getClassroom(roomName);
        
        // Then
        assertTrue(result, "수정이 성공해야 함");
        assertNotNull(updatedRoom);
        assertEquals(newCapacity, updatedRoom.capacity, "수용 인원이 변경되어야 함");
    }
    
    /**
     * 테스트 10: 존재하지 않는 강의실 수정 시도
     */
    @Test
    @DisplayName("존재하지 않는 강의실 수정 시도 시 실패")
    void testUpdateCapacityForNonExistentClassroom() {
        // When
        boolean result = manager.updateCapacity("존재하지않는강의실", 50);
        
        // Then
        assertFalse(result, "존재하지 않는 강의실 수정은 실패해야 함");
    }
    
    /**
     * 테스트 11: 강의실 추가
     */
    @Test
    @DisplayName("강의실 추가 테스트")
    void testAddClassroom() {
        // Given
        ClassroomDTO newClassroom = new ClassroomDTO("920호", "CLASS", 50);
        
        // When
        boolean result = manager.addClassroom(newClassroom);
        ClassroomDTO addedRoom = manager.getClassroom("920호");
        
        // Then
        assertTrue(result, "추가가 성공해야 함");
        assertNotNull(addedRoom, "추가된 강의실을 조회할 수 있어야 함");
        assertEquals("920호", addedRoom.name);
        assertEquals(50, addedRoom.capacity);
    }
    
    /**
     * 테스트 12: 중복 강의실 추가 시도
     */
    @Test
    @DisplayName("중복 강의실 추가 시도 시 실패")
    void testAddDuplicateClassroom() {
        // Given
        ClassroomDTO duplicateClassroom = new ClassroomDTO("908호", "CLASS", 50);
        
        // When
        boolean result = manager.addClassroom(duplicateClassroom);
        
        // Then
        assertFalse(result, "중복 강의실 추가는 실패해야 함");
    }
    
    /**
     * 테스트 13: 강의실 삭제
     */
    @Test
    @DisplayName("강의실 삭제 테스트")
    void testDeleteClassroom() {
        // Given
        String roomName = "908호";
        
        // When
        boolean result = manager.deleteClassroom(roomName);
        ClassroomDTO deletedRoom = manager.getClassroom(roomName);
        
        // Then
        assertTrue(result, "삭제가 성공해야 함");
        assertNull(deletedRoom, "삭제된 강의실은 조회되지 않아야 함");
    }
    
    /**
     * 테스트 14: 존재하지 않는 강의실 삭제 시도
     */
    @Test
    @DisplayName("존재하지 않는 강의실 삭제 시도 시 실패")
    void testDeleteNonExistentClassroom() {
        // When
        boolean result = manager.deleteClassroom("존재하지않는강의실");
        
        // Then
        assertFalse(result, "존재하지 않는 강의실 삭제는 실패해야 함");
    }
    
    /**
     * 테스트 15: classroomExists 메서드 테스트
     */
    @Test
    @DisplayName("classroomExists 메서드 테스트")
    void testClassroomExists() {
        // When & Then
        assertTrue(manager.classroomExists("908호"), "908호는 존재해야 함");
        assertTrue(manager.classroomExists("911호"), "911호는 존재해야 함");
        assertFalse(manager.classroomExists("존재하지않는강의실"), "존재하지 않는 강의실은 false");
    }
    
    /**
     * 테스트 16: null 강의실 추가 시도
     */
    @Test
    @DisplayName("null 강의실 추가 시도 시 실패")
    void testAddNullClassroom() {
        // When
        boolean result = manager.addClassroom(null);
        
        // Then
        assertFalse(result, "null 강의실 추가는 실패해야 함");
    }
    
    /**
     * 테스트 17: null 또는 빈 이름으로 삭제 시도
     */
    @Test
    @DisplayName("null 또는 빈 이름으로 삭제 시도 시 실패")
    void testDeleteWithNullOrEmptyName() {
        // When & Then
        assertFalse(manager.deleteClassroom(null), "null 이름 삭제는 실패해야 함");
        assertFalse(manager.deleteClassroom(""), "빈 이름 삭제는 실패해야 함");
        assertFalse(manager.deleteClassroom("   "), "공백 이름 삭제는 실패해야 함");
    }
    
    /**
     * 테스트 18: 수용 인원 체크 - 존재하지 않는 강의실
     */
    @Test
    @DisplayName("수용 인원 체크 - 존재하지 않는 강의실은 false")
    void testCheckCapacityForNonExistentClassroom() {
        // When
        boolean result = manager.checkCapacity("존재하지않는강의실", 10);
        
        // Then
        assertFalse(result, "존재하지 않는 강의실은 false를 반환해야 함");
    }
    
    /**
     * 테스트 19: volatile 키워드로 인한 가시성 보장 (개념적 테스트)
     */
    @Test
    @DisplayName("volatile 키워드로 멀티스레드 가시성 보장")
    void testVolatileVisibility() throws InterruptedException {
        // Given: 여러 스레드에서 getInstance 호출
        final int THREAD_COUNT = 20;
        Thread[] threads = new Thread[THREAD_COUNT];
        ServerClassroomManager[] instances = new ServerClassroomManager[THREAD_COUNT];
        
        // When
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                instances[index] = ServerClassroomManager.getInstance();
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Then: 모든 스레드가 같은 인스턴스를 받아야 함
        for (int i = 1; i < THREAD_COUNT; i++) {
            assertSame(instances[0], instances[i]);
        }
    }
    
    /**
     * 테스트 20: 강의실과 실습실이 독립적으로 관리되는지 확인
     */
    @Test
    @DisplayName("강의실과 실습실 독립적 관리 확인")
    void testClassroomAndLabIndependence() {
        // Given
        int classroomCount = manager.getAllClassrooms().size();
        int labCount = manager.getAllLabs().size();
        
        ClassroomDTO newClassroom = new ClassroomDTO("920호", "CLASS", 30);
        ClassroomDTO newLab = new ClassroomDTO("921호", "LAB", 40);
        
        // When
        manager.addClassroom(newClassroom);
        manager.addClassroom(newLab);
        
        // Then
        assertEquals(classroomCount + 1, manager.getAllClassrooms().size(), 
            "강의실만 1개 증가해야 함");
        assertEquals(labCount + 1, manager.getAllLabs().size(), 
            "실습실만 1개 증가해야 함");
    }
}
