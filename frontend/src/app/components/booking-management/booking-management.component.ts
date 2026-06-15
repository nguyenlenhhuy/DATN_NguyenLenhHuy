import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BookingManagementService, BookingResponseDTO, WalkInBookingRequestDTO, AvailableRoomInfo, AuditLogDTO } from '../../services/booking-management.service';

@Component({
  selector: 'app-booking-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './booking-management.component.html',
  styleUrls: ['./booking-management.component.scss']
})
export class BookingManagementComponent implements OnInit {
  bookings: BookingResponseDTO[] = [];
  availableRoomsDetail: AvailableRoomInfo[] = [];
  availableVouchers: any[] = [];
  isWalkInModalOpen: boolean = false;

  selectedRoomNumbers: string[] = [];
  filterFloor: string = '';
  filterType: string = '';
  formErrors: Record<string, string> = {};

  previewOriginalPrice: number = 0;
  previewDiscountAmount: number = 0;
  previewFinalAmount: number = 0;

  searchTerm: string = '';
  checkInDateFilter: string = '';

  isAuditModalOpen = false;
  auditLogs: AuditLogDTO[] = [];
  auditBookingId: number | null = null;
  auditLoading = false;

  isRefundModalOpen = false;
  refundBookingId: number | null = null;
  refundBookingAmount: number = 0;
  refundReason = '';
  refundError = '';
  refundOperatorName = '';
  refundTimestamp: Date = new Date();

  walkInForm: WalkInBookingRequestDTO = {
    roomNumber: '', customerName: '', customerPhone: '', customerCccd: '',
    customerEmail: '', checkInDate: '', checkOutDate: '', appliedCode: ''
  };

  constructor(private bookingService: BookingManagementService) {}

  get filteredBookings(): BookingResponseDTO[] {
    return this.bookings.filter(b => {
      const matchName = b.customerName.toLowerCase().includes(this.searchTerm.toLowerCase());
      const matchDate = !this.checkInDateFilter || (b.checkInDate && b.checkInDate.startsWith(this.checkInDateFilter));
      return matchName && matchDate;
    });
  }

  get uniqueFloors(): string[] {
    return [...new Set(this.availableRoomsDetail.map(r => r.floor?.toString() ?? ''))].filter(f => !!f).sort();
  }

  get uniqueTypes(): string[] {
    return [...new Set(this.availableRoomsDetail.map(r => r.typeName))].filter(t => !!t).sort();
  }

  get filteredRooms(): AvailableRoomInfo[] {
    return this.availableRoomsDetail.filter(r => {
      const matchFloor = !this.filterFloor || r.floor?.toString() === this.filterFloor;
      const matchType = !this.filterType || r.typeName === this.filterType;
      return matchFloor && matchType;
    });
  }

  ngOnInit(): void {
    this.loadBookings();
  }

  loadBookings(): void {
    this.bookingService.getAllBookings().subscribe({
      next: (data) => this.bookings = data,
      error: (err) => console.error(err)
    });
  }

  toggleRoom(roomNumber: string): void {
    const idx = this.selectedRoomNumbers.indexOf(roomNumber);
    if (idx >= 0) this.selectedRoomNumbers.splice(idx, 1);
    else this.selectedRoomNumbers.push(roomNumber);
    if (this.selectedRoomNumbers.length > 0) this.calculatePreviewPrice();
    else { this.previewOriginalPrice = 0; this.previewDiscountAmount = 0; this.previewFinalAmount = 0; }
    delete this.formErrors['rooms'];
  }

  isSelected(roomNumber: string): boolean {
    return this.selectedRoomNumbers.includes(roomNumber);
  }

  clearError(key: string): void { delete this.formErrors[key]; }

  openWalkInModal(): void {
    const today = new Date().toISOString().split('T')[0];
    this.walkInForm = {
      roomNumber: '', customerName: '', customerPhone: '', customerCccd: '',
      customerEmail: '', checkInDate: today, checkOutDate: '', appliedCode: ''
    };
    this.selectedRoomNumbers = [];
    this.filterFloor = '';
    this.filterType = '';
    this.formErrors = {};
    this.previewOriginalPrice = 0;
    this.previewDiscountAmount = 0;
    this.previewFinalAmount = 0;
    this.isWalkInModalOpen = true;
    this.refreshAvailableRooms();
    this.bookingService.getAvailablePromotions().subscribe({
      next: (v) => this.availableVouchers = v,
      error: (err) => console.error(err)
    });
  }

  refreshAvailableRooms(): void {
    const { checkInDate, checkOutDate } = this.walkInForm;
    this.bookingService.getAvailableRoomsDetail(checkInDate || undefined, checkOutDate || undefined).subscribe({
      next: (rooms) => {
        this.availableRoomsDetail = rooms;
        const availNums = rooms.map(r => r.roomNumber);
        this.selectedRoomNumbers = this.selectedRoomNumbers.filter(n => availNums.includes(n));
        if (this.selectedRoomNumbers.length > 0) this.calculatePreviewPrice();
        else { this.previewOriginalPrice = 0; this.previewDiscountAmount = 0; this.previewFinalAmount = 0; }
      },
      error: (err) => console.error('Lỗi tải danh sách phòng:', err)
    });
  }

  onDateChange(): void {
    if (this.walkInForm.checkInDate && this.walkInForm.checkOutDate) {
      this.refreshAvailableRooms();
    }
    this.calculatePreviewPrice();
  }

  calculatePreviewPrice(): void {
    if (!this.selectedRoomNumbers.length || !this.walkInForm.checkInDate || !this.walkInForm.checkOutDate) return;
    this.bookingService.getPreviewPriceMulti(
      this.selectedRoomNumbers, this.walkInForm.checkInDate, this.walkInForm.checkOutDate, this.walkInForm.appliedCode
    ).subscribe({
      next: (res) => {
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

  validateForm(): boolean {
    this.formErrors = {};
    if (!this.selectedRoomNumbers.length)
      this.formErrors['rooms'] = 'Vui lòng chọn ít nhất 1 phòng';
    if (!this.walkInForm.customerName?.trim())
      this.formErrors['name'] = 'Vui lòng nhập tên khách hàng';
    if (!this.walkInForm.customerPhone?.trim()) {
      this.formErrors['phone'] = 'Vui lòng nhập số điện thoại';
    } else if (!/^[0-9]{10,11}$/.test(this.walkInForm.customerPhone.trim())) {
      this.formErrors['phone'] = 'Số điện thoại phải có 10-11 chữ số';
    }
    if (!this.walkInForm.customerCccd?.trim()) {
      this.formErrors['cccd'] = 'Vui lòng nhập số CCCD';
    } else if (!/^[0-9]{9,12}$/.test(this.walkInForm.customerCccd.trim())) {
      this.formErrors['cccd'] = 'CCCD phải có 9-12 chữ số';
    }
    if (!this.walkInForm.checkInDate)
      this.formErrors['checkIn'] = 'Vui lòng chọn ngày nhận phòng';
    if (!this.walkInForm.checkOutDate) {
      this.formErrors['checkOut'] = 'Vui lòng chọn ngày trả phòng';
    } else if (this.walkInForm.checkInDate && this.walkInForm.checkOutDate <= this.walkInForm.checkInDate) {
      this.formErrors['checkOut'] = 'Ngày trả phòng phải sau ngày nhận phòng';
    }
    return Object.keys(this.formErrors).length === 0;
  }

  onCreateWalkInBooking(): void {
    if (!this.validateForm()) return;
    const payload: WalkInBookingRequestDTO = {
      ...this.walkInForm,
      roomNumbers: this.selectedRoomNumbers,
      roomNumber: this.selectedRoomNumbers[0] || ''
    };
    this.bookingService.createWalkInBooking(payload).subscribe({
      next: (res: any) => {
        alert(res.message || 'Tạo đơn walk-in thành công!');
        this.isWalkInModalOpen = false;
        this.loadBookings();
      },
      error: (err: any) => {
        this.formErrors['submit'] = err.error?.message || err.message || 'Không thể tạo đơn. Vui lòng thử lại.';
      }
    });
  }

  onProcessCheckIn(bookingId: number): void {
    this.bookingService.processCheckIn(bookingId).subscribe({
      next: (res: any) => { alert(res.message); this.loadBookings(); }
    });
  }

  onProcessCancel(bookingId: number): void {
    if (confirm(`Xác nhận thực hiện HỦY đơn đặt phòng #${bookingId} và giải phóng phòng vật lý về kho trống?`)) {
      const staffId = Number(localStorage.getItem('staffId') || 1);
      this.bookingService.cancelBooking(bookingId, staffId).subscribe({
        next: (res: any) => { alert(res.message || 'Đã hủy đơn và mở khóa phòng trống thành công!'); this.loadBookings(); },
        error: (err: any) => alert('Hủy đơn thất bại: ' + (err.error?.message || err.message))
      });
    }
  }

  onProcessCheckOut(bookingId: number): void {
    this.bookingService.processCheckOut(bookingId).subscribe({
      next: (res: any) => { alert(res.message); this.loadBookings(); }
    });
  }

  onViewAuditLog(bookingId: number): void {
    this.auditBookingId = bookingId;
    this.auditLogs = [];
    this.auditLoading = true;
    this.isAuditModalOpen = true;
    this.bookingService.getBookingAuditLogs(bookingId).subscribe({
      next: (logs) => { this.auditLogs = logs; this.auditLoading = false; },
      error: () => { this.auditLoading = false; }
    });
  }

  getActionIcon(action: string): string {
    const icons: Record<string, string> = {
      'CREATE_BOOKING': '📋',
      'CREATE_BATCH_BOOKING': '📋',
      'CONFIRM_PAYMENT': '💰',
      'CANCEL_BOOKING': '🚫',
      'CHECK_IN': '🔑',
      'CHECK_OUT': '🚪',
      'REFUND': '💸',
    };
    return icons[action] ?? '📝';
  }

  onOpenRefundModal(b: BookingResponseDTO): void {
    this.refundBookingId = b.bookingId;
    this.refundBookingAmount = b.finalAmount;
    this.refundReason = '';
    this.refundError = '';
    this.refundOperatorName = localStorage.getItem('username') || 'Nhân viên';
    this.refundTimestamp = new Date();
    this.isRefundModalOpen = true;
  }

  onConfirmRefund(): void {
    if (!this.refundReason.trim()) {
      this.refundError = 'Vui lòng nhập lý do hoàn tiền';
      return;
    }
    if (!this.refundBookingId) return;

    const operatorId = Number(localStorage.getItem('staffId') || 1);
    const operatorName = localStorage.getItem('username') || 'Nhân viên';

    this.bookingService.processRefund(this.refundBookingId, operatorId, operatorName, this.refundReason).subscribe({
      next: (res: any) => {
        alert(res.message || 'Hoàn tiền thành công!');
        this.isRefundModalOpen = false;
        this.loadBookings();
      },
      error: (err: any) => {
        this.refundError = err.error?.message || err.message || 'Hoàn tiền thất bại. Vui lòng thử lại!';
      }
    });
  }
}
