import { Component, OnInit, DestroyRef, inject, CUSTOM_ELEMENTS_SCHEMA, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, FormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterModule } from '@angular/router'; 
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { register } from 'swiper/element/bundle';
import { HeaderComponent } from '../../components/header/header.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { RoomService } from '../../services/room.service';
import { FavoriteService } from '../../services/favorite.service';
import { RoomResponseDTO } from '../../models/room.model';
import { AuthService } from '../../services/auth.service';

// Đăng ký các thẻ tùy chỉnh Web Components cho SwiperJS
register();

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule, 
    ReactiveFormsModule, 
    FormsModule, 
    RouterModule, 
    HeaderComponent, 
    FooterComponent
  ],
  templateUrl: './home.component.html',
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class HomeComponent implements OnInit {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private roomService = inject(RoomService);
  private favoriteService = inject(FavoriteService);
  private destroyRef = inject(DestroyRef); 
  public authService = inject(AuthService);
  searchForm!: FormGroup;
  featuredRooms: any[] = []; 
  roomTypes: any[] = [];

  // === BIẾN QUẢN LÝ KHOẢNG GIÁ ===
  minPrice: number = 0;
  maxPrice: number = 10000000;
  maxSliderLimit: number = 10000000;
  selectedPricePreset: string = '0-10000000'; 

  promotions = [
    { title: 'Giảm 20% mùa hè', code: 'SUMMER20' },
    { title: 'Combo Gia đình', code: 'FAMILYFUN' }
  ];

  ngOnInit(): void {
    this.initSearchForm();
    this.loadFeaturedRooms();
    this.loadRoomTypes();
  }

  // ĐÃ FIX: Bỏ Validators.required để người dùng có thể tìm kiếm mà không cần nhập ngày
  initSearchForm(): void {
    this.searchForm = this.fb.group({
      roomType: [''],
      checkIn: [''], 
      checkOut: [''],
      guests: ['1']
    }, { validators: this.dateValidator });
  }

  dateValidator(control: AbstractControl): ValidationErrors | null {
    const checkIn = control.get('checkIn')?.value;
    const checkOut = control.get('checkOut')?.value;
    // Chỉ báo lỗi nếu có nhập cả 2 ngày nhưng ngày trả < ngày nhận
    if (checkIn && checkOut && new Date(checkOut) <= new Date(checkIn)) {
      return { dateInvalid: true };
    }
    return null;
  }

  onPresetChange(): void {
    if (this.selectedPricePreset === 'custom') return;
    const parts = this.selectedPricePreset.split('-');
    if (parts.length === 2) {
      this.minPrice = Number(parts[0]);
      this.maxPrice = Number(parts[1]);
    }
  }

  onManualPriceChange(): void {
    if (this.minPrice > this.maxPrice) {
      const temp = this.minPrice;
      this.minPrice = this.maxPrice;
      this.maxPrice = temp;
    }
    this.selectedPricePreset = 'custom'; 
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  }

  loadRoomTypes(): void {
    this.roomService.getRoomTypes()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (types) => this.roomTypes = types,
        error: () => this.roomTypes = [{ typeName: 'Deluxe' }, { typeName: 'Suite' }] 
      });
  }

  loadFeaturedRooms(): void {
    this.roomService.getFeaturedRooms()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (rooms: RoomResponseDTO[]) => {
          const sanitizedRooms = rooms.map(room => {
            let finalPrice = 0;
            if (room.price && typeof room.price === 'object') {
              finalPrice = Number((room.price as any).amount || 0);
            } else {
              finalPrice = Number(room.price || 0);
            }
            return {
              ...room,
              price: finalPrice,
              rating: room.rating ?? (Math.floor(Math.random() * 2) + 4)
            };
          });

          const sortedRooms = sanitizedRooms.sort((a, b) => {
            if (b.rating !== a.rating) return b.rating - a.rating;
            if (a.price !== b.price) return a.price - b.price;
            return a.roomId - b.roomId;
          });

          this.syncFavorites(sortedRooms);
        },
        error: (err) => console.error('Lỗi khi tải danh sách phòng nổi bật:', err)
      });
  }

 private syncFavorites(sortedData: any[]): void {
    // 🔥 CHỐT CHẶN: Kiểm tra khách vãng lai
    if (!this.authService.isLoggedIn()) {
      // Trả về danh sách phòng nổi bật với trạng thái chưa yêu thích
      this.featuredRooms = sortedData.map(room => ({
        ...room,
        isFavorite: false
      }));
      return; // Cắt luồng ngay lập tức, chặn API gọi xuống server
    }

    // LUỒNG CŨ: Chỉ chạy khi đã có Token đăng nhập
    this.favoriteService.getMyFavorites()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (favorites: any[]) => {
          const favoriteRoomIds = favorites.map(fav => fav.room ? fav.room.id : null);
          this.featuredRooms = sortedData.map(room => ({
            ...room,
            isFavorite: favoriteRoomIds.includes(room.roomId)
          }));
        },
        error: () => {
          this.featuredRooms = sortedData; 
        }
      });
  }

  toggleFavorite(event: Event, room: any): void {
    event.stopPropagation(); 
    this.favoriteService.toggleFavorite(room.roomId).subscribe({
      next: () => { room.isFavorite = !room.isFavorite; },
      error: () => alert("Có lỗi xảy ra khi xử lý danh sách yêu thích!")
    });
  }

  getRoomImage(room: any): string {
    return (room.imageUrl && room.imageUrl.trim() !== '') 
      ? room.imageUrl 
      : 'https://images.unsplash.com/photo-1582719478250-c89404bb8a0e?q=80&w=1000';
  }

  viewRoomDetail(roomId: number, event: Event): void {
    event.stopPropagation();
    this.router.navigate(['/rooms', roomId]);
  }

  // === HÀM THỰC THI TÌM KIẾM CHÍNH ĐÃ FIX LỖI ===
  onSearch(): void {
    if (this.searchForm.valid) {
      const queryParams: any = {
        typeName: this.searchForm.value.roomType, 
        checkIn: this.searchForm.value.checkIn,
        checkOut: this.searchForm.value.checkOut,
        guestCount: this.searchForm.value.guests
      };

      if (this.minPrice > 0) {
        queryParams.minPrice = this.minPrice;
      }
      if (this.maxPrice < this.maxSliderLimit) {
        queryParams.maxPrice = this.maxPrice;
      }

      Object.keys(queryParams).forEach(key => {
        if (!queryParams[key]) delete queryParams[key];
      });

      // Điều hướng ngay lập tức
      this.router.navigate(['/rooms'], { queryParams: queryParams });
    } else {
      alert("Vui lòng kiểm tra lại Ngày nhận phòng và Ngày trả phòng!");
      this.searchForm.markAllAsTouched(); 
    }
  }
}