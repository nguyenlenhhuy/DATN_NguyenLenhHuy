import { Component, OnInit, OnDestroy, DestroyRef, inject, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, FormsModule, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { register } from 'swiper/element/bundle';
import { HttpClient } from '@angular/common/http';
import { HeaderComponent } from '../../components/header/header.component';
import { RoomService } from '../../services/room.service';
import { FavoriteService } from '../../services/favorite.service';
import { RoomResponseDTO } from '../../models/room.model';
import { AuthService } from '../../services/auth.service';
import { environment } from '../../../environments/environment';

register();

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    RouterModule,
    HeaderComponent
  ],
  templateUrl: './home.component.html',
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class HomeComponent implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private roomService = inject(RoomService);
  private favoriteService = inject(FavoriteService);
  private destroyRef = inject(DestroyRef);
  private http = inject(HttpClient);
  public authService = inject(AuthService);

  searchForm!: FormGroup;
  featuredRooms: any[] = [];
  roomTypes: any[] = [];
  promotions: any[] = [];

  // Price filter
  minPrice: number = 0;
  maxPrice: number = 10000000;
  maxSliderLimit: number = 10000000;
  selectedPricePreset: string = '0-10000000';

  // Countdown
  countdownHours = '00';
  countdownMinutes = '00';
  countdownSeconds = '00';
  private countdownTarget: Date = new Date();
  private countdownInterval: any;

  // Copy coupon feedback
  copiedCode: string | null = null;

  ngOnInit(): void {
    this.initSearchForm();
    this.loadFeaturedRooms();
    this.loadRoomTypes();
    this.loadPromotions();
  }

  ngOnDestroy(): void {
    if (this.countdownInterval) clearInterval(this.countdownInterval);
  }

  // ── Countdown ──────────────────────────────────────────────────────────────

  private startCountdown(targetDate?: Date): void {
    if (this.countdownInterval) clearInterval(this.countdownInterval);

    if (targetDate) {
      this.countdownTarget = targetDate;
    } else {
      const t = new Date();
      t.setDate(t.getDate() + 3);
      t.setHours(23, 59, 59, 0);
      this.countdownTarget = t;
    }

    const tick = () => {
      const distance = this.countdownTarget.getTime() - Date.now();
      if (distance <= 0) {
        this.countdownHours = '00';
        this.countdownMinutes = '00';
        this.countdownSeconds = '00';
        clearInterval(this.countdownInterval);
        return;
      }
      this.countdownHours   = String(Math.floor((distance % 86400000) / 3600000)).padStart(2, '0');
      this.countdownMinutes = String(Math.floor((distance % 3600000)  / 60000)).padStart(2, '0');
      this.countdownSeconds = String(Math.floor((distance % 60000)    / 1000)).padStart(2, '0');
    };

    tick();
    this.countdownInterval = setInterval(tick, 1000);
  }

  // ── Promotions ─────────────────────────────────────────────────────────────

  loadPromotions(): void {
    this.http.get<any[]>(`${environment.apiUrl}/bookings/management/promotions/active`).subscribe({
      next: (promos) => {
        this.promotions = promos.slice(0, 4);
        const nearest = promos
          .filter(p => p.endDate)
          .sort((a, b) => new Date(a.endDate).getTime() - new Date(b.endDate).getTime())[0];
        this.startCountdown(nearest ? new Date(nearest.endDate) : undefined);
      },
      error: () => {
        this.promotions = [
          { code: 'SUMMER26', discountPercentage: 20, endDate: null },
          { code: 'WELCOME10', discountPercentage: 10, endDate: null }
        ];
        this.startCountdown();
      }
    });
  }

  getPromoTitle(promo: any): string {
    return `Giảm ${promo.discountPercentage}% cho đặt phòng trực tuyến`;
  }

  copyCode(code: string): void {
    navigator.clipboard.writeText(code).then(() => {
      this.copiedCode = code;
      setTimeout(() => { this.copiedCode = null; }, 2000);
    });
  }

  // ── Search form ────────────────────────────────────────────────────────────

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      roomType: [''],
      checkIn: [''],
      checkOut: [''],
      guests: ['1']
    }, { validators: this.dateValidator });
  }

  dateValidator(control: AbstractControl): ValidationErrors | null {
    const checkIn  = control.get('checkIn')?.value;
    const checkOut = control.get('checkOut')?.value;
    if (checkIn && checkOut && new Date(checkOut) <= new Date(checkIn)) {
      return { dateInvalid: true };
    }
    return null;
  }

  onPresetChange(): void {
    if (this.selectedPricePreset === 'custom') return;
    const [min, max] = this.selectedPricePreset.split('-').map(Number);
    this.minPrice = min;
    this.maxPrice = max;
  }

  onManualPriceChange(): void {
    if (this.minPrice > this.maxPrice) [this.minPrice, this.maxPrice] = [this.maxPrice, this.minPrice];
    this.selectedPricePreset = 'custom';
  }

  onSearch(): void {
    if (this.searchForm.valid) {
      const queryParams: any = {
        typeName:   this.searchForm.value.roomType,
        checkIn:    this.searchForm.value.checkIn,
        checkOut:   this.searchForm.value.checkOut,
        guestCount: this.searchForm.value.guests
      };
      if (this.minPrice > 0)                     queryParams.minPrice = this.minPrice;
      if (this.maxPrice < this.maxSliderLimit)    queryParams.maxPrice = this.maxPrice;
      Object.keys(queryParams).forEach(k => { if (!queryParams[k]) delete queryParams[k]; });
      this.router.navigate(['/rooms'], { queryParams });
    } else {
      alert('Vui lòng kiểm tra lại Ngày nhận phòng và Ngày trả phòng!');
      this.searchForm.markAllAsTouched();
    }
  }

  // ── Room data ──────────────────────────────────────────────────────────────

  loadRoomTypes(): void {
    this.roomService.getRoomTypes()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (types) => this.roomTypes = types,
        error: () => this.roomTypes = []
      });
  }

  loadFeaturedRooms(): void {
    this.roomService.getFeaturedRooms()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (rooms: RoomResponseDTO[]) => {
          const sanitized = rooms.map(room => {
            let price = 0;
            if (room.price && typeof room.price === 'object') {
              price = Number((room.price as any).amount || 0);
            } else {
              price = Number(room.price || 0);
            }
            return { ...room, price, rating: room.rating ?? 4.5 };
          }).sort((a, b) => {
            if (b.rating !== a.rating) return b.rating - a.rating;
            if (a.price !== b.price)   return a.price - b.price;
            return a.roomId - b.roomId;
          });
          this.syncFavorites(sanitized);
        },
        error: (err) => console.error('Lỗi tải phòng nổi bật:', err)
      });
  }

  private syncFavorites(data: any[]): void {
    if (!this.authService.isLoggedIn()) {
      this.featuredRooms = data.map(r => ({ ...r, isFavorite: false }));
      return;
    }
    this.favoriteService.getMyFavorites()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (favs: any[]) => {
          const ids = favs.map(f => f.room?.id ?? null);
          this.featuredRooms = data.map(r => ({ ...r, isFavorite: ids.includes(r.roomId) }));
        },
        error: () => { this.featuredRooms = data; }
      });
  }

  toggleFavorite(event: Event, room: any): void {
    event.stopPropagation();
    this.favoriteService.toggleFavorite(room.roomId).subscribe({
      next: () => { room.isFavorite = !room.isFavorite; },
      error: () => alert('Có lỗi xảy ra khi xử lý danh sách yêu thích!')
    });
  }

  getRoomImage(room: any): string {
    return (room.imageUrl && room.imageUrl.trim())
      ? room.imageUrl
      : 'https://images.unsplash.com/photo-1582719478250-c89404bb8a0e?q=80&w=1000';
  }

  viewRoomDetail(roomId: number, event: Event): void {
    event.stopPropagation();
    this.router.navigate(['/rooms', roomId]);
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  }
}
