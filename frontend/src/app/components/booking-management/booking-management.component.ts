import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

// Import chính xác cấu trúc Token và DTO từ file service chung
import { BookingManagementService, BookingResponseDTO, WalkInBookingRequestDTO } from '../../services/booking-management.service';

@Component({
  selector: 'app-booking-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './booking-management.component.html',
  styles: []
})
export class BookingManagementComponent implements OnInit {
  bookings: BookingResponseDTO[] = [];
  availableRooms: string[] = []; 
  availableVouchers: any[] = []; 
  isWalkInModalOpen: boolean = false; 

  // Các biến phục vụ hiển thị giá tạm tính trực quan trên Modal Form
  previewOriginalPrice: number = 0;
  previewDiscountAmount: number = 0;
  previewFinalAmount: number = 0;

  walkInForm: WalkInBookingRequestDTO = {
    roomNumber: '',
    customerName: '',
    customerPhone: '',
    customerCccd: '',
    checkInDate: '',
    checkOutDate: '',
    appliedCode: ''
  };

  constructor(private bookingService: BookingManagementService) {}

  ngOnInit(): void {
    this.loadBookings();
  }

  /**
   * Tải danh sách đơn đặt phòng hiển thị lên bảng chính
   */
  loadBookings(): void {
    this.bookingService.getAllBookings().subscribe({
      next: (data: BookingResponseDTO[]) => this.bookings = data,
      error: (err: any) => console.error('Lỗi tải danh sách đặt phòng:', err)
    });
  }

  /**
   * Mở Modal popup và tự động tải song song danh sách phòng trống + voucher khả dụng
   */
  openWalkInModal(): void {
    const today = new Date().toISOString().split('T')[0];
    this.walkInForm = {
      roomNumber: '',
      customerName: '',
      customerPhone: '',
      customerCccd: '',
      checkInDate: today,
      checkOutDate: '',
      appliedCode: ''
    };

    // Reset sạch các trường giá trị tạm tính về 0 khi khởi tạo form mới
    this.previewOriginalPrice = 0;
    this.previewDiscountAmount = 0;
    this.previewFinalAmount = 0;

    // Bước 1: Gọi API tải danh sách phòng trống vật lý
    this.bookingService.getAvailableRooms().subscribe({
      next: (rooms: string[]) => {
        this.availableRooms = rooms;
        
        // Bước 2: Gọi tiếp API tải danh sách voucher khuyến mãi đang chạy
        this.bookingService.getAvailablePromotions().subscribe({
          next: (vouchers: any[]) => {
            this.availableVouchers = vouchers;
            this.isWalkInModalOpen = true; // Chỉ mở modal hiển thị khi toàn bộ dữ liệu gợi ý đã nạp xong
          },
          error: (err: any) => console.error('Không thể lấy danh sách voucher gợi ý:', err)
        });

      },
      error: (err: any) => alert('Không thể lấy danh sách phòng trống gợi ý: ' + err.message)
    });
  }

  /**
   * 🎯 HÀM DUY NHẤT: Gọi API xuống Backend lấy giá phòng thực tế từ DB thời gian thực
   */
  calculatePreviewPrice(): void {
    const form = this.walkInForm;
    
    // Nếu chưa chọn đủ thông tin cốt lõi thì không gọi API
    if (!form.roomNumber || !form.checkInDate || !form.checkOutDate) {
      this.previewOriginalPrice = 0;
      this.previewDiscountAmount = 0;
      this.previewFinalAmount = 0;
      return;
    }

    // Gọi API Backend tính toán dựa trên loại phòng thực tế cấu hình trong DB
    this.bookingService.getPreviewPrice(form.roomNumber, form.checkInDate, form.checkOutDate, form.appliedCode).subscribe({
      next: (res: any) => {
        this.previewOriginalPrice = res.originalPrice;
        this.previewDiscountAmount = res.discountAmount;
        this.previewFinalAmount = res.finalAmount;
      },
      error: (err: any) => {
        console.error('Lỗi tính giá tạm tính:', err);
        this.previewOriginalPrice = 0;
        this.previewDiscountAmount = 0;
        this.previewFinalAmount = 0;
      }
    });
  }

  /**
   * Xử lý thay đổi Voucher khi lễ tân tương tác Dropdown menu
   */
  onVoucherChange(event: Event): void {
    const selectElement = event.target as HTMLSelectElement;
    this.walkInForm.appliedCode = selectElement.value;
    this.calculatePreviewPrice(); // Tính lại tiền ngay lập tức khi đổi mã voucher
  }

  /**
   * Nghiệp vụ gửi dữ liệu đặt phòng tại quầy lên Backend
   */
  onCreateWalkInBooking(): void {
    if (!this.walkInForm.roomNumber || !this.walkInForm.customerName || 
        !this.walkInForm.customerPhone || !this.walkInForm.customerCccd || 
        !this.walkInForm.checkInDate || !this.walkInForm.checkOutDate) {
      alert('Vui lòng nhập đầy đủ các trường thông tin bắt buộc (*)!');
      return;
    }

    this.bookingService.createWalkInBooking(this.walkInForm).subscribe({
      next: (res: any) => {
        alert(res.message || 'Khởi tạo đơn đặt phòng tại quầy thành công!');
        this.isWalkInModalOpen = false; 
        this.loadBookings(); 
      },
      error: (err: any) => alert('Lỗi khởi tạo đơn: ' + (err.error?.message || err.message))
    });
  }

  /**
   * Thao tác làm thủ tục Nhận phòng trực tiếp tại quầy
   */
  onProcessCheckIn(bookingId: number): void {
    if (confirm('Xác nhận làm thủ tục nhận phòng (Check-in) cho đơn này?')) {
      this.bookingService.processCheckIn(bookingId).subscribe({
        next: (res: any) => {
          alert(res.message);
          this.loadBookings();
        },
        error: (err: any) => alert('Lỗi Check-in: ' + err.message)
      });
    }
  }

  /**
   * Thao tác làm thủ tục Trả phòng và quyết toán hóa đơn tiền phòng
   */
  onProcessCheckOut(bookingId: number): void {
    if (confirm('Xác nhận làm thủ tục trả phòng và quyết toán hóa đơn?')) {
      this.bookingService.processCheckOut(bookingId).subscribe({
        next: (res: any) => {
          alert(res.message);
          this.loadBookings();
        },
        error: (err: any) => alert('Lỗi Check-out: ' + err.message)
      });
    }
  }
}