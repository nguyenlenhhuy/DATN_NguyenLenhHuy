# Tài liệu Kỹ thuật — Hệ thống Đặt Phòng Khách Sạn (LuxeHotel)

> **Dự án:** Đồ án tốt nghiệp — Hệ thống quản lý & đặt phòng khách sạn trực tuyến  
> **Stack:** Spring Boot 3 + Angular 17 + MySQL 8  
> **Tác giả:** Nguyễn Lê Nhật Huy

---

## Mục lục

1. [Kiến trúc tổng thể](#1-kiến-trúc-tổng-thể)
2. [Xác thực & Phân quyền](#2-xác-thực--phân-quyền)
3. [Đặt phòng & Chống Overbooking](#3-đặt-phòng--chống-overbooking)
4. [Tính giá động](#4-tính-giá-động)
5. [Mã giảm giá (Promotion)](#5-mã-giảm-giá-promotion)
6. [Thanh toán — PayOS](#6-thanh-toán--payos)
7. [Quản lý trạng thái phòng](#7-quản-lý-trạng-thái-phòng)
8. [Lịch đặt phòng & Calendar Heatmap](#8-lịch-đặt-phòng--calendar-heatmap)
9. [Dashboard & Thống kê](#9-dashboard--thống-kê)
10. [Chatbot AI (Gemini RAG)](#10-chatbot-ai-gemini-rag)
11. [Email tự động](#11-email-tự-động)
12. [Audit Log](#12-audit-log)
13. [Scheduler tự động](#13-scheduler-tự-động)
14. [Bảo mật API](#14-bảo-mật-api)
15. [Frontend Angular](#15-frontend-angular)

---

## 1. Kiến trúc tổng thể

### Mô hình
```
[Angular SPA] ──HTTP/SSE──► [Spring Boot REST API] ──JPA──► [MySQL 8]
                                     │
                          ┌──────────┼──────────────┐
                      [PayOS SDK]  [Gemini API]  [Gmail SMTP]
```

### Lý do chọn Monolith thay vì Microservices
- Quy mô dự án đồ án: 1 developer, không cần phân tán
- Giảm độ phức tạp vận hành (không cần Docker Swarm / K8s)
- Dễ debug end-to-end trong cùng 1 process
- Có thể tách thành microservices sau nếu scale

### Lý do chọn Spring WebFlux (Reactor Netty)
- Hỗ trợ **SSE (Server-Sent Events)** cho chatbot streaming mà không cần WebSocket
- Non-blocking I/O: phù hợp khi gọi Gemini API (latency cao, I/O-bound)
- Cho phép trả `Flux<String>` cho stream response mà không block thread

---

## 2. Xác thực & Phân quyền

### 2.1 JWT (JSON Web Token) — HS256

**Luồng hoạt động:**
```
Đăng nhập → Server ký JWT (HS256) → Client lưu localStorage
→ Mỗi request gắn "Authorization: Bearer <token>"
→ JwtAuthenticationFilter parse token → inject vào SecurityContext
```

**Cấu trúc token:**
```json
{
  "sub": "user@email.com",
  "role": "CUSTOMER",
  "iat": 1718000000,
  "exp": 1718604800
}
```

**Lý do chọn JWT thay vì Session:**
- Stateless: không cần lưu session trên server → dễ scale horizontal
- Angular SPA chạy tách biệt domain với backend → CORS-safe
- Tự mang theo thông tin role → không cần query DB cho mỗi request

**Lý do chọn HS256 thay vì RS256:**
- Đồ án single-server: không cần verify từ nhiều service → symmetric key đủ dùng
- RS256 cần quản lý public/private key pair, phức tạp hơn không cần thiết

### 2.2 Phân quyền RBAC (Role-Based Access Control)

| Role | Quyền |
|------|-------|
| `GUEST` | Xem phòng, chatbot |
| `CUSTOMER` | Đặt phòng, xem lịch sử, yêu thích, đánh giá |
| `STAFF` | Quản lý đặt phòng, check-in/out, xem thống kê vận hành |
| `ADMIN` | Toàn quyền: quản lý user, phòng, khuyến mãi, dashboard |

**Lý do dùng 4 tầng role:**
- STAFF không được xóa phòng hay quản lý user → nguyên tắc least privilege
- ADMIN cần quyền hủy cả đơn đã CHECK_IN (STAFF không được)

### 2.3 OTP Email — Xác thực đăng ký & quên mật khẩu

**Luồng:**
```
Đăng ký → Tài khoản PENDING → Gửi OTP 6 số → Xác thực → ACTIVE
Quên MK → Nhập email → Gửi OTP → Xác thực → Đặt lại mật khẩu
```

**Lý do dùng OTP thay vì link email:**
- Người dùng mobile dễ nhập 6 số hơn click link dài
- Tránh vấn đề link bị block bởi email client corporate
- Thời gian hết hạn OTP ngắn (5-10 phút) → an toàn hơn link dài hạn

**Lý do lưu OTP vào DB (bảng `otp_storage`) thay vì cache:**
- Không cần Redis cho dự án này → đơn giản hóa infrastructure
- OTP chỉ tồn tại ngắn, TTL cleanup bằng scheduled job

### 2.4 BCrypt — Mã hóa mật khẩu

**Lý do chọn BCrypt:**
- Adaptive: có thể tăng cost factor khi hardware mạnh hơn
- Built-in salt: mỗi hash có salt riêng → chống rainbow table
- Spring Security hỗ trợ native, không cần thêm dependency

---

## 3. Đặt phòng & Chống Overbooking

### 3.1 Vấn đề Overbooking

Khi 2 người dùng cùng lúc đặt 1 phòng trong cùng khoảng ngày, nếu không có cơ chế đồng bộ, cả 2 đều thành công → phòng bị đặt 2 lần.

### 3.2 Giải pháp 3 lớp

#### Lớp 1 — Room Hold (In-memory, 10 phút)

```java
ConcurrentHashMap<Long, HoldData> holds  // roomId → HoldData{token, userId, expiresAt}
```

**Hoạt động:**
- Khi user vào trang đặt phòng → `acquireHold(roomId, userId)` → trả về UUID token
- Người thứ 2 cố hold cùng phòng → bị từ chối ngay: *"Phòng đang được xử lý. Thử lại sau X giây"*
- Sau 10 phút không hoàn tất → hold tự giải phóng

**Lý do dùng in-memory thay vì DB:**
- Hold là trạng thái tạm thời, không cần persist
- In-memory nhanh hơn DB query ~100x
- `ConcurrentHashMap` thread-safe cho concurrent access
- `@Scheduled` dọn expired hold mỗi 60 giây

**Lý do dùng token (UUID) thay vì chỉ userId:**
- Chống trường hợp user mở 2 tab → tab thứ 2 không thể dùng hold của tab 1
- Khi submit booking phải gửi kèm token → server verify đúng phiên hold

#### Lớp 2 — Pessimistic Lock (DB row-level)

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT r FROM Room r WHERE r.id = :id")
Optional<Room> findByIdWithLock(Long id);
// → Sinh ra: SELECT * FROM rooms WHERE id=? FOR UPDATE
```

**Lý do dùng Pessimistic thay vì Optimistic Lock:**
- Overbooking có hậu quả nghiêm trọng → ưu tiên correctness hơn performance
- Optimistic lock dùng version field → retry logic phức tạp khi conflict
- Transaction booking ngắn (< 100ms) → lock contention thấp, không ảnh hưởng throughput

#### Lớp 3 — Date Overlap Query (kiểm tra chồng lịch)

```sql
SELECT COUNT(b) > 0 FROM Booking b
JOIN b.bookingDetails bd
WHERE bd.room.id = :roomId
AND b.status IN (PENDING, CONFIRMED, CHECK_IN)
AND (:checkIn < b.checkOutDate AND :checkOut > b.checkInDate)
```

**Công thức phát hiện chồng lịch (interval overlap):**
```
A chồng B ⟺ A.start < B.end AND A.end > B.start
```
Đây là điều kiện cần và đủ để 2 khoảng thời gian giao nhau.

**Lý do không dùng chỉ Room.status:**
- `Room.status = AVAILABLE` nhưng có thể có booking CONFIRMED tương lai → false negative
- Query booking là nguồn truth chính xác nhất về lịch đặt

### 3.3 Luồng xử lý booking hoàn chỉnh

```
1. User chọn phòng → acquireHold (token 10 phút)
2. Submit form → findByIdWithLock (PESSIMISTIC_WRITE)
3. isHeldByOther → reject nếu phòng bị người khác giữ
4. isRoomOccupied → reject nếu lịch trùng
5. calculateTotalAmount → tính giá
6. validateCoupon (backend) → tính giảm giá
7. save(Booking) → save(BookingDetail) → save(Invoice)
8. releaseHold (sau khi commit thành công)
9. Nếu PAYOS → createPaymentLink
```

---

## 4. Tính giá động

### 4.1 Bài toán

Giá phòng thay đổi theo ngày: ngày lễ, cuối tuần, mùa cao điểm.

### 4.2 Giải pháp

```java
BigDecimal calculateTotalAmount(Room room, LocalDate checkIn, LocalDate checkOut) {
    // Duyệt từng ngày trong khoảng [checkIn, checkOut)
    for (LocalDate d = checkIn; d.isBefore(checkOut); d = d.plusDays(1)) {
        // Tra bảng room_price: có override cho ngày này không?
        BigDecimal dailyPrice = roomPriceRepository.findByRoomAndDate(room, d)
            .map(RoomPrice::getPrice)
            .orElse(room.getRoomType().getBasePrice()); // fallback giá gốc
        total = total.add(dailyPrice);
    }
}
```

**Lý do dùng `BigDecimal` thay vì `double`:**
- Tiền tệ yêu cầu precision tuyệt đối — `double` có lỗi làm tròn nhị phân
- Ví dụ: `0.1 + 0.2 = 0.30000000000000004` với double
- `BigDecimal` đảm bảo phép cộng chính xác đến từng đồng

**Snapshot giá tại thời điểm đặt:**
```java
BookingDetail.priceAtBooking = avgPriceSnapshot  // Giá TB/đêm khi đặt
```
→ Tránh tình huống: giá phòng thay đổi sau khi booking đã tạo

---

## 5. Mã giảm giá (Promotion)

### 5.1 Luồng xử lý

```
Client gửi couponCode → Backend validate độc lập:
  1. Tìm promotion theo code (case-insensitive)
  2. Kiểm tra isActive = true
  3. Kiểm tra startDate ≤ checkIn ≤ endDate
  4. Tính discountAmount = totalPrice × discountPercentage / 100
  5. finalAmount = totalPrice - discountAmount
```

**Lý do validate hoàn toàn ở backend:**
- Client có thể bị tamper (devtools thay đổi giảm giá)
- Backend là nguồn truth duy nhất → không trust số tiền frontend gửi lên

**Lý do lưu riêng 3 trường:**
```java
booking.totalPrice     // Giá gốc trước giảm
booking.discountAmount // Số tiền được giảm
booking.finalAmount    // Số tiền thực tế thu / gửi sang PayOS
```
→ Audit trail rõ ràng, có thể tái tính kiểm tra sau này

### 5.2 Gửi email khuyến mãi đến danh sách yêu thích

Khi admin tạo promotion mới → tự động email đến tất cả user có phòng trong danh sách yêu thích (`Favorite`):
```java
TransactionSynchronization.afterCommit() → emailService.sendPromotionEmail(...)
```

**Lý do dùng `afterCommit()`:**
- Tránh gửi email khi transaction rollback (DB lỗi sau khi gửi email → không rollback được email)
- Đảm bảo promotion đã persist trước khi email bay đi

---

## 6. Thanh toán — PayOS

### 6.1 Luồng thanh toán QR/Bank Transfer

```
1. Tạo booking → Invoice (UNPAID)
2. PaymentService.createPaymentLink(invoiceId)
   → PayOS SDK: {orderCode, amount, items, returnUrl, cancelUrl}
   → Trả về checkoutUrl (trang QR PayOS)
3. Angular redirect user → trang QR PayOS
4. User quét QR / chuyển khoản
5a. PayOS Webhook callback → verifyAndProcessPayment(orderCode)
5b. Angular polling verify sau khi redirect về returnUrl
6. Cập nhật: Booking→CONFIRMED, Invoice→PAID
7. Gửi email hóa đơn (afterCommit)
```

**Lý do dùng PayOS thay vì tích hợp VNPay/MoMo trực tiếp:**
- PayOS hỗ trợ QR liên ngân hàng (VietQR) → khách dùng mọi app ngân hàng
- SDK Java chính thức, dễ tích hợp
- Checksum verification chống giả mạo callback
- Free tier đủ cho mục đích đồ án

**Lý do có cả Webhook lẫn verify endpoint:**
- Webhook: real-time, nhưng có thể bị miss nếu server restart
- Verify (polling từ Angular): backup đảm bảo trạng thái đồng bộ
- 2 cơ chế song song → robust hơn

### 6.2 Bảo mật thanh toán

- `finalAmount` tính tại backend, không trust giá từ client
- `orderCode` = `invoiceId` → map 1-1, không thể giả mạo invoice khác
- Checksum PayOS verify bằng HMAC-SHA256 với `checksumKey`

---

## 7. Quản lý trạng thái phòng

### 7.1 Room Status State Machine

```
AVAILABLE ──[walk-in / check-in]──► OCCUPIED
AVAILABLE ──[bảo trì]─────────────► MAINTENANCE
OCCUPIED  ──[check-out]───────────► DIRTY
DIRTY     ──[dọn xong]────────────► AVAILABLE
AVAILABLE ──[đặt trước future]────► RESERVED (manual)
```

### 7.2 Tại sao online booking không đổi Room.status?

- Online booking (CONFIRMED) = đặt tương lai, khách chưa đến
- Room vẫn là `AVAILABLE` cho đến lúc check-in thực tế
- Công suất tính từ **booking records** (không từ room.status) → chính xác hơn

| Booking Flow | Room.status thay đổi? |
|---|---|
| Online đặt phòng (PENDING/CONFIRMED) | ❌ Không — vẫn AVAILABLE |
| Walk-in tạo ngay tại quầy | ✅ → OCCUPIED |
| Check-in (online booking) | ✅ → OCCUPIED |
| Check-out | ✅ → DIRTY |
| Hủy booking CHECK_IN | ✅ → AVAILABLE |

### 7.3 Tính công suất từ booking records

```sql
-- Đếm phòng đang có booking active hôm nay
SELECT COUNT(DISTINCT bd.room_id)
FROM booking_details bd
INNER JOIN bookings b ON bd.booking_id = b.id
WHERE b.status IN ('CONFIRMED', 'CHECK_IN', 'CHECK_OUT')
AND b.check_in_date <= :day AND b.check_out_date > :day
```

**Lý do include CHECK_OUT:**
- CHECK_OUT = khách đã trả phòng và đã thu tiền đủ
- Tính vào lịch sử công suất (phòng đó đã được dùng trong khoảng thời gian đó)
- Hiển thị chính xác heatmap calendar ngày quá khứ

**Lý do exclude CANCELLED (kể cả REFUNDED):**
- Khi hoàn tiền: `booking.status = CANCELLED` → tự động bị loại khỏi query
- Phòng đã hoàn tiền = không đóng góp vào công suất thực tế

---

## 8. Lịch đặt phòng & Calendar Heatmap

### 8.1 Blocked dates trên trang chi tiết phòng

```java
// Backend trả về danh sách khoảng ngày đã đặt
bookingRepository.findBookedRangesForRoom(roomId, from, to)
// WHERE status IN (PENDING, CONFIRMED, CHECK_IN)
// AND checkOut > from AND checkIn < to
```

Angular nhận mảng `[{checkIn, checkOut}]` → disable những ngày đó trên date picker.

### 8.2 Occupancy Calendar Heatmap

```java
// Mỗi ngày trong tháng → % công suất
int pct = (int) Math.min(Math.round(occupied * 100.0 / totalRooms), 100);
```

**Màu sắc:**
| % | Màu |
|---|-----|
| 0% | Xám nhạt |
| 1–29% | Xanh lá |
| 30–59% | Vàng |
| 60–89% | Cam |
| 90–100% | Đỏ |

**Click vào ngày → Chi tiết ngày:**
- Số phòng CHECK_IN / CONFIRMED / CHECK_OUT
- Danh sách phòng + tên khách + ngày ở

---

## 9. Dashboard & Thống kê

### 9.1 KPI Cards

| Metric | Cách tính |
|---|---|
| Doanh thu hôm nay | `SUM(invoice.amountPaid)` WHERE date = today |
| Đặt phòng hôm nay | `COUNT(booking)` WHERE createdAt = today |
| Công suất (%) | `(totalRooms - availableRooms) / totalRooms × 100` |
| ADR (Giá TB/phòng) | `totalRevenuePeriod / occupiedRooms` |

### 9.2 RevenueScheduler — Snapshot doanh thu cuối ngày

```java
@Scheduled(cron = "0 55 23 * * ?")  // 23:55 mỗi ngày
public void autoUpdateDailyStats() {
    // Tính doanh thu ngày → lưu bảng daily_revenue_stats
}
```

**Lý do dùng snapshot thay vì tính realtime:**
- Tránh query nặng `SUM` toàn bảng invoice khi load dashboard
- Dashboard lịch sử dùng snapshot → response < 10ms
- Tradeoff: dữ liệu ngày hôm nay vẫn tính realtime, lịch sử dùng snapshot

### 9.3 Booking Status Statistics

```sql
SELECT status, COUNT(*) as count FROM bookings GROUP BY status
```

Trả về phân bổ: PENDING / CONFIRMED / CANCELLED / CHECK_IN / CHECK_OUT  
→ Hiển thị pie chart tổng quan đơn hàng

---

## 10. Chatbot AI (Gemini RAG)

### 10.1 RAG — Retrieval-Augmented Generation

Thay vì để AI "hallucinate" thông tin phòng, hệ thống inject dữ liệu thực từ DB vào system prompt:

```
ChatContextService.buildContext(role, userId):

GUEST/CUSTOMER:
  → Danh sách phòng AVAILABLE (max 15), giá từ RoomType.basePrice
  → Format: "Phòng 101 | Loại: Deluxe | Tầng: 1 | Sức chứa: 2 | Giá: 800,000 VND | ID: 5"

CUSTOMER đã đăng nhập:
  → Thêm 3 đơn đặt gần nhất của user đó

STAFF/ADMIN:
  → Thêm thống kê vận hành: OCCUPIED, DIRTY, MAINTENANCE, RESERVED
```

**Lý do RAG thay vì fine-tune model:**
- Dữ liệu phòng thay đổi liên tục → fine-tune sẽ lỗi thời ngay
- RAG inject dữ liệu fresh từ DB mỗi request → luôn cập nhật
- Không tốn chi phí fine-tune (hàng nghìn USD)

### 10.2 Streaming SSE — Hiệu ứng typing

```
/api/chat/stream → Flux<String> (text/event-stream)
→ Gemini streamGenerateContent?alt=sse
→ Parse từng "data: {}" line → emit chunk
→ Angular nhận chunk → append dần vào bubble
```

**Lý do dùng SSE thay vì WebSocket:**
- Chat là one-way stream (server → client) → SSE đủ dùng, đơn giản hơn
- SSE native với HTTP/1.1, không cần upgrade protocol
- Spring WebFlux `Flux<String>` support SSE built-in

**Lý do dùng `Mono<String>` cho /send (không dùng `.block()`):**
- App chạy trên Reactor Netty → `.block()` trên reactor-nio thread bị cấm
- Refactor toàn bộ pipeline sang reactive: `bodyToMono → map → retryWhen → onErrorResume`

### 10.3 Rate Limit Handling (429)

```java
.retryWhen(Retry.backoff(2, Duration.ofSeconds(3))
    .filter(e -> e instanceof WebClientResponseException.TooManyRequests)
    .maxBackoff(Duration.ofSeconds(10)))
.onErrorResume(e -> {
    if (e instanceof WebClientResponseException wce && wce.getStatusCode().value() == 429)
        return Mono.just("Dạ, trợ lý AI đang bận. Vui lòng thử lại sau 1–2 phút!");
    return Mono.just(fallback(dto.getMessage()));
});
```

**Lý do chọn `gemini-2.0-flash`:**

| Model | RPM (free tier) | RPD |
|---|---|---|
| gemini-2.5-flash | 10 | 500 |
| **gemini-2.0-flash** | **15** | **1500** |

→ 15 RPM + 3x RPD cao hơn, ít gặp 429 hơn

**Lý do exponential backoff:**
- Retry ngay lập tức → 429 tiếp theo ngay
- Backoff 3s → 6s → 10s → cho API time "hồi phục"
- Max 2 retry → không chặn user quá lâu (tối đa ~20 giây)

### 10.4 Room Card HTML trong chat

```html
<div class="room-card">
  <div class="room-name">Phòng 101 – Deluxe</div>
  <div class="room-meta">Tầng 1 • Tối đa 2 khách</div>
  <div class="room-price">800,000 VND/đêm</div>
  <a href="/rooms/5" class="book-btn">🛏 Xem & Đặt phòng</a>
</div>
```

**Lý do dùng HTML thay vì Markdown:**
- Angular chat widget render `[innerHTML]="msg.text | safeHtml"` → HTML render được
- Markdown `**bold**` hiển thị literal dấu `**` trong innerHTML
- HTML card có style riêng, hover effect, link click trực tiếp đến trang phòng

**Lý do link tương đối `/rooms/[ID]` thay vì absolute URL:**
- `http://localhost:4200/rooms/5` → lỗi trên production
- `/rooms/5` → Angular Router xử lý đúng mọi môi trường

---

## 11. Email tự động

### 11.1 Các loại email

| Loại | Trigger | Nội dung |
|---|---|---|
| OTP đăng ký | Sau khi tạo tài khoản | Mã 6 số kích hoạt |
| OTP quên mật khẩu | Yêu cầu reset | Mã 6 số reset |
| Hóa đơn BANK_TRANSFER | Booking confirm payment | PDF-like HTML invoice |
| Hóa đơn PayOS | Sau khi webhook xác nhận | HTML invoice |
| Hóa đơn CASH | Sau check-out | HTML invoice |
| Khuyến mãi mới | Admin tạo promotion | Thông báo ưu đãi |

### 11.2 Pattern `afterCommit()` — Email an toàn

```java
TransactionSynchronizationManager.registerSynchronization(
    new TransactionSynchronization() {
        @Override public void afterCommit() {
            emailService.sendInvoiceEmail(...);
        }
    }
);
```

**Lý do dùng `afterCommit()` thay vì gửi thẳng:**
- Nếu DB rollback sau khi gửi email → email đã bay, không rollback được
- `afterCommit()` chỉ gửi khi transaction commit thành công 100%
- Áp dụng cho tất cả: invoice email, promotion email

### 11.3 SMTP Configuration

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.properties.mail.smtp.starttls.enable=true  # STARTTLS
spring.mail.properties.mail.smtp.connectiontimeout=15000
```

**Lý do dùng port 587 + STARTTLS thay vì 465 + SSL:**
- Port 587 là chuẩn RFC 6409 (Submission port) hiện đại hơn
- STARTTLS: bắt đầu plain text → upgrade lên TLS trong cùng connection
- Port 465 (SMTPS) bị deprecated dù vẫn hoạt động

---

## 12. Audit Log

```java
// Ghi lại mọi thao tác quan trọng
saveAuditLog(userId, action, entityId, detail);
// action: CREATE_BOOKING, CONFIRM_PAYMENT, CANCEL_BOOKING, CHECK_IN, CHECK_OUT, REFUND
```

**Lý do cần Audit Log:**
- Tracing: tìm nguyên nhân sự cố (ai làm gì, lúc nào)
- Accountability: chứng minh nhân viên có thực hiện thao tác không
- Compliance: lưu vết giao dịch tài chính

**Thông tin lưu:**
- `userId` — ai thực hiện
- `action` — hành động gì
- `entityId` — tác động lên booking/invoice nào
- `detail` — mô tả chi tiết (lý do hủy, số tiền...)
- `createdAt` — timestamp

---

## 13. Scheduler tự động

### 13.1 BookingCleanupScheduler — Hủy đơn PENDING quá hạn

```java
@Scheduled(fixedRate = 30_000)  // Mỗi 30 giây
public void autoCancelExpiredBookings() {
    LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
    // 1. Tìm bookingIds PENDING quá 5 phút
    // 2. Giải phóng rooms về AVAILABLE
    // 3. Hủy invoices → CANCELLED
    // 4. Hủy bookings → CANCELLED
}
```

**Lý do cần cleanup scheduler:**
- User tạo booking PENDING nhưng không thanh toán (đóng tab, lỗi mạng...)
- Phòng bị "treo" → người khác không đặt được
- 5 phút đủ để user hoàn tất thanh toán PayOS

**Lý do 3 bước riêng biệt thay vì 1 query JOIN:**
- `releaseRoomsByBookingIds` update hàng loạt bằng sub-query → hiệu quả hơn N+1
- Tách bước đảm bảo nếu 1 bước lỗi, bước khác không bị ảnh hưởng
- Transaction rollback từng bước rõ ràng

### 13.2 RevenueScheduler — Snapshot doanh thu

```java
@Scheduled(cron = "0 55 23 * * ?")  // 23:55 mỗi ngày
```

**Lý do chạy 23:55 thay vì 00:00:**
- Tránh xung đột với các job hệ thống khác thường chạy đúng nửa đêm
- 5 phút đệm để các giao dịch cuối ngày settle xong

---

## 14. Bảo mật API

### 14.1 Spring Security Filter Chain

```
Request → JwtAuthenticationFilter → SecurityConfig → Controller
              ↓
         Parse Bearer token
         Inject Authentication → SecurityContext
```

**Các endpoint public (không cần JWT):**
- `POST /api/auth/**` — đăng nhập, đăng ký, OTP
- `GET /api/rooms/**` — xem phòng
- `GET /api/chat/**` — chatbot (anonymous)
- `GET /api/payment/webhook` — PayOS callback

### 14.2 Ngăn chặn các lỗ hổng phổ biến

| Lỗ hổng | Biện pháp |
|---|---|
| SQL Injection | JPA/JPQL parameterized queries — không nối string SQL |
| XSS | Angular escape HTML mặc định; chatbot dùng `DomSanitizer` |
| IDOR | `request.setUserId(currentUser.getId())` — không tin userId từ client |
| JWT tampering | HS256 signature verify ở mọi request |
| Coupon bypass | Backend recalculate discount độc lập |
| Concurrent booking | Pessimistic Lock + Room Hold |

### 14.3 CORS

```java
@CrossOrigin("*")  // Dev/demo
// Production: cấu hình domain cụ thể
```

---

## 15. Frontend Angular

### 15.1 Standalone Components (Angular 17)

Không dùng NgModule — mỗi component tự khai báo `imports` riêng.

**Lý do:**
- Giảm boilerplate NgModule
- Tree-shaking tốt hơn → bundle nhỏ hơn
- Xu hướng Angular hiện đại từ v15+

### 15.2 HTTP Interceptor — Auto-inject JWT

```typescript
// auth.interceptor.ts
intercept(req, next) {
    const token = localStorage.getItem('token');
    if (token) {
        req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
    }
    return next.handle(req);
}
```

**Lý do dùng interceptor:**
- Không cần thêm header thủ công ở mỗi service call
- Single point of responsibility cho auth header
- Dễ xử lý 401 → redirect login tại 1 chỗ

### 15.3 Route Guards

```typescript
// auth.guard.ts  → redirect /login nếu chưa đăng nhập
// admin.guard.ts → redirect /home nếu không phải ADMIN
```

### 15.4 SafeHtml Pipe — Render HTML chatbot an toàn

```typescript
@Pipe({ name: 'safeHtml' })
export class SafeHtmlPipe implements PipeTransform {
    transform(value: string): SafeHtml {
        return this.sanitizer.bypassSecurityTrustHtml(value);
    }
}
```

```html
<div [innerHTML]="msg.text | safeHtml"></div>
```

**Lý do cần SafeHtml:**
- Angular mặc định escape toàn bộ HTML trong `[innerHTML]`
- Chatbot trả về HTML card với `<div>`, `<a>`, `<b>` → cần render thật
- `bypassSecurityTrustHtml` báo Angular: "đã trusted, không cần escape"
- Chỉ dùng với content từ backend của mình (không dùng với user input)

### 15.5 SSE Client cho chatbot streaming

```typescript
// Dùng fetch() native thay vì EventSource vì cần POST body
const response = await fetch('/api/chat/stream', {
    method: 'POST',
    body: JSON.stringify(dto),
    headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` }
});
const reader = response.body!.getReader();
// Đọc từng chunk → decode → append vào bubble
```

**Lý do dùng `fetch` + `ReadableStream` thay vì `EventSource`:**
- `EventSource` chỉ hỗ trợ GET, không gửi được JSON body hoặc custom headers (JWT)
- `fetch` + `ReadableStream` linh hoạt hơn, hỗ trợ POST + Authorization header

---

## Tổng hợp lý do chọn công nghệ

| Công nghệ | Thay thế cân nhắc | Lý do chọn |
|---|---|---|
| Spring Boot | Quarkus, Micronaut | Ecosystem mature, tài liệu nhiều, phù hợp JPA |
| Spring WebFlux | Spring MVC thuần | Cần SSE streaming cho chatbot, non-blocking I/O |
| MySQL 8 | PostgreSQL | InnoDB row-level lock, phổ biến ở VN, trigger support |
| JWT HS256 | Session, RS256 | Stateless, SPA-friendly, đủ cho single-server |
| BCrypt | SHA-256, Argon2 | Built-in Spring Security, adaptive cost, battle-tested |
| PayOS | VNPay, MoMo | VietQR liên ngân hàng, SDK Java chính thức |
| Gemini 2.0 Flash | GPT-4o, Claude | Free tier 15 RPM, multilingual VN tốt, cost = 0 |
| Angular 17 | React, Vue | Standalone Components, strong typing, enterprise-grade |
| Chart.js | D3.js, ApexCharts | Nhẹ, dễ dùng với Angular, đủ cho doughnut + bar |
| `ConcurrentHashMap` | Redis, DB lock | Room hold tạm thời, không cần persist, hiệu năng cao |

---

*Tài liệu được tạo ngày 2026-06-15*
