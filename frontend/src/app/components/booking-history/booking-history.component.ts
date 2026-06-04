import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common'; 
import { RouterModule, Router } from '@angular/router'; 
import { FormsModule } from '@angular/forms'; 
// 🔑 ĐỒNG BỘ: Nhập trực tiếp Interface chuẩn có đầy đủ roomId, roomNumber, roomType từ file Service gốc
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

  currentFilter: string = 'ALL'; 
  totalBookingsCount = 0;
  activeBookingsCount = 0;
  completedBookingsCount = 0;
  totalSpentAmount = 0;

  selectedBooking: BookingHistoryResponseDTO | null = null;
  isDetailModalOpen: boolean = false;

  reviewBookingId: number | null = null;
  isReviewModalOpen: boolean = false;
  reviewRating: number = 5; 
  reviewComment: string = '';

  constructor(private bookingService: BookingManagementService, private router: Router) {}

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
        console.error('Lỗi kết nối API lịch sử:', err);
        this.errorMessage = err.error?.message || 'Không thể tải danh sách lịch sử lúc này!';
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

  openDetailModal(booking: BookingHistoryResponseDTO): void {
    this.selectedBooking = booking;
    this.isDetailModalOpen = true;
  }

  closeDetailModal(): void {
    this.selectedBooking = null;
    this.isDetailModalOpen = false;
  }

  // =========================================================================
  // ⚡ LUỒNG ĐẶT LẠI - ĐIỀU HƯỚNG THẲNG ĐẾN ĐÍCH DANH CHI TIẾT CĂN PHÒNG PHÒNG
  // =========================================================================
  reBookRoom(booking: BookingHistoryResponseDTO): void {
    // 1. Kiểm tra an toàn biến động trường ID phòng tránh lỗi Undefined ngoài giao diện
    if (!booking.roomId) {
      alert('Không tìm thấy thông tin phòng vật lý để thực hiện luồng đặt lại!');
      return;
    }

    // 2. Logging vết hệ thống lên DevTools phục vụ debug nhanh
    console.log(`Đang chuyển hướng người dùng đến trang chi tiết của phòng ID: #${booking.roomId}`);

    // 3. Thực hiện điều hướng kèm mốc thời gian lưu trú cũ làm gợi ý mặc định
    this.router.navigate([`/rooms/${booking.roomId}`], {
      queryParams: {
        checkIn: booking.checkInDate,
        checkOut: booking.checkOutDate
      }
    });
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
            </table>

            <h3 style="color: #111827; margin-top: 30px;">Chi Tiết Dịch Vụ Lưu Trú</h3>
            <table class="details-table">
              <thead>
                <tr>
                  <th>Nội dung hiển thị</th>
                  <th>Số phòng</th>
                  <th>Loại phòng</th>
                  <th>Thời gian kỳ nghỉ</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>Thuê phòng lưu trú trực tuyến</td>
                  <td><strong>Phòng ${booking.roomNumber}</strong></td>
                  <td>${booking.roomType}</td>
                  <td>${new Date(booking.checkInDate).toLocaleDateString('vi-VN')} ➔ ${new Date(booking.checkOutDate).toLocaleDateString('vi-VN')}</td>
                </tr>
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

  openReviewModal(bookingId: number): void {
    this.reviewBookingId = bookingId;
    this.reviewRating = 5; 
    this.reviewComment = '';
    this.isReviewModalOpen = true;
  }

  closeReviewModal(): void {
    this.isReviewModalOpen = false;
    this.reviewBookingId = null;
  }

  submitReview(): void {
    if (!this.reviewComment.trim()) {
      alert('Vui lòng nhập nội dung đánh giá trước khi gửi!');
      return;
    }
    alert(`Cảm ơn bạn đã đánh giá ${this.reviewRating}/5 sao cho kỳ nghỉ này!`);
    const updatedBooking = this.allBookings.find(b => b.bookingId === this.reviewBookingId);
    if (updatedBooking) {
      updatedBooking.canReview = false; 
    }
    this.closeReviewModal();
  }

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
}