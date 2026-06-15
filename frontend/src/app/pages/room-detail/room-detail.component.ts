import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';

interface CalendarDay {
  date: string;   // 'YYYY-MM-DD'
  day: number;
  isPast: boolean;
  isBooked: boolean;
  isWeekend: boolean;
}

interface CalendarMonth {
  label: string;
  year: number;
  month: number;
  days: (CalendarDay | null)[];  // null = ô padding đầu tháng
}
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
import { RoomHoldService } from '../../services/room-hold.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-room-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, HeaderComponent, FormsModule],
  templateUrl: './room-detail.component.html',
  styleUrl: './room-detail.component.scss'
})
export class RoomDetailComponent implements OnInit, OnDestroy {
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
  currentBookingId: number = 0;

  fromBookingId: number | null = null;
  canReviewFromBooking: boolean = false;
  alreadyReviewed: boolean = false;
  reviewCheckLoading: boolean = false;

  reviews: ReviewResponseDTO[] = [];
  activeTab: string = 'intro';

  // Availability Calendar
  bookedRanges: { checkIn: string; checkOut: string }[] = [];
  calendarMonths: CalendarMonth[] = [];
  dateConflictError = '';
  showCalendar = false;
  bookingError = '';

  // Room Hold
  holdToken: string | null = null;
  isHeldByOther = false;
  holdHeldByOtherMessage = '';
  holdCountdown = '';
  private currentRoomId: number | null = null;
  private holdTimerId: any = null;
  private renewTimerId: any = null;

  constructor(
    private route: ActivatedRoute,
    private roomService: RoomService,
    private reviewService: ReviewService,
    private bookingService: BookingService,
    private router: Router,
    private http: HttpClient,
    public authService: AuthService,
    private roomHoldService: RoomHoldService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const roomIdStr = this.route.snapshot.paramMap.get('id');
    if (roomIdStr) {
      const roomId = Number(roomIdStr);
      this.currentRoomId = roomId;
      this.loadRoomDetail(roomId);
      this.loadRoomReviews(roomId);
      this.loadRoomAvailability(roomId);
      // Hold KHÔNG kích hoạt khi xem trang — chỉ kích hoạt khi bấm "Đặt phòng"
    } else {
      this.router.navigate(['/rooms']);
    }

    const fromBookingStr = this.route.snapshot.queryParamMap.get('fromBooking');
    if (fromBookingStr && this.authService.isLoggedIn()) {
      this.fromBookingId = Number(fromBookingStr);
      this.currentBookingId = this.fromBookingId;
      this.activeTab = 'reviews';
      // roomId sẽ được đọc sau khi loadRoomDetail hoàn tất (room.roomId)
    }

    const today = new Date();
    const tomorrow = new Date();
    tomorrow.setDate(today.getDate() + 1);

    this.checkInDate = this.formatDateToYYYYMMDD(today);
    this.checkOutDate = this.formatDateToYYYYMMDD(tomorrow);
  }

  checkReviewEligibility(bookingId: number, roomId: number): void {
    this.reviewCheckLoading = true;
    this.reviewService.checkCanReview(bookingId, roomId).subscribe({
      next: (res) => {
        this.canReviewFromBooking = res.canReview;
        this.alreadyReviewed = !res.canReview && res.reason === 'already_reviewed';
        this.reviewCheckLoading = false;
      },
      error: () => {
        this.canReviewFromBooking = false;
        this.reviewCheckLoading = false;
      }
    });
  }

  ngOnDestroy(): void {
    if (this.holdTimerId) clearInterval(this.holdTimerId);
    if (this.renewTimerId) clearInterval(this.renewTimerId);
    if (this.currentRoomId && this.holdToken) {
      this.roomHoldService.releaseHold(this.currentRoomId, this.holdToken).subscribe();
    }
  }

  private acquireRoomHold(roomId: number, onSuccess: () => void): void {
    this.roomHoldService.acquireHold(roomId).subscribe({
      next: (res) => {
        if (res.success && res.holdToken) {
          this.holdToken = res.holdToken;
          this.isHeldByOther = false;
          this.startHoldCountdown(res.expiresInSeconds ?? 600);
          this.startHoldRenew(roomId);
          onSuccess();
        } else {
          this.isBooking = false;
          this.isHeldByOther = true;
          this.holdHeldByOtherMessage = res.message || 'Phòng đang được người khác xử lý. Vui lòng thử lại sau.';
        }
      },
      error: (err) => {
        this.isBooking = false;
        this.isHeldByOther = true;
        this.holdHeldByOtherMessage = err.error?.message || 'Phòng đang được người khác xử lý. Vui lòng thử lại sau.';
      }
    });
  }

  private startHoldCountdown(seconds: number): void {
    if (this.holdTimerId) clearInterval(this.holdTimerId);
    let remaining = seconds;
    const tick = () => {
      if (remaining <= 0) {
        clearInterval(this.holdTimerId);
        this.holdToken = null;
        this.holdCountdown = '';
        return;
      }
      const m = Math.floor(remaining / 60);
      const s = remaining % 60;
      this.holdCountdown = `${m}:${String(s).padStart(2, '0')}`;
      remaining--;
    };
    tick();
    this.holdTimerId = setInterval(tick, 1000);
  }

  private startHoldRenew(roomId: number): void {
    if (this.renewTimerId) clearInterval(this.renewTimerId);
    this.renewTimerId = setInterval(() => {
      if (this.holdToken) {
        this.roomHoldService.renewHold(roomId, this.holdToken).subscribe(res => {
          if (res.success && res.expiresInSeconds) {
            this.startHoldCountdown(res.expiresInSeconds);
          }
        });
      }
    }, 5 * 60 * 1000);
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

        // Tự mở lịch khi phòng không AVAILABLE để hướng dẫn đặt trước
        if (data.status !== 'AVAILABLE' && data.status !== 'MAINTENANCE') {
          this.showCalendar = true;
        }

        // Kiểm tra quyền đánh giá sau khi đã có roomId từ room data
        if (this.fromBookingId && this.room.roomId) {
          this.checkReviewEligibility(this.fromBookingId, this.room.roomId);
        }
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

    this.http.get<any>(`${environment.apiUrl}/bookings/management/promotions/validate`, { params }).subscribe({
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
    if (!this.authService.isLoggedIn()) {
      this.redirectToLogin();
      return;
    }
    if (!this.checkInDate || !this.checkOutDate) {
      this.bookingError = 'Vui lòng chọn ngày nhận và trả phòng!';
      return;
    }
    if (new Date(this.checkOutDate) <= new Date(this.checkInDate)) {
      this.bookingError = 'Ngày trả phòng phải sau ngày nhận phòng!';
      return;
    }
    if (!this.validateDates()) {
      // dateConflictError đã được set bởi validateDates()
      return;
    }

    this.isBooking = true;
    this.bookingError = '';
    this.isHeldByOther = false;

    const doSubmit = () => {
      const payload: BookingRequestDTO = {
        roomId: this.room.roomId!,
        checkIn: this.checkInDate,
        checkOut: this.checkOutDate,
        paymentMethod: 'PAYOS',
        couponCode: this.discount > 0 ? this.couponCode.trim().toUpperCase() : undefined,
        holdToken: this.holdToken ?? undefined
      };
      this.bookingService.createBooking(payload).subscribe({
        next: (response) => {
          if (response.checkoutUrl) {
            window.location.href = response.checkoutUrl;
          } else {
            this.router.navigate(['/history']);
          }
        },
        error: (error) => {
          this.isBooking = false;
          const msg: string = error.error?.message || 'Có lỗi xảy ra, vui lòng thử lại.';
          // Nếu server báo conflict ngày → cập nhật lịch và hiển thị lỗi tại chỗ
          if (error.status === 409) {
            this.bookingError = msg;
            if (this.currentRoomId) this.loadRoomAvailability(this.currentRoomId);
          } else {
            this.bookingError = msg;
          }
        }
      });
    };

    // Acquire hold at booking time (not on page load)
    if (this.currentRoomId && !this.holdToken) {
      this.acquireRoomHold(this.currentRoomId, doSubmit);
    } else {
      doSubmit();
    }
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
      roomId: this.room.roomId!,
      rating: this.userRating,
      comment: this.reviewContent,
      mediaUrls: []
    };

    // 4. [GỌI API]
    this.reviewService.submitReview(payload).subscribe({
      next: (response: ReviewResponseDTO) => {
        this.reviews.unshift(response);
        this.userRating = 0;
        this.reviewContent = '';
        this.isSubmittingReview = false;
        this.canReviewFromBooking = false;
        this.alreadyReviewed = true;
        alert('Cảm ơn bạn đã gửi đánh giá!');
      },
      error: (err: any) => {
        // [QUAN TRỌNG]: Phải nhả khóa UI ra nếu server báo lỗi
        this.isSubmittingReview = false;
        alert(err.error?.message || 'Có lỗi xảy ra khi gửi đánh giá. Vui lòng thử lại.');
      }
    });
  }

  setTab(tab: string): void {
    this.activeTab = tab;
  }

  private formatDateToYYYYMMDD(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  // ─── Availability Calendar ───────────────────────────────────────────────

  loadRoomAvailability(roomId: number): void {
    this.roomService.getRoomAvailability(roomId).subscribe({
      next: (ranges) => {
        this.bookedRanges = ranges;
        this.buildCalendar();
        this.cdr.markForCheck();
      },
      error: () => {}
    });
  }

  buildCalendar(): void {
    const today = new Date();
    const monthNames = ['Tháng 1','Tháng 2','Tháng 3','Tháng 4','Tháng 5','Tháng 6',
                        'Tháng 7','Tháng 8','Tháng 9','Tháng 10','Tháng 11','Tháng 12'];
    this.calendarMonths = [];

    for (let m = 0; m < 2; m++) {
      const d = new Date(today.getFullYear(), today.getMonth() + m, 1);
      const year  = d.getFullYear();
      const month = d.getMonth();
      const daysInMonth   = new Date(year, month + 1, 0).getDate();
      const firstDayOfWeek = (new Date(year, month, 1).getDay() + 6) % 7; // Mon=0…Sun=6

      const days: (CalendarDay | null)[] = [];
      for (let p = 0; p < firstDayOfWeek; p++) days.push(null);

      const todayStart = new Date(today.getFullYear(), today.getMonth(), today.getDate());
      for (let day = 1; day <= daysInMonth; day++) {
        const dateObj = new Date(year, month, day);
        const dateStr = this.formatDateToYYYYMMDD(dateObj);
        const isPast  = dateObj < todayStart;
        const isBooked   = !isPast && this.isDateInBookedRange(dateStr);
        const isWeekend  = dateObj.getDay() === 0 || dateObj.getDay() === 6;
        days.push({ date: dateStr, day, isPast, isBooked, isWeekend });
      }

      this.calendarMonths.push({ label: `${monthNames[month]} ${year}`, year, month, days });
    }
  }

  isDateInBookedRange(dateStr: string): boolean {
    return this.bookedRanges.some(r => dateStr >= r.checkIn && dateStr < r.checkOut);
  }

  isDateInSelectedRange(dateStr: string): boolean {
    if (!this.checkInDate || !this.checkOutDate) return false;
    return dateStr > this.checkInDate && dateStr < this.checkOutDate;
  }

  onDateChange(): void {
    this.bookingError = '';
    this.calculateTotal();
    this.validateDates();
  }

  validateDates(): boolean {
    if (!this.checkInDate || !this.checkOutDate) {
      this.dateConflictError = '';
      return true;
    }
    if (this.checkOutDate <= this.checkInDate) {
      this.dateConflictError = 'Ngày trả phòng phải sau ngày nhận phòng!';
      return false;
    }
    const hasConflict = this.bookedRanges.some(r =>
      this.checkInDate < r.checkOut && this.checkOutDate > r.checkIn
    );
    if (hasConflict) {
      this.dateConflictError = 'Khoảng thời gian này đã có người đặt. Vui lòng chọn ngày khác!';
      return false;
    }
    this.dateConflictError = '';
    return true;
  }

  get isRoomBookable(): boolean {
    return this.room?.status !== 'MAINTENANCE';
  }

  get isAdvanceBooking(): boolean {
    return this.room?.status === 'OCCUPIED'
        || this.room?.status === 'DIRTY'
        || this.room?.status === 'RESERVED';
  }

  get statusLabel(): string {
    switch (this.room?.status) {
      case 'OCCUPIED':  return 'có khách đang ở';
      case 'DIRTY':     return 'đang được dọn dẹp';
      case 'RESERVED':  return 'đã có lịch đặt';
      default:          return 'không trống';
    }
  }

  get isTodayAvailable(): boolean {
    const today = this.formatDateToYYYYMMDD(new Date());
    return !this.isDateInBookedRange(today);
  }

  get nextAvailableFrom(): string | null {
    const today = this.formatDateToYYYYMMDD(new Date());
    if (!this.isDateInBookedRange(today)) return null;
    const checkout = this.bookedRanges
      .filter(r => r.checkIn <= today && r.checkOut > today)
      .map(r => r.checkOut)
      .sort()[0];
    if (!checkout) return null;
    const d = new Date(checkout);
    return `${d.getDate()}/${d.getMonth() + 1}/${d.getFullYear()}`;
  }

  redirectToLogin(): void {
    alert('Vui lòng đăng nhập để thực hiện chức năng này!');
    this.router.navigate(['/login'], { 
      queryParams: { returnUrl: this.router.url } // Đăng nhập xong sẽ quay lại trang này
    });
  }
}