# ⚙️ Classroom Reservation System - Server

> 멀티스레드 기반 강의실 예약 시스템 서버

![Java](https://img.shields.io/badge/Java-21-007396?style=flat-square)
![Maven](https://img.shields.io/badge/Maven-3.x-C71A36?style=flat-square)
![Socket](https://img.shields.io/badge/TCP/IP-Multithreaded-blue?style=flat-square)

## 🎯 개요

TCP/IP 소켓 통신을 통해 여러 클라이언트의 요청을 동시에 처리하는 멀티스레드 서버입니다. Command 패턴 기반 확장 가능한 아키텍처와 파일 기반 데이터 저장소를 사용합니다.

### 핵심 특징

- ✅ 멀티스레드 동시 접속 처리 (Thread Pool)
- ✅ Command 패턴 기반 확장 가능한 요청 처리
- ✅ Observer 패턴 실시간 알림 시스템
- ✅ 파일 기반 영속성 (CSV)
- ✅ 체계적인 예외 처리 계층
- ✅ 외부 접속 지원 (0.0.0.0 바인딩)

## ✨ 주요 기능

| 기능                    | 상세                                               |
| ----------------------- | -------------------------------------------------- |
| **인증 및 사용자 관리** | 로그인/회원가입, 비밀번호 변경, 사용자 CRUD (조교) |
| **예약 관리**           | 신청, 승인/거부, 변경, 취소, 조회 (개인/전체/주간) |
| **강의실 관리**         | 조회, 추가/삭제, 정원/상태 수정 (조교)             |
| **알림 시스템**         | 실시간 알림 (온라인), 오프라인 알림 저장           |
| **통계**                | 예약 요청 수, 승인률, 강의실 사용률                |

## 🎨 디자인 패턴

### 1. Command Pattern

**목적**: 모든 클라이언트 요청을 Command 객체로 캡슐화

| 카테고리    | Command 수 | 주요 예시                                                                       |
| ----------- | ---------- | ------------------------------------------------------------------------------- |
| 인증        | 3개        | LoginCommand, RegisterCommand, ChangePasswordCommand                            |
| 예약        | 7개        | ReserveRequestCommand, ApproveReservationCommand, CancelReservationCommand      |
| 조회        | 6개        | ViewReservationCommand, ViewMyReservationsCommand, ViewWeeklyReservationCommand |
| 강의실 관리 | 7개        | AddClassroomCommand, DeleteClassroomCommand, UpdateRoomCapacityCommand          |
| 사용자 관리 | 3개        | GetAllUsersCommand, UpdateUserCommand, DeleteUserCommand                        |
| 통계        | 7개        | GetReservationRequestsCommand, CountPendingRequestCommand                       |

**총 33개 Command 구현**

### 2. Factory Method Pattern

**목적**: 요청 타입에 따라 적절한 Command 객체 생성

```java
public class DefaultCommandFactory {
    public Command createCommand(String requestType) {
        return switch (requestType) {
            case "LOGIN" -> new LoginCommand(...);
            case "RESERVE_REQUEST" -> new ReserveRequestCommand(...);
            // ... 33개 케이스
        };
    }
}
```

### 3. Singleton Pattern

| 클래스                     | 역할                         |
| -------------------------- | ---------------------------- |
| ServerClassroomManager     | 강의실 정보 캐싱 및 관리     |
| ReservationSubject         | 알림 Subject (Observer 패턴) |
| OfflineNotificationManager | 오프라인 알림 저장/조회      |

### 4. Observer Pattern

**목적**: 예약 상태 변경 시 실시간 알림 전송

```
조교 승인/거부
    ↓
ReservationSubject.notifyObservers()
    ├── 온라인 사용자: 소켓으로 즉시 전송
    └── 오프라인 사용자: 파일 저장 → 로그인 시 전송
```

## 📁 프로젝트 구조

```
src/main/java/Server/
├── LoginServer.java              # 메인 서버 (Thread Pool)
├── UserDAO.java                  # 사용자 데이터 접근
├── OfflineNotificationHelper.java # 오프라인 알림 처리
├── commands/                     # Command 패턴 (33개)
│   ├── Command.java              # Command 인터페이스
│   ├── CommandFactory.java       # Factory 인터페이스
│   ├── DefaultCommandFactory.java # Factory 구현
│   ├── CommandInvoker.java       # Command 실행자
│   ├── LoginCommand.java
│   ├── ReserveRequestCommand.java
│   ├── ApproveReservationCommand.java
│   └── ... (30개 더)
├── manager/
│   └── ServerClassroomManager.java # Singleton
└── exceptions/                   # 예외 계층
    ├── AuthenticationException.java
    ├── BusinessLogicException.java
    ├── DatabaseException.java
    └── InvalidInputException.java

data/                             # 파일 기반 DB
├── users.txt
├── Classrooms.txt
├── ReserveClass.txt
├── ReserveLab.txt
├── ReservationRequest.txt
└── notifications/
    └── {userId}.txt
```

## 🗂 데이터 저장

### 파일 기반 DB (CSV)

| 파일                       | 용도                                                 |
| -------------------------- | ---------------------------------------------------- |
| users.txt                  | 사용자 정보 (userId, password, name)                 |
| Classrooms.txt             | 강의실 목록 (roomNumber, roomType, capacity, status) |
| ReserveClass.txt           | 승인된 강의실 예약                                   |
| ReserveLab.txt             | 승인된 실습실 예약                                   |
| ReservationRequest.txt     | 예약 신청 (대기 중)                                  |
| ChangeRequest.txt          | 예약 변경 신청                                       |
| notifications/{userId}.txt | 오프라인 알림                                        |

### 데이터 포맷 예시

**users.txt**

```
S123,pass123,홍길동
P678,pass456,김교수
```

**Classrooms.txt**

```
101,강의실,30,사용가능
201,실습실,25,사용가능
```

**ReserveClass.txt**

```
강의실,101호,2025-11-28,목요일,1교시(09:00~10:00),강의,학생,전체,5,S12345
```

## 🚀 설치 및 실행

### 사전 요구사항

- Java 21+, Maven 3.x
- OOM-Common 모듈 설치

### 빌드 및 실행

```bash
# 1. Common 모듈 빌드
cd ../OOM-Common && mvn clean install

# 2. Server 빌드 및 실행
cd ../OOM-Server
mvn clean package
java -jar target/pos-server.jar
```

### 설정 (config.properties)

```properties
server.port=8000
max.clients=50
```

## 🌐 외부 접속 설정

서버는 `0.0.0.0`으로 바인딩되어 **이미 외부 접속이 가능**하도록 구현되어 있습니다.

### 로컬 네트워크에서 접속

1. 서버 실행 중인 PC의 IP 확인 (예: `192.168.0.100`)
2. 클라이언트의 `config.properties` 수정:
   ```properties
   server.ip=192.168.0.100
   server.port=8000
   ```
3. 방화벽에서 8000번 포트 허용

### 인터넷을 통한 외부 접속

1. 공유기 관리 페이지에서 **포트포워딩** 설정
   - 외부 포트: 8000
   - 내부 IP: 서버 PC IP
   - 내부 포트: 8000
2. 공인 IP 확인 (예: `203.0.113.100`)
3. 클라이언트의 `config.properties` 수정:
   ```properties
   server.ip=203.0.113.100
   server.port=8000
   ```

**참고**: Windows 방화벽 설정

```bash
# 방화벽 인바운드 규칙 추가
netsh advfirewall firewall add rule name="ClassroomServer" dir=in action=allow protocol=TCP localport=8000
```

## 🔒 동시성 제어

| 메커니즘          | 적용 위치          | 목적                    |
| ----------------- | ------------------ | ----------------------- |
| ConcurrentHashMap | 로그인 사용자 관리 | Thread-safe 사용자 목록 |
| synchronized 블록 | 파일 읽기/쓰기     | 파일 접근 동기화        |
| FILE_LOCK 객체    | 모든 파일 작업     | Race condition 방지     |

```java
private static final Object FILE_LOCK = new Object();

synchronized (FILE_LOCK) {
    // 파일 읽기/쓰기
}
```

## 📡 통신 프로토콜

### 요청/응답 포맷 (CSV)

**요청**

```
COMMAND_TYPE,param1,param2,...
```

**응답**

```
SUCCESS,message
FAILURE,error_message
DATA,field1,field2,...
```

### 주요 명령어

| 명령어              | 요청 예시                              | 응답 예시                |
| ------------------- | -------------------------------------- | ------------------------ |
| LOGIN               | `LOGIN,S123,pass123`                   | `SUCCESS,홍길동`         |
| REGISTER            | `REGISTER,S123,홍길동,pass123,S`       | `SUCCESS`                |
| RESERVE_REQUEST     | `RESERVE_REQUEST,101호,2025-11-28,...` | `SUCCESS`                |
| APPROVE_RESERVATION | `APPROVE_RESERVATION,reservationId`    | `SUCCESS`                |
| GET_CLASSROOMS      | `GET_CLASSROOMS`                       | `DATA,101,강의실,30,...` |

### 실시간 알림

```
NOTIFICATION,APPROVED,101호,2025-11-28,1교시
NOTIFICATION,REJECTED,101호,2025-11-28,1교시,정원 초과
```

## ⚠️ 예외 처리

### 예외 계층 구조

```
RuntimeException
├── InvalidInputException          # 잘못된 입력
├── AuthenticationException        # 인증 실패
├── BusinessLogicException         # 비즈니스 규칙 위반
└── DatabaseException              # 데이터 접근 오류
```

### CommandInvoker 에러 로깅

```java
try {
    command.execute(tokens);
} catch (InvalidInputException e) {
    logError(ErrorLevel.WARNING, e);
} catch (AuthenticationException e) {
    logError(ErrorLevel.ERROR, e);
} catch (DatabaseException e) {
    logError(ErrorLevel.CRITICAL, e);
}
```

## 🧪 테스트

```bash
mvn test
```

**테스트 커버리지**: 80%+

- Command 패턴 실행 테스트
- Manager Singleton 테스트
- 예외 처리 테스트
- Rollback 기능 테스트

## 🔧 확장성

### 새로운 Command 추가 3단계

**1. Command 구현**

```java
public class NewCommand implements Command {
    @Override
    public void execute(String[] tokens) {
        // 구현
    }
}
```

**2. Factory 등록**

```java
case "NEW_COMMAND" -> new NewCommand(...);
```

**3. 클라이언트 호출**

```
NEW_COMMAND,param1,param2
```

## 📊 통계

- **Command**: 33개 구현
- **디자인 패턴**: 4개 (Command, Factory Method, Singleton, Observer)
- **동시 접속**: 최대 50명

## 🔗 관련 프로젝트

- [OOM-Client](https://github.com/chikchok1/OOM-Client) - Swing GUI 클라이언트
- [OOM-Common](https://github.com/chikchok1/OOM-Common) - 공통 라이브러리

---

**OOM Team** | 객체지향 프로그래밍 과제
