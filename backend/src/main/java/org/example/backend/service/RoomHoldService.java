package org.example.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý tạm giữ phòng (Room Hold) bằng bộ nhớ trong.
 *
 * Khi người dùng/nhân viên bắt đầu quy trình đặt phòng, một "hold"
 * được tạo cho phòng đó trong 10 phút.  Nếu người khác cũng cố đặt
 * cùng phòng trong thời gian hold, hệ thống từ chối với thông báo rõ ràng.
 *
 * Hold tự động hết hạn và được dọn dẹp mỗi phút qua @Scheduled.
 */
@Slf4j
@Service
public class RoomHoldService {

    /** Thời gian giữ phòng: 10 phút */
    private static final Duration HOLD_DURATION = Duration.ofMinutes(10);

    /** roomId → HoldData */
    private final ConcurrentHashMap<Long, HoldData> holds = new ConcurrentHashMap<>();

    // ── Inner record ────────────────────────────────────────────────────────────

    public record HoldData(
            String token,
            Long userId,
            String username,
            LocalDateTime expiresAt
    ) {
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }

        public boolean isOwnedBy(Long uid) {
            return Objects.equals(this.userId, uid);
        }

        public long remainingSeconds() {
            long secs = Duration.between(LocalDateTime.now(), expiresAt).getSeconds();
            return Math.max(0, secs);
        }
    }

    // ── Public API ──────────────────────────────────────────────────────────────

    /**
     * Cố gắng chiếm giữ phòng cho user.
     *
     * @return hold token (UUID) để xác thực khi submit đặt phòng
     * @throws IllegalStateException nếu phòng đang bị giữ bởi người khác
     */
    public String acquireHold(Long roomId, Long userId, String username) {
        cleanupExpired();

        HoldData existing = holds.get(roomId);
        if (existing != null && !existing.isExpired() && !existing.isOwnedBy(userId)) {
            throw new IllegalStateException(
                    "Phòng đang được xử lý bởi nhân viên/khách khác. Vui lòng thử lại sau "
                    + existing.remainingSeconds() + " giây!");
        }

        String token = UUID.randomUUID().toString();
        holds.put(roomId, new HoldData(token, userId, username, LocalDateTime.now().plus(HOLD_DURATION)));
        log.info("[RoomHold] Acquired roomId={} by user={} token={}", roomId, username, token.substring(0, 8));
        return token;
    }

    /**
     * Gia hạn hold (người dùng vẫn đang trong quy trình đặt phòng).
     * Chỉ cho phép gia hạn nếu token khớp.
     */
    public boolean renewHold(Long roomId, String token) {
        HoldData existing = holds.get(roomId);
        if (existing != null && existing.token().equals(token) && !existing.isExpired()) {
            holds.put(roomId, new HoldData(
                    existing.token(), existing.userId(), existing.username(),
                    LocalDateTime.now().plus(HOLD_DURATION)));
            return true;
        }
        return false;
    }

    /**
     * Giải phóng hold (khi đặt phòng thành công hoặc người dùng thoát trang).
     * Chỉ giải phóng nếu token khớp để tránh race condition.
     */
    public void releaseHold(Long roomId, String token) {
        if (token == null) return;
        holds.computeIfPresent(roomId, (id, data) -> {
            if (data.token().equals(token)) {
                log.info("[RoomHold] Released roomId={}", roomId);
                return null;            // null → xoá entry
            }
            return data;               // token không khớp → giữ nguyên
        });
    }

    /**
     * Kiểm tra phòng có đang bị giữ bởi người KHÁC không.
     * Dùng trước khi tạo booking để ngăn conflict.
     */
    public boolean isHeldByOther(Long roomId, Long userId) {
        HoldData hold = holds.get(roomId);
        return hold != null && !hold.isExpired() && !hold.isOwnedBy(userId);
    }

    /**
     * Lấy thông tin hold của một phòng (cho frontend hiển thị trạng thái).
     */
    public Optional<HoldData> getHold(Long roomId) {
        HoldData hold = holds.get(roomId);
        if (hold == null || hold.isExpired()) {
            holds.remove(roomId);
            return Optional.empty();
        }
        return Optional.of(hold);
    }

    /**
     * Trả về danh sách roomId đang bị giữ (để frontend ẩn/disable card).
     */
    public Set<Long> getHeldRoomIds() {
        cleanupExpired();
        return Collections.unmodifiableSet(holds.keySet());
    }

    // ── Scheduled cleanup ────────────────────────────────────────────────────────

    @Scheduled(fixedRate = 60_000)
    public void cleanupExpired() {
        int before = holds.size();
        holds.entrySet().removeIf(e -> e.getValue().isExpired());
        int removed = before - holds.size();
        if (removed > 0) {
            log.info("[RoomHold] Cleaned up {} expired hold(s)", removed);
        }
    }
}
