import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common'; 
import { RouterModule, Router } from '@angular/router'; 
import { FormsModule } from '@angular/forms'; 
// Nhập trực tiếp kiểu dữ liệu phẳng đồng bộ từ Service gốc
import { BookingManagementService, BookingHistoryResponseDTO } from '../../services/booking-management.service';

@Component({
  selector: 'app-booking-history',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule], 
  templateUrl: './booking-history.component.html',
  styleUrls: ['./booking-history.component.scss']
})
export class BookingHistoryComponent implements OnInit {
  allBookings: BookingHistoryResponseDTO[] = []; 
  filteredBookings: BookingHistoryResponseDTO[] = []; 
  isLoading: boolean = true;
  errorMessage: string = '';

  // 📊 BỘ LỌC TRẠNG THÁI HIỆN TẠI
  currentFilter: string = 'ALL'; 

  // 📊 BỘ CHỈ SỐ THỐNG KÊ DASHBOARD
  totalBookingsCount = 0;
  activeBookingsCount = 0;
  completedBookingsCount = 0;
  totalSpentAmount = 0;

  // 🔍 TRẠNG THÁI POPUP XEM CHI TIẾT
  selectedBooking: BookingHistoryResponseDTO | null = null;
  isDetailModalOpen: boolean = false;

  // ⭐ TRẠNG THÁI POPUP ĐÁNH GIÁ DỊCH VỤ
  reviewBookingId: number | null = null;
  selectedBookingForReview: BookingHistoryResponseDTO | null = null;
  isReviewModalOpen: boolean = false;
  reviewRating: number = 5;
  hoverRating: number = 0;
  reviewComment: string = '';

  constructor(
    private bookingService: BookingManagementService, 
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadHistory();
  }

  loadHistory(): void {
    this.isLoading = true;
    this.bookingService.getCustomerHistory().subscribe({
      next: (data: BookingHistoryResponseDTO[]) => {
        this.allBookings = data;
        this.filteredBookings = data; 
        this.calculateStats(); 
        this.isLoading = false;
      },
      error: (err: any) => {
        console.error('Lỗi kết nối API lịch sử phòng:', err);
        this.errorMessage = err.error?.message || 'Không thể tải danh sách lịch sử kỳ nghỉ lúc này!';
        this.isLoading = false;
      }
    });
  }

  calculateStats(): void {
    this.totalBookingsCount = this.allBookings.length;
    this.activeBookingsCount = this.allBookings.filter(b => b.status === 'CHECK_IN').length;
    this.completedBookingsCount = this.allBookings.filter(b => b.status === 'CHECK_OUT').length;
    this.totalSpentAmount = this.allBookings
      .filter(b => b.status !== 'CANCELLED')
      .reduce((sum, b) => sum + b.totalPrice, 0);
  }

  // =========================================================================
  // ⚡ SỰ KIỆN: LỌC THÔNG TIN PHÒNG THEO TỪNG CARD STATS DASHBOARD
  // =========================================================================
  filterByStatus(statusType: string): void {
    this.currentFilter = statusType;
    if (statusType === 'ALL') {
      this.filteredBookings = this.allBookings;
    } else if (statusType === 'COMPLETED') {
      this.filteredBookings = this.allBookings.filter(b => b.status === 'CHECK_OUT');
    } else {
      this.filteredBookings = this.allBookings.filter(b => b.status === statusType);
    }
  }

  // =========================================================================
  // ⚡ SỰ KIỆN: XEM CHI TIẾT ĐƠN ĐẶT PHÒNG
  // =========================================================================
  openDetailModal(booking: BookingHistoryResponseDTO): void {
    this.selectedBooking = booking;
    this.isDetailModalOpen = true;
  }

  closeDetailModal(): void {
    this.selectedBooking = null;
    this.isDetailModalOpen = false;
  }

  // =========================================================================
  // ⚡ SỰ KIỆN: ĐẶT LẠI PHÒNG - ĐIỀU HƯỚNG ĐÍCH DANH CHI TIẾT PHÒNG VỪA ĐẶT
  // =========================================================================
  reBookRoom(booking: BookingHistoryResponseDTO): void {
    if (!booking.roomId) {
      alert('Không tìm thấy thông tin phòng vật lý để thực hiện luồng đặt lại!');
      return;
    }

    console.log(`Đang điều hướng người dùng quay lại phòng ID: #${booking.roomId}`);
    
    this.router.navigate([`/rooms/${booking.roomId}`], {
      queryParams: {
        checkIn: booking.checkInDate,
        checkOut: booking.checkOutDate
      }
    });
  }

  // =========================================================================
  // ⚡ SỰ KIỆN: ĐÓNG MỞ POPUP KHỐI ĐÁNH GIÁ
  // =========================================================================
  openReviewModal(booking: BookingHistoryResponseDTO): void {
    this.reviewBookingId = booking.bookingId;
    this.selectedBookingForReview = booking;
    this.reviewRating = 5;
    this.hoverRating = 0;
    this.reviewComment = '';
    this.isReviewModalOpen = true;
  }

  closeReviewModal(): void {
    this.isReviewModalOpen = false;
    this.reviewBookingId = null;
    this.selectedBookingForReview = null;
    this.hoverRating = 0;
  }

  setReviewRating(n: number): void { this.reviewRating = n; }
  hoverStar(n: number): void      { this.hoverRating = n; }
  clearHover(): void               { this.hoverRating = 0; }
  activeStars(): number            { return this.hoverRating || this.reviewRating; }

  ratingLabel(n: number): string {
    const labels = ['', 'Rất tệ', 'Không hài lòng', 'Tạm ổn', 'Tốt', 'Hoàn hảo'];
    return labels[n] || '';
  }

  // =========================================================================
  // ⚡ ĐÃ CẬP NHẬT CHUẨN CÚ PHÁP: GỬI ĐÁNH GIÁ REAL-TIME XUỐNG API SPRING BOOT
  // =========================================================================
  submitReview(): void {
    if (!this.reviewBookingId) return;

    if (!this.reviewComment.trim()) {
      alert('Vui lòng nhập nội dung chia sẻ trải nghiệm lưu trú trước khi gửi!');
      return;
    }

    const roomId = this.selectedBookingForReview?.roomId;
    if (!roomId) {
      alert('Không xác định được phòng cần đánh giá. Vui lòng thử qua trang chi tiết phòng.');
      return;
    }
    this.bookingService.submitReview(this.reviewBookingId, roomId, this.reviewRating, this.reviewComment).subscribe({
      next: (response: any) => {
        alert(`Cảm ơn bạn đã đóng góp ý kiến ${this.reviewRating}/5 sao cho dịch vụ!`);
        
        const updatedBooking = this.allBookings.find(b => b.bookingId === this.reviewBookingId);
        if (updatedBooking) {
          updatedBooking.canReview = false; 
        }
        
        this.closeReviewModal(); 
      },
      error: (err: any) => { // 🔥 Đã sửa chính xác từ "->" sang "=>" sạch lỗi biên dịch
        console.error('Lỗi khi đẩy gói tin đánh giá lên cơ sở dữ liệu:', err);
        alert(err.error?.message || 'Có lỗi xảy ra, hệ thống chưa thể lưu nhận xét lúc này!');
      }
    });
  }

  // =========================================================================
  // HÀM TIỆN ÍCH TÍNH TOÁN HIỂN THỊ TIMELINE
  // =========================================================================
  getNightCount(checkIn: string, checkOut: string): number {
    const start = new Date(checkIn);
    const end = new Date(checkOut);
    const diffTime = Math.abs(end.getTime() - start.getTime());
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays > 0 ? diffDays : 1;
  }

  getProgress(status: string): number {
    switch (status) {
      case 'PENDING': return 25;
      case 'CONFIRMED': return 50;
      case 'CHECK_IN': return 75;
      case 'CHECK_OUT': return 100;
      case 'CANCELLED': return 100;
      default: return 0;
    }
  }

  // =========================================================================
  // ⚡ SỰ KIỆN: IN HÓA ĐƠN PDF TỪ TRÌNH DUYỆT (PRINT INVOICE TRANSACTION)
  // =========================================================================
  printInvoicePDF(booking: BookingHistoryResponseDTO): void {
    const printWindow = window.open('', '_blank');
    if (!printWindow) {
      alert('Vui lòng cho phép trình duyệt mở popup để xuất hóa đơn!');
      return;
    }

    printWindow.document.write(`
      <html>
        <head>
          <title>HoaDon_LuxeHotel_#${booking.bookingId}</title>
          <style>
            body { font-family: 'Helvetica Neue', Arial, sans-serif; color: #333; padding: 40px; line-height: 1.6; }
            .invoice-box { max-width: 800px; margin: auto; border: 1px solid #eee; padding: 30px; border-radius: 8px; box-shadow: 0 0 10px rgba(0,0,0,0.05); }
            .header-table { width: 100%; border-collapse: collapse; margin-bottom: 40px; }
            .title { font-size: 28px; font-weight: bold; color: #1e3a8a; }
            .info-table { width: 100%; border-collapse: collapse; margin-bottom: 30px; }
            .info-table td { padding: 8px; border-bottom: 1px solid #f9f9f9; text-align: left; }
            .info-table .font-b { font-weight: bold; }
            .details-table { width: 100%; border-collapse: collapse; margin-top: 20px; }
            .details-table th { background: #f4f6f9; padding: 12px; font-weight: bold; text-align: left; border-bottom: 2px solid #e5e7eb; }
            .details-table td { padding: 12px; border-bottom: 1px solid #e5e7eb; }
            .total-box { text-align: right; margin-top: 30px; font-size: 18px; font-weight: bold; color: #10b981; }
            .footer { margin-top: 50px; text-align: center; font-size: 12px; color: #9ca3af; border-top: 1px solid #eee; padding-top: 20px; }
          </style>
        </head>
        <body>
          <div class="invoice-box">
            <table class="header-table">
              <tr>
                <td class="title">LUXE HOTEL INVOICE</td>
                <td style="text-align: right;">
                  <strong>Mã hóa đơn:</strong> #LH-${booking.bookingId}<br>
                  <strong>Ngày xuất:</strong> ${new Date().toLocaleDateString('vi-VN')}
                </td>
              </tr>
            </table>

            <h3 style="color: #111827; border-bottom: 1px solid #e5e7eb; padding-bottom: 8px;">Thông Tin Khách Hàng & Giao Dịch</h3>
            <table class="info-table">
              <tr>
                <td><span class="font-b">Chi nhánh khách sạn:</span> ${booking.hotelName}</td>
                <td><span class="font-b">Trạng thái quyết toán:</span> Đã hoàn tất thanh toán</td>
              </tr>
              <tr>
                <td colspan="2"><span class="font-b">Địa chỉ chi nhánh:</span> ${booking.hotelAddress}</td>
              </tr>
              <tr>
                <td colspan="2"><span class="font-b">Số phòng đặt:</span>
                  ${booking.rooms && booking.rooms.length > 1
                    ? `<strong>${booking.rooms.length} phòng</strong>: ` + booking.rooms.map((r: any) => `Phòng ${r.roomNumber}`).join(', ')
                    : `Phòng <strong>${booking.roomNumber}</strong>`
                  }
                </td>
              </tr>
            </table>

            <h3 style="color: #111827; margin-top: 30px;">Chi Tiết Dịch Vụ Lưu Trú</h3>
            <table class="details-table">
              <thead>
                <tr>
                  <th>STT</th>
                  <th>Số phòng</th>
                  <th>Loại phòng</th>
                  <th>Thời gian kỳ nghỉ</th>
                </tr>
              </thead>
              <tbody>
                ${(booking.rooms && booking.rooms.length > 1
                  ? booking.rooms.map((r: any, i: number) => `
                    <tr>
                      <td>${i + 1}</td>
                      <td><strong>Phòng ${r.roomNumber}</strong></td>
                      <td>${r.roomType}</td>
                      <td>${new Date(booking.checkInDate).toLocaleDateString('vi-VN')} ➔ ${new Date(booking.checkOutDate).toLocaleDateString('vi-VN')}</td>
                    </tr>`).join('')
                  : `<tr>
                      <td>1</td>
                      <td><strong>Phòng ${booking.roomNumber}</strong></td>
                      <td>${booking.roomType}</td>
                      <td>${new Date(booking.checkInDate).toLocaleDateString('vi-VN')} ➔ ${new Date(booking.checkOutDate).toLocaleDateString('vi-VN')}</td>
                    </tr>`
                )}
              </tbody>
            </table>

            <div class="total-box">
              Tổng số tiền thực trả: ${booking.totalPrice.toLocaleString('vi-VN')} VND
            </div>

            <div class="footer">
              Cảm ơn quý khách đã tin tưởng và lựa chọn dịch vụ của hệ thống khách sạn LuxeHotel!<br>
              Mọi thắc mắc vui lòng liên hệ quầy lễ tân để được hỗ trợ giải quyết trực tiếp.
            </div>
          </div>
          <script>
            window.onload = function() { window.print(); window.close(); }
          </script>
        </body>
      </html>
    `);
    printWindow.document.close();
  }
  
}