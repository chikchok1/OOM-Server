package Server;

import common.observer.OfflineNotificationManager;
import common.observer.ReservationNotification;
import java.util.List;
import java.io.PrintWriter;

/**
 * 오프라인 알림 전송 헬퍼 클래스
 */
public class OfflineNotificationHelper {
    
    /**
     * 로그인 시 저장된 오프라인 알림을 전송
     * @param userId 사용자 ID
     * @param out 클라이언트 PrintWriter
     */
    public static void sendOfflineNotifications(String userId, PrintWriter out) {
        try {
            OfflineNotificationManager offlineManager = OfflineNotificationManager.getInstance();
            List<ReservationNotification> notifications = offlineManager.getNotifications(userId);
            
            if (notifications.isEmpty()) {
                System.out.println("[오프라인 알림] " + userId + "의 저장된 알림 없음");
                return;
            }
            
            System.out.println("[오프라인 알림] " + userId + "에게 " + notifications.size() + "개 알림 전송 시작");
            
            for (ReservationNotification notification : notifications) {
                String notificationMessage = createNotificationMessage(notification);
                out.println(notificationMessage);
                out.flush();
                
                System.out.println("[오프라인 알림] 전송: " + notification.getMessage());
                
                // 알림 간 간격 (빠른 연속 전송 방지)
                Thread.sleep(100);
            }
            
            // 알림 전송 완료 후 저장된 알림 삭제
            offlineManager.clearNotifications(userId);
            System.out.println("[오프라인 알림] " + userId + "의 알림 전송 완료 및 삭제");
            
        } catch (Exception e) {
            System.err.println("[오프라인 알림] 전송 중 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 알림 메시지 생성
     */
    private static String createNotificationMessage(ReservationNotification notification) {
        // 프로토콜: NOTIFICATION,타입,메시지,강의실,날짜,요일,시간
        return String.format("NOTIFICATION,%s,%s,%s,%s,%s,%s",
            notification.getType(),
            notification.getMessage(),
            notification.getRoom(),
            notification.getDate(),
            notification.getDay(),
            notification.getTime()
        );
    }
}
