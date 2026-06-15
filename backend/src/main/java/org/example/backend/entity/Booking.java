package org.example.backend.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.*;
import org.example.backend.entity.enums.BookingStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ĐÃ SỬA: Chuyển nullable thành true để Lễ tân đặt phòng tại quầy cho khách vãng lai không cần tạo Account
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    // =========================================================================
    // 🪪 ĐÃ BỔ SUNG: 3 THUỘC TÍNH PHÁP LÝ HỨNG DỮ LIỆU KHÁCH QUẦY (ĐỐI SOÁT CÔNG AN)
    // =========================================================================
    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_cccd", length = 20)
    private String customerCccd;

    @Column(name = "customer_phone", length = 15)
    private String customerPhone;

    @Column(name = "customer_email", length = 100)
    private String customerEmail;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @Column(name = "total_price", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalPrice; // Snapshot tổng tiền gốc ban đầu trước khi giảm giá

    // ĐÃ BỔ SUNG: Liên kết đến bảng Khuyến mãi để phục vụ Module Promotions
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    @Builder.Default // Đảm bảo Lombok Builder không ghi đè giá trị khởi tạo mặc định
    @Column(name = "discount_amount", precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO; // Số tiền được giảm

    @Column(name = "final_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal finalAmount; // Số tiền thực tế khách phải trả (Gửi sang PayOS/Tiền mặt)

    @Builder.Default // Đảm bảo Lombok Builder không ghi đè trạng thái mặc định
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id")
    private Hotel hotel;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BookingDetail> bookingDetails;

    // ĐÃ BỔ SUNG QUAN TRỌNG: Ánh xạ hai chiều tới Invoice giúp dứt điểm lỗi "Cannot resolve symbol 'invoice'"
    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private Invoice invoice;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.discountAmount == null) this.discountAmount = BigDecimal.ZERO;
        if (this.finalAmount == null) this.finalAmount = this.totalPrice;
    }
}