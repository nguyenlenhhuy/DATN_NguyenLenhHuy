import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';


import { RoomService } from '../../services/room.service';
import { RoomResponseDTO } from '../../models/room.model';
import { HeaderComponent } from '../../components/header/header.component';
import { HttpClient, HttpParams } from '@angular/common/http';

// Import Review Service
import { ReviewService, ReviewRequestDTO } from '../../services/review.service'; 

// Import Booking Service
import { BookingService, BookingRequestDTO } from '../../services/booking.service';

// Chuẩn hóa Interface khớp với Backend ReviewResponseDTO
export interface ReviewResponseDTO {
  id?: number;
  userName: string;
  rating: number;
  comment: string;
  replyContent?: string;
  createdAt: string | Date;
  mediaUrls?: string[];
}

@Component({
  selector: 'app-room-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, HeaderComponent, FormsModule],
  templateUrl: './room-detail.component.html',
  styleUrl: './room-detail.component.scss'
})
export class RoomDetailComponent implements OnInit {
  // --- STATE QUẢN LÝ THÔNG TIN PHÒNG ---
  room!: RoomResponseDTO;
  isLoading = true;
  selectedImage: string = ''; 
  currentIndex: number = 0; 

  // --- STATE TÍNH TOÁN ĐẶT PHÒNG ---
  checkInDate: string = ''; 
  checkOutDate: string = '';
  numberOfNights: number = 1;
  couponCode: string = '';
  discount: number = 0;
  totalPrice: number = 0;
  isBooking: boolean = false; // Cờ chặn spam click khi đang gọi API đặt phòng

  // --- STATE ĐÁNH GIÁ & BÌNH LUẬN ---
  userRating: number = 0;
  hoveredStar: number = 0;
  reviewContent: string = ''; 
  isSubmittingReview: boolean = false; 
  currentBookingId: number = 1; // Trong thực tế, ID này cần lấy từ lịch sử đặt phòng của user

  // Danh sách review
  reviews: ReviewResponseDTO[] = [
    {
      id: 1,
      userName: 'Trần Văn A',
      rating: 5,
      comment: 'Phòng rất đẹp và sạch sẽ. View nhìn ra biển tuyệt vời đúng như trên ảnh.',
      createdAt: new Date('2024-05-20'),
    }
  ];

  constructor(
    private route: ActivatedRoute,
    private roomService: RoomService,
    private reviewService: ReviewService,
    private bookingService: BookingService, // Inject BookingService
    private router: Router,
    private http: HttpClient // 🔥 BỔ SUNG: Tiêm HttpClient vào đây để sử dụng
  ) {}

  ngOnInit(): void {
    const roomIdStr = this.route.snapshot.paramMap.get('id');
    if (roomIdStr) {
      const roomId = Number(roomIdStr);
      this.loadRoomDetail(roomId);
    } else {
      this.router.navigate(['/rooms']);
    }

    // Tự động tính ngày Hôm nay & Ngày mai cho phân hệ Đặt phòng thực tế
    const today = new Date();
    const tomorrow = new Date();
    tomorrow.setDate(today.getDate() + 1); 

    // Định dạng về chuẩn YYYY-MM-DD thông qua hàm helper ở dưới
    this.checkInDate = this.formatDateToYYYYMMDD(today);
    this.checkOutDate = this.formatDateToYYYYMMDD(tomorrow);
  }

  // ==========================================
  // XỬ LÝ DỮ LIỆU PHÒNG & HÌNH ẢNH
  // ==========================================

  loadRoomDetail(id: number): void {
    this.isLoading = true;
    this.roomService.getRoomById(id).subscribe({
      next: (data: RoomResponseDTO) => {
        this.room = data;
        
        // Đồng bộ chuẩn hóa theo biến `imageUrls` từ Model thực tế của bạn
        if (this.room.imageUrls && this.room.imageUrls.length > 0) {
          this.selectedImage = this.room.imageUrls[0];
          this.currentIndex = 0;
        } else {
          this.selectedImage = this.room.imageUrl || 'https://images.unsplash.com/photo-1582719478250-c89404bb8a0e?q=80&w=1000&auto=format&fit=crop';
        }
        
        this.isLoading = false;
        this.calculateTotal(); 
      },
      error: (err) => {
        console.error('Lỗi không thể tải chi tiết phòng:', err);
        this.isLoading = false;
      }
    });
  }

  changePreviewImage(url: string, index: number): void {
    this.selectedImage = url;
    this.currentIndex = index;
  }

  nextImage(): void {
    const album = this.room.imageUrls;
    if (album && this.currentIndex < album.length - 1) {
      this.currentIndex++;
      this.selectedImage = album[this.currentIndex];
    }
  }

  prevImage(): void {
    const album = this.room.imageUrls;
    if (album && this.currentIndex > 0) {
      this.currentIndex--;
      this.selectedImage = album[this.currentIndex];
    }
  }

  formatPrice(price: number | undefined | null): string {
    if (price === undefined || price === null) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  }

  goBack(): void {
    this.router.navigate(['/rooms']);
  }

  // ==========================================
  // XỬ LÝ TÍNH TOÁN TIỀN PHÒNG
  // ==========================================

calculateTotal(): void {
    if (!this.checkInDate || !this.checkOutDate) return;
    
    const start = new Date(this.checkInDate);
    const end = new Date(this.checkOutDate);
    const diffDays = Math.max(1, Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)));
    
    this.numberOfNights = diffDays;

    // 💡 TỐI ƯU THỰC TẾ: Nếu khách thay đổi ngày ở, reset số tiền giảm giá về 0 
    // để ép khách phải ấn lại nút áp dụng, tránh bug lợi dụng số tiền cũ.
    this.discount = 0; 
    this.updateFinalPrice();
  }

  /**
   * 🔥 CẶP NHẬT CHUẨN: Hàm áp dụng mã giảm giá an toàn, xử lý triệt để ký tự đặc biệt (&, %)
   */
  applyCoupon(): void {
    const cleanCode = this.couponCode.trim();
    if (!cleanCode) {
      alert('Vui lòng nhập mã giảm giá trước khi bấm áp dụng!');
      return;
    }

    // Tính số tiền gốc tạm thời (Giá phòng x Số đêm) để gửi lên cho Backend kiểm tra điều kiện
    const baseAmount = (this.room?.price || 0) * this.numberOfNights;

    // 🌟 GIẢI PHÁP ĐỘC QUYỀN: Dùng HttpParams bọc param để Angular tự động Encode URL
    // Ví dụ: "GIAMGIA99%" sẽ tự động được biến đổi thành "GIAMGIA99%25" an toàn 100%
    const params = new HttpParams()
      .set('code', cleanCode)
      .set('amount', baseAmount.toString());

    // Bắn request kèm params đối tượng thay vì nối chuỗi url động
    this.http.get<any>('http://localhost:8080/api/bookings/management/promotions/validate', { params }).subscribe({
      next: (res) => {
        // Backend phản hồi dạng: { valid: true, discountAmount: X, finalAmount: Y }
        this.discount = res.discountAmount;
        this.updateFinalPrice();
        alert(`Áp dụng mã khuyến mãi thành công! Bạn được giảm ${this.formatPrice(this.discount)}`);
      },
      error: (err) => {
        // Bóc tách câu báo lỗi cụ thể từ RuntimeException dưới Backend
        const errorMsg = err.error?.message || 'Mã giảm giá không hợp lệ hoặc đã bị vô hiệu hóa!';
        alert(errorMsg);
        this.discount = 0;
        this.updateFinalPrice();
      }
    });
  }

  updateFinalPrice(): void {
    if (this.room && this.room.price) {
      this.totalPrice = (this.room.price * this.numberOfNights) - this.discount;
    }
  }
  // XỬ LÝ ĐẶT PHÒNG & THANH TOÁN PAYOS
  // ==========================================

  bookRoom(): void {
    if (!this.checkInDate || !this.checkOutDate) {
      alert('Vui lòng chọn ngày nhận và trả phòng!');
      return;
    }

    const start = new Date(this.checkInDate);
    const end = new Date(this.checkOutDate);
    if (end <= start) {
      alert('Ngày trả phòng phải sau ngày nhận phòng!');
      return;
    }

    this.isBooking = true; 

    // Đúc gói payload truyền dữ liệu lên API Đặt phòng
    const payload: any = {
      roomId: this.room.roomId!, 
      checkIn: this.checkInDate,
      checkOut: this.checkOutDate,
      paymentMethod: 'PAYOS',
      // 🔥 BỔ SUNG: Gửi kèm mã coupon lên nếu có áp dụng thành công, để Backend tính lại tiền khi làm việc với PayOS
      couponCode: this.discount > 0 ? this.couponCode.trim().toUpperCase() : null 
    };

    this.bookingService.createBooking(payload).subscribe({
      next: (response) => {
        if (response.checkoutUrl) {
          window.location.href = response.checkoutUrl;
        } else {
          alert('Đặt phòng thành công!');
          this.router.navigate(['/history']);
        }
      },
      error: (error) => {
        this.isBooking = false;
        const errorMsg = error.error?.message || error.error || 'Có lỗi xảy ra, vui lòng thử lại.';
        alert('Lỗi đặt phòng: ' + errorMsg);
        console.error('Lỗi khi gọi API Đặt phòng:', error);
      }
    });
  }

  // ==========================================
  // XỬ LÝ ĐÁNH GIÁ VÀ BÌNH LUẬN
  // ==========================================

  getRatingText(rating: number): string {
    const texts = ['Tệ', 'Chưa tốt', 'Bình thường', 'Rất tốt', 'Tuyệt vời'];
    return texts[rating - 1] || '';
  }

  getInitials(name: string): string {
    if (!name) return 'U';
    const words = name.trim().split(' ');
    return words[words.length - 1].charAt(0).toUpperCase();
  }

  submitReview(): void {
    if (!this.userRating || !this.reviewContent.trim() || this.isSubmittingReview) return;
    
    this.isSubmittingReview = true;
    
    const payload: ReviewRequestDTO = {
      bookingId: this.currentBookingId,
      rating: this.userRating,
      comment: this.reviewContent,
      mediaUrls: []
    };

    this.reviewService.submitReview(payload).subscribe({
      next: (response: ReviewResponseDTO) => {
        this.reviews.unshift(response);
        
        this.userRating = 0;
        this.reviewContent = '';
        this.isSubmittingReview = false;
        
        alert('Cảm ơn bạn đã gửi đánh giá!');
      },
      error: (err: Error) => {
        this.isSubmittingReview = false;
        alert(err.message || 'Có lỗi xảy ra, vui lòng thử lại.');
      }
    });
  }

  // ==========================================
  // ⚙️ HÀM HELPER ĐỊNH DẠNG NGÀY KHÔNG LỆCH MÚI GIỜ
  // ==========================================
  private formatDateToYYYYMMDD(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}