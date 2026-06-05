import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { RoomService } from '../../services/room.service';
import { RoomResponseDTO as RoomModelDTO } from '../../models/room.model'; // Đổi tên alias để tránh trùng lặp nếu cần
import { HeaderComponent } from '../../components/header/header.component';
import { HttpClient, HttpParams } from '@angular/common/http';

// 🔥 ĐÃ SỬA BƯỚC 3: Gộp chung ReviewResponseDTO lấy từ Service tập trung, xóa bỏ khối khai báo cứng cũ ở đầu trang
import { ReviewService, ReviewRequestDTO, ReviewResponseDTO } from '../../services/review.service'; 

// Import Booking Service
import { BookingService, BookingRequestDTO } from '../../services/booking.service';

@Component({
  selector: 'app-room-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, HeaderComponent, FormsModule],
  templateUrl: './room-detail.component.html',
  styleUrl: './room-detail.component.scss'
})
export class RoomDetailComponent implements OnInit {
  // --- STATE QUẢN LÝ THÔNG TIN PHÒNG ---
  room!: RoomModelDTO; // Sử dụng DTO từ Room Model
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
  isBooking: boolean = false; 

  // --- STATE ĐÁNH GIÁ & BÌNH LUẬN ---
  userRating: number = 0;
  hoveredStar: number = 0;
  reviewContent: string = ''; 
  isSubmittingReview: boolean = false; 
  currentBookingId: number = 1; 

  // Mảng động hứng dữ liệu bình luận từ MySQL
  reviews: ReviewResponseDTO[] = []; 

  constructor(
    private route: ActivatedRoute,
    private roomService: RoomService,
    private reviewService: ReviewService,
    private bookingService: BookingService, 
    private router: Router,
    private http: HttpClient 
  ) {}

  ngOnInit(): void {
    const roomIdStr = this.route.snapshot.paramMap.get('id');
    if (roomIdStr) {
      const roomId = Number(roomIdStr);
      this.loadRoomDetail(roomId);
      this.loadRoomReviews(roomId); // Tự động kích hoạt nạp review động
    } else {
      this.router.navigate(['/rooms']);
    }

    const today = new Date();
    const tomorrow = new Date();
    tomorrow.setDate(today.getDate() + 1); 

    this.checkInDate = this.formatDateToYYYYMMDD(today);
    this.checkOutDate = this.formatDateToYYYYMMDD(tomorrow);
  }

  // ==========================================
  // LUỒNG TẢI ĐÁNH GIÁ THỰC TẾ TỪ BACKEND
  // ==========================================
  loadRoomReviews(roomId: number): void {
    this.reviewService.getReviewsByRoomId(roomId).subscribe({
      next: (data: ReviewResponseDTO[]) => {
        this.reviews = data; // Đổ mảng dữ liệu thật từ database vào giao diện
      },
      error: (err: any) => {
        console.error('Hệ thống: Không thể tải danh sách bình luận của phòng này:', err);
      }
    });
  }

  // 🔥 Hàm tính trung bình cộng số sao real-time từ mảng reviews
  getAverageRating(): number {
    if (!this.reviews || this.reviews.length === 0) return 0;
    const totalStars = this.reviews.reduce((sum, item) => sum + item.rating, 0);
    return totalStars / this.reviews.length;
  }

  // ==========================================
  // XỬ LÝ DỮ LIỆU PHÒNG & HÌNH ẢNH
  // ==========================================
  loadRoomDetail(id: number): void {
    this.isLoading = true;
    this.roomService.getRoomById(id).subscribe({
      next: (data: any) => {
        this.room = data;
        
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
    this.discount = 0; 
    this.updateFinalPrice();
  }

  applyCoupon(): void {
    const cleanCode = this.couponCode.trim();
    if (!cleanCode) {
      alert('Vui lòng nhập mã giảm giá trước khi bấm áp dụng!');
      return;
    }

    const baseAmount = (this.room?.price || 0) * this.numberOfNights;

    const params = new HttpParams()
      .set('code', cleanCode)
      .set('amount', baseAmount.toString());

    this.http.get<any>('http://localhost:8080/api/bookings/management/promotions/validate', { params }).subscribe({
      next: (res) => {
        this.discount = res.discountAmount;
        this.updateFinalPrice();
        alert(`Áp dụng mã khuyến mãi thành công! Bạn được giảm ${this.formatPrice(this.discount)}`);
      },
      error: (err) => {
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

  // ==========================================
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

    const payload: any = {
      roomId: this.room.roomId!, 
      checkIn: this.checkInDate,
      checkOut: this.checkOutDate,
      paymentMethod: 'PAYOS',
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
      error: (err: any) => {
        this.isSubmittingReview = false;
        alert(err.error?.message || 'Có lỗi xảy ra, vui lòng thử lại.');
      }
    });
  }

  private formatDateToYYYYMMDD(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}