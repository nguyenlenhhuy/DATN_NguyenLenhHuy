import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { RoomService } from '../../services/room.service';
import { FavoriteService } from '../../services/favorite.service';
import { BookingService, BatchBookingRequestDTO } from '../../services/booking.service';
import { HeaderComponent } from '../../components/header/header.component';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-favorites',
  standalone: true,
  imports: [CommonModule, RouterModule, HeaderComponent, FormsModule],
  templateUrl: './favorites.component.html',
  styleUrl: './favorites.component.scss'
})
export class FavoritesComponent implements OnInit {
  favoriteRooms: any[] = [];
  isLoading = false;

  // Chế độ chọn nhiều phòng
  isSelectMode = false;
  selectedRoomIds = new Set<number>();

  // Modal đặt nhiều phòng
  isBatchModalOpen = false;
  batchCheckIn: string = '';
  batchCheckOut: string = '';
  batchCouponCode: string = '';
  batchDiscount: number = 0;
  batchTotalOriginal: number = 0;
  batchFinalAmount: number = 0;
  isBatchBooking = false;
  isValidatingCoupon = false;

  constructor(
    private roomService: RoomService,
    private favoriteService: FavoriteService,
    private bookingService: BookingService,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.loadFavorites();
    const today = new Date();
    const tomorrow = new Date();
    tomorrow.setDate(today.getDate() + 1);
    this.batchCheckIn = this.formatDate(today);
    this.batchCheckOut = this.formatDate(tomorrow);
  }

  loadFavorites(): void {
    this.isLoading = true;
    this.roomService.getFavoriteRooms().subscribe({
      next: (data: any[]) => {
        if (data && data.length > 0 && data[0].room) {
          this.favoriteRooms = data.map(fav => fav.room);
        } else {
          this.favoriteRooms = data || [];
        }
        this.isLoading = false;
      },
      error: (err: any) => {
        console.error('Lỗi tải danh sách yêu thích:', err);
        this.isLoading = false;
      }
    });
  }

  // ── Chế độ chọn nhiều phòng ────────────────────────────────────────────────

  toggleSelectMode(): void {
    this.isSelectMode = !this.isSelectMode;
    if (!this.isSelectMode) {
      this.selectedRoomIds.clear();
    }
  }

  toggleRoomSelection(roomId: number, event: Event): void {
    event.stopPropagation();
    if (this.selectedRoomIds.has(roomId)) {
      this.selectedRoomIds.delete(roomId);
    } else {
      this.selectedRoomIds.add(roomId);
    }
  }

  isSelected(roomId: number): boolean {
    return this.selectedRoomIds.has(roomId);
  }

  get selectedCount(): number {
    return this.selectedRoomIds.size;
  }

  get selectedRooms(): any[] {
    return this.favoriteRooms.filter(r => this.selectedRoomIds.has(r.roomId));
  }

  get unavailableSelectedCount(): number {
    return this.selectedRooms.filter(r => r.status !== 'AVAILABLE').length;
  }

  selectAllAvailable(): void {
    this.favoriteRooms
      .filter(r => r.status === 'AVAILABLE')
      .forEach(r => this.selectedRoomIds.add(r.roomId));
  }

  clearSelection(): void {
    this.selectedRoomIds.clear();
  }

  // ── Modal đặt nhiều phòng ──────────────────────────────────────────────────

  openBatchModal(): void {
    const unavailable = this.unavailableSelectedCount;
    if (unavailable > 0) {
      alert(`${unavailable} phòng đã chọn đang không khả dụng. Vui lòng bỏ chọn các phòng đó trước.`);
      return;
    }
    if (this.selectedCount === 0) {
      alert('Vui lòng chọn ít nhất 1 phòng!');
      return;
    }
    this.batchDiscount = 0;
    this.batchCouponCode = '';
    this.calculateBatchTotal();
    this.isBatchModalOpen = true;
  }

  closeBatchModal(): void {
    this.isBatchModalOpen = false;
  }

  calculateBatchTotal(): void {
    if (!this.batchCheckIn || !this.batchCheckOut) return;
    const start = new Date(this.batchCheckIn);
    const end = new Date(this.batchCheckOut);
    const nights = Math.max(1, Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)));

    this.batchTotalOriginal = this.selectedRooms.reduce((sum, room) => {
      const effectivePrice = room.appliedPromotion
        ? room.price * (1 - room.appliedPromotion.discountPercentage / 100)
        : room.price;
      return sum + (effectivePrice * nights);
    }, 0);

    this.batchFinalAmount = this.batchTotalOriginal - this.batchDiscount;
  }

  onBatchDateChange(): void {
    this.batchDiscount = 0;
    this.calculateBatchTotal();
  }

  applyBatchCoupon(): void {
    const code = this.batchCouponCode.trim().toUpperCase();
    if (!code) {
      alert('Vui lòng nhập mã giảm giá!');
      return;
    }
    this.isValidatingCoupon = true;
    const params = new HttpParams()
      .set('code', code)
      .set('amount', this.batchTotalOriginal.toString());

    this.http.get<any>(`${environment.apiUrl}/bookings/management/promotions/validate`, { params }).subscribe({
      next: (res) => {
        this.batchDiscount = res.discountAmount;
        this.batchFinalAmount = this.batchTotalOriginal - this.batchDiscount;
        this.isValidatingCoupon = false;
        alert(`Áp dụng thành công! Giảm ${this.formatPrice(this.batchDiscount)}`);
      },
      error: (err) => {
        this.batchDiscount = 0;
        this.batchFinalAmount = this.batchTotalOriginal;
        this.isValidatingCoupon = false;
        alert(err.error?.message || 'Mã giảm giá không hợp lệ!');
      }
    });
  }

  confirmBatchBooking(): void {
    if (!this.batchCheckIn || !this.batchCheckOut) {
      alert('Vui lòng chọn ngày nhận và trả phòng!');
      return;
    }
    if (new Date(this.batchCheckOut) <= new Date(this.batchCheckIn)) {
      alert('Ngày trả phòng phải sau ngày nhận!');
      return;
    }

    this.isBatchBooking = true;

    const payload: BatchBookingRequestDTO = {
      roomIds: Array.from(this.selectedRoomIds),
      checkIn: this.batchCheckIn,
      checkOut: this.batchCheckOut,
      paymentMethod: 'PAYOS',
      couponCode: this.batchDiscount > 0 ? this.batchCouponCode.trim().toUpperCase() : undefined
    };

    this.bookingService.createBatchBooking(payload).subscribe({
      next: (res) => {
        if (res.checkoutUrl) {
          window.location.href = res.checkoutUrl;
        } else {
          alert(`Đặt ${payload.roomIds.length} phòng thành công!`);
          this.router.navigate(['/history']);
        }
      },
      error: (err) => {
        this.isBatchBooking = false;
        alert('Lỗi đặt phòng: ' + (err.error?.message || 'Có lỗi xảy ra.'));
      }
    });
  }

  // ── Hành động thẻ phòng đơn lẻ ────────────────────────────────────────────

  getPromotionCode(room: any): string | null {
    if (room?.appliedPromotion?.code) return room.appliedPromotion.code;
    return null;
  }

  onRemoveFavorite(room: any, event: Event): void {
    event.stopPropagation();
    this.selectedRoomIds.delete(room.roomId);
    this.favoriteService.toggleFavorite(room.roomId).subscribe({
      next: () => {
        this.favoriteRooms = this.favoriteRooms.filter(r => r.roomId !== room.roomId);
      },
      error: (err: any) => console.error('Lỗi khi bỏ yêu thích phòng:', err)
    });
  }

  viewRoomDetail(roomId: number, event?: Event): void {
    if (event) event.stopPropagation();
    this.router.navigate(['/rooms', roomId]);
  }

  bookNow(event: Event, room: any): void {
    event.stopPropagation();
    this.router.navigate(['/rooms', room.roomId]);
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  formatPrice(price: number | undefined): string {
    if (!price && price !== 0) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  }

  getRoomImage(room: any): string {
    return room?.imageUrl?.trim()
      ? room.imageUrl
      : 'https://images.unsplash.com/photo-1582719478250-c89404bb8a0e?q=80&w=1000&auto=format&fit=crop';
  }

  onImageError(event: Event): void {
    (event.target as HTMLImageElement).src =
      'https://images.unsplash.com/photo-1582719478250-c89404bb8a0e?q=80&w=1000&auto=format&fit=crop';
  }

  private formatDate(date: Date): string {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }
}
