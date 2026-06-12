import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { RoomService } from '../../services/room.service';
import { RoomResponseDTO as RoomModelDTO } from '../../models/room.model'; 
import { HeaderComponent } from '../../components/header/header.component';
import { HttpClient, HttpParams } from '@angular/common/http';
import { ReviewService, ReviewRequestDTO, ReviewResponseDTO } from '../../services/review.service'; 
import { BookingService, BookingRequestDTO } from '../../services/booking.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-room-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, HeaderComponent, FormsModule],
  templateUrl: './room-detail.component.html',
  styleUrl: './room-detail.component.scss'
})
export class RoomDetailComponent implements OnInit {
  room!: RoomModelDTO; 
  isLoading = true;
  selectedImage: string = ''; 
  currentIndex: number = 0; 

  checkInDate: string = ''; 
  checkOutDate: string = '';
  numberOfNights: number = 1;
  couponCode: string = '';
  discount: number = 0;
  totalPrice: number = 0;
  isBooking: boolean = false; 

  userRating: number = 0;
  hoveredStar: number = 0;
  reviewContent: string = ''; 
  isSubmittingReview: boolean = false; 
  currentBookingId: number = 1; 

  reviews: ReviewResponseDTO[] = []; 

  constructor(
    private route: ActivatedRoute,
    private roomService: RoomService,
    private reviewService: ReviewService,
    private bookingService: BookingService, 
    private router: Router,
    private http: HttpClient,
    public authService: AuthService 
  ) {}

  ngOnInit(): void {
    const roomIdStr = this.route.snapshot.paramMap.get('id');
    if (roomIdStr) {
      const roomId = Number(roomIdStr);
      this.loadRoomDetail(roomId);
      this.loadRoomReviews(roomId); 
    } else {
      this.router.navigate(['/rooms']);
    }

    const today = new Date();
    const tomorrow = new Date();
    tomorrow.setDate(today.getDate() + 1); 

    this.checkInDate = this.formatDateToYYYYMMDD(today);
    this.checkOutDate = this.formatDateToYYYYMMDD(tomorrow);
  }

  loadRoomReviews(roomId: number): void {
    this.reviewService.getReviewsByRoomId(roomId).subscribe({
      next: (data: ReviewResponseDTO[]) => {
        this.reviews = data; 
      },
      error: (err: any) => {
        console.error('Hệ thống: Không thể tải danh sách bình luận:', err);
      }
    });
  }

  getAverageRating(): number {
    if (!this.reviews || this.reviews.length === 0) return 0;
    const totalStars = this.reviews.reduce((sum, item) => sum + item.rating, 0);
    return totalStars / this.reviews.length;
  }

  loadRoomDetail(id: number): void {
    this.isLoading = true;
    this.roomService.getRoomById(id).subscribe({
      next: (data: RoomModelDTO) => {
        this.room = data;
        
        // Xử lý ảnh bìa
        const roomAny = this.room as any;
        if (roomAny.albumImages && roomAny.albumImages.length > 0) {
          this.selectedImage = roomAny.albumImages[0];
          this.currentIndex = 0;
        } else {
          this.selectedImage = this.room.imageUrl || 'https://images.unsplash.com/photo-1582719478250-c89404bb8a0e?q=80&w=1000&auto=format&fit=crop';
        }
        
        // 🔥 ĐÃ SỬA: Chỉ tự động áp dụng mã giảm giá khi người dùng ĐÃ ĐĂNG NHẬP
        if (this.room.appliedPromotions && this.room.appliedPromotions.length > 0) {
          this.couponCode = this.room.appliedPromotions[0].code;
          
          if (this.authService.isLoggedIn()) {
            setTimeout(() => {
              this.applyCoupon();
            }, 300);
          }
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
    // FIX TS2339
    const album = (this.room as any).albumImages;
    if (album && this.currentIndex < album.length - 1) {
      this.currentIndex++;
      this.selectedImage = album[this.currentIndex];
    }
  }

  prevImage(): void {
    // FIX TS2339
    const album = (this.room as any).albumImages;
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

  calculateTotal(): void {
    if (!this.checkInDate || !this.checkOutDate) return;
    const start = new Date(this.checkInDate);
    const end = new Date(this.checkOutDate);
    const diffDays = Math.max(1, Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)));
    this.numberOfNights = diffDays;
    this.discount = 0; 
    this.updateFinalPrice();
  }

  onCouponChange(value: string): void {
    if (!value || value.trim() === '') {
      this.discount = 0;
      this.updateFinalPrice();
    }
  }

 applyCoupon(): void {
    // 1. [CHỐT CHẶN BẢO MẬT]: Kiểm tra trạng thái đăng nhập đầu tiên
    if (!this.authService.isLoggedIn()) {
      this.redirectToLogin(); // Gọi hàm thông báo và chuyển hướng đã tạo ở bước trước
      return; // Cắt luồng thực thi ngay lập tức, tuyệt đối không gọi API
    }

    // 2. [XỬ LÝ DỮ LIỆU ĐẦU VÀO]: Chuẩn hóa và kiểm tra mã giảm giá
    const cleanCode = this.couponCode.trim();
    if (!cleanCode) {
      alert('Vui lòng nhập mã giảm giá trước khi bấm áp dụng!');
      return;
    }

    // 3. [GỌI API]: Chuẩn bị tham số và gửi request
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
        // Bắt lỗi từ server (ví dụ: mã hết hạn, sai mã)
        alert(err.error?.message || 'Mã giảm giá không hợp lệ!');
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

  bookRoom(): void {
    // 1. [CHỐT CHẶN BẢO MẬT]: Bắt buộc phải đăng nhập mới được gọi API
    if (!this.authService.isLoggedIn()) {
      this.redirectToLogin();
      return; // Cắt luồng ngay lập tức, ngăn chặn lỗi 403
    }

    // 2. [KIỂM TRA DỮ LIỆU ĐẦU VÀO]: Validate ngày tháng hợp lệ
    if (!this.checkInDate || !this.checkOutDate) { 
      alert('Vui lòng chọn ngày nhận và trả phòng!'); 
      return; 
    }
    if (new Date(this.checkOutDate) <= new Date(this.checkInDate)) { 
      alert('Ngày trả phòng phải sau ngày nhận!'); 
      return; 
    }

    // 3. [KHÓA GIAO DIỆN]: Bật cờ loading, chặn người dùng double-click
    this.isBooking = true; 

    // 4. [CHUẨN BỊ DỮ LIỆU GỬI ĐI]: Đóng gói Payload
    const payload = {
      roomId: this.room.roomId!, 
      checkIn: this.checkInDate,
      checkOut: this.checkOutDate,
      paymentMethod: 'PAYOS',
      couponCode: this.discount > 0 ? this.couponCode.trim().toUpperCase() : null 
    };

    // 5. [GIAO TIẾP VỚI SERVER]: Gọi API tạo Booking
    this.bookingService.createBooking(payload).subscribe({
      next: (response) => {
        // Phân luồng luân chuyển sau khi đặt phòng thành công
        if (response.checkoutUrl) {
          // Luồng 1: Có URL cổng thanh toán -> Đẩy sang trang thanh toán của bên thứ 3
          window.location.href = response.checkoutUrl;
        } else { 
          // Luồng 2: Thanh toán sau / Trả tiền mặt -> Chuyển về lịch sử đặt phòng
          alert('Đặt phòng thành công!'); 
          this.router.navigate(['/history']); 
        }
      },
      error: (error) => {
        // [QUAN TRỌNG]: Bắt buộc phải "nhả" khóa UI ra để người dùng có thể thử lại
        this.isBooking = false; 
        alert('Lỗi đặt phòng: ' + (error.error?.message || 'Có lỗi xảy ra.'));
      }
    });
  }

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
    // 1. [CHỐT CHẶN BẢO MẬT]: Bắt buộc đăng nhập mới được đánh giá
    if (!this.authService.isLoggedIn()) {
      this.redirectToLogin();
      return; // Ngăn request bay xuống server gây lỗi 403
    }

    // 2. [KIỂM TRA DỮ LIỆU ĐẦU VÀO]: Báo lỗi rõ ràng thay vì return im lặng
    if (!this.userRating) {
      alert('Vui lòng chọn số sao để đánh giá trải nghiệm của bạn!');
      return;
    }
    if (!this.reviewContent.trim()) {
      alert('Vui lòng nhập nội dung đánh giá!');
      return;
    }

    // Chặn người dùng spam click nhiều lần
    if (this.isSubmittingReview) {
      return; 
    }

    // 3. [KHÓA UI & ĐÓNG GÓI PAYLOAD]
    this.isSubmittingReview = true;
    const payload: ReviewRequestDTO = { 
      bookingId: this.currentBookingId, 
      rating: this.userRating, 
      comment: this.reviewContent, 
      mediaUrls: [] 
    };

    // 4. [GỌI API]
    this.reviewService.submitReview(payload).subscribe({
      next: (response: ReviewResponseDTO) => {
        // Đẩy bình luận mới nhất lên đầu danh sách
        this.reviews.unshift(response);
        
        // Reset form và nhả khóa UI (Nên tách dòng để code dễ đọc, dễ bảo trì)
        this.userRating = 0; 
        this.reviewContent = ''; 
        this.isSubmittingReview = false;
        
        alert('Cảm ơn bạn đã gửi đánh giá!');
      },
      error: (err: any) => {
        // [QUAN TRỌNG]: Phải nhả khóa UI ra nếu server báo lỗi
        this.isSubmittingReview = false;
        alert(err.error?.message || 'Có lỗi xảy ra khi gửi đánh giá. Vui lòng thử lại.');
      }
    });
  }

  private formatDateToYYYYMMDD(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
  redirectToLogin(): void {
    alert('Vui lòng đăng nhập để thực hiện chức năng này!');
    this.router.navigate(['/login'], { 
      queryParams: { returnUrl: this.router.url } // Đăng nhập xong sẽ quay lại trang này
    });
  }
}