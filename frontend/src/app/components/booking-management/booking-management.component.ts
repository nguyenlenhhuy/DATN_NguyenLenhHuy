import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BookingManagementService, BookingResponseDTO, WalkInBookingRequestDTO } from '../../services/booking-management.service';

@Component({
  selector: 'app-booking-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './booking-management.component.html',
  styleUrls: ['./booking-management.component.scss'] // Đã đổi sang liên kết file scss của bạn
})
export class BookingManagementComponent implements OnInit {
  bookings: BookingResponseDTO[] = [];
  availableRooms: string[] = [];
  availableVouchers: any[] = [];
  isWalkInModalOpen: boolean = false;

  previewOriginalPrice: number = 0;
  previewDiscountAmount: number = 0;
  previewFinalAmount: number = 0;

  // Biến lọc
  searchTerm: string = '';
  checkInDateFilter: string = '';

  walkInForm: WalkInBookingRequestDTO = {
    roomNumber: '', customerName: '', customerPhone: '', customerCccd: '',
    checkInDate: '', checkOutDate: '', appliedCode: ''
  };

  constructor(private bookingService: BookingManagementService) {}

  get filteredBookings(): BookingResponseDTO[] {
    return this.bookings.filter(b => {
      const matchName = b.customerName.toLowerCase().includes(this.searchTerm.toLowerCase());
      const matchDate = !this.checkInDateFilter || (b.checkInDate && b.checkInDate.startsWith(this.checkInDateFilter));
      return matchName && matchDate;
    });
  }

  ngOnInit(): void {
    this.loadBookings();
  }

  loadBookings(): void {
    this.bookingService.getAllBookings().subscribe({
      next: (data: BookingResponseDTO[]) => this.bookings = data,
      error: (err: any) => console.error(err)
    });
  }

  openWalkInModal(): void {
    const today = new Date().toISOString().split('T')[0];
    this.walkInForm = {
      roomNumber: '', customerName: '', customerPhone: '', customerCccd: '', checkInDate: today, checkOutDate: '', appliedCode: ''
    };
    this.previewOriginalPrice = 0;
    this.previewDiscountAmount = 0;
    this.previewFinalAmount = 0;

    this.bookingService.getAvailableRooms().subscribe({
      next: (rooms: string[]) => {
        this.availableRooms = rooms;
        this.bookingService.getAvailablePromotions().subscribe({
          next: (vouchers: any[]) => this.availableVouchers = vouchers,
          error: (err: any) => console.error(err)
        });
        this.isWalkInModalOpen = true;
      },
      error: (err: any) => alert(err.message)
    });
  }

  calculatePreviewPrice(): void {
    const form = this.walkInForm;
    if (!form.roomNumber || !form.checkInDate || !form.checkOutDate) return;
    this.bookingService.getPreviewPrice(form.roomNumber, form.checkInDate, form.checkOutDate, form.appliedCode).subscribe({
      next: (res: any) => {
        this.previewOriginalPrice = res.originalPrice;
        this.previewDiscountAmount = res.discountAmount;
        this.previewFinalAmount = res.finalAmount;
      }
    });
  }

  onVoucherChange(event: Event): void {
    const selectElement = event.target as HTMLSelectElement;
    this.walkInForm.appliedCode = selectElement.value;
    this.calculatePreviewPrice();
  }

  onCreateWalkInBooking(): void {
    this.bookingService.createWalkInBooking(this.walkInForm).subscribe({
      next: (res: any) => {
        alert(res.message);
        this.isWalkInModalOpen = false;
        this.loadBookings();
      }
    });
  }

  onProcessCheckIn(bookingId: number): void {
    this.bookingService.processCheckIn(bookingId).subscribe({
      next: (res: any) => { alert(res.message); this.loadBookings(); }
    });
  }

  // 🔥 HÀM MỚI: Xử lý kích hoạt hủy đơn đặt phòng treo 5 phút tại quầy
  onProcessCancel(bookingId: number): void {
    if (confirm(`Xác nhận thực hiện HỦY đơn đặt phòng #${bookingId} và giải phóng phòng vật lý về kho trống?`)) {
      const staffId = Number(localStorage.getItem('staffId') || 1);
      
      // Đảm bảo hàm cancelBooking(bookingId, staffId) đã được bạn khai báo trong booking-management.service.ts
      this.bookingService.cancelBooking(bookingId, staffId).subscribe({
        next: (res: any) => {
          alert(res.message || 'Đã hủy đơn hàng và mở khóa phòng trống thành công!');
          this.loadBookings();
        },
        error: (err: any) => alert('Hủy đơn thất bại: ' + (err.error?.message || err.message))
      });
    }
  }

  onProcessCheckOut(bookingId: number): void {
    this.bookingService.processCheckOut(bookingId).subscribe({
      next: (res: any) => { alert(res.message); this.loadBookings(); }
    });
  }
}