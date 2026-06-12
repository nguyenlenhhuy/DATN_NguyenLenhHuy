import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router'; 
import { RoomService } from '../../services/room.service';
import { FavoriteService } from '../../services/favorite.service';
import { HeaderComponent } from '../../components/header/header.component';

@Component({
  selector: 'app-favorites',
  standalone: true,
  imports: [CommonModule, RouterModule, HeaderComponent],
  templateUrl: './favorites.component.html',
  styleUrl: './favorites.component.scss' 
})
export class FavoritesComponent implements OnInit {
  favoriteRooms: any[] = []; 
  isLoading = false;

  constructor(
    private roomService: RoomService,
    private favoriteService: FavoriteService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadFavorites();
  }

  // Tải danh sách phòng yêu thích
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
        console.error("Lỗi tải danh sách yêu thích:", err);
        this.isLoading = false;
      }
    });
  }

  // --- ĐÃ CẬP NHẬT LẠI CHO KHỚP VỚI BACKEND ---
  getPromotionCode(room: any): string | null {
    // API trả về một object 'appliedPromotion' (không có 's' và không phải mảng)
    if (room && room.appliedPromotion && room.appliedPromotion.code) {
      return room.appliedPromotion.code;
    }
    return null;
  }

  onRemoveFavorite(room: any, event: Event): void {
    event.stopPropagation(); 
    this.favoriteService.toggleFavorite(room.roomId).subscribe({
      next: () => {
        this.favoriteRooms = this.favoriteRooms.filter(r => r.roomId !== room.roomId);
      },
      error: (err: any) => console.error("Lỗi khi bỏ yêu thích phòng:", err)
    });
  }

  formatPrice(price: number | undefined): string {
    if (price === undefined || price === null || price === 0) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  }

  getRoomImage(room: any): string {
    return (room && room.imageUrl && room.imageUrl.trim() !== '') 
      ? room.imageUrl 
      : 'https://images.unsplash.com/photo-1582719478250-c89404bb8a0e?q=80&w=1000&auto=format&fit=crop';
  }

  onImageError(event: Event): void {
    const target = event.target as HTMLImageElement;
    target.src = 'https://images.unsplash.com/photo-1582719478250-c89404bb8a0e?q=80&w=1000&auto=format&fit=crop';
  }

  viewRoomDetail(roomId: number, event?: Event): void {
    if (event) {
      event.stopPropagation();
    }
    this.router.navigate(['/rooms', roomId]);
  }

  bookNow(event: Event, room: any): void {
    event.stopPropagation();
    this.router.navigate(['/rooms', room.roomId]);
  }
}