import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { RoomService } from '../../services/room.service';
import { FavoriteService } from '../../services/favorite.service';
import { RoomResponseDTO } from '../../models/room.model';
import { HeaderComponent } from '../../components/header/header.component';

@Component({
  selector: 'app-favorites',
  standalone: true,
  imports: [CommonModule, RouterModule, HeaderComponent],
  templateUrl: './favorites.component.html',
  styleUrl: './favorites.component.scss' // Liên kết với file style scss
})
export class FavoritesComponent implements OnInit {
  favoriteRooms: RoomResponseDTO[] = [];
  isLoading = false;

  constructor(
    private roomService: RoomService,
    private favoriteService: FavoriteService
  ) {}

  ngOnInit(): void {
    this.loadFavorites();
  }

  // Tải danh sách phòng khách sạn đã được User thả tim lưu lại
  loadFavorites(): void {
    this.isLoading = true;
    this.roomService.getFavoriteRooms().subscribe({
      next: (data: RoomResponseDTO[]) => {
        this.favoriteRooms = data;
        this.isLoading = false;
      },
      error: (err: any) => {
        console.error("Lỗi tải danh sách yêu thích:", err);
        this.isLoading = false;
      }
    });
  }

  // Xử lý sự kiện click bỏ thích: Xóa phòng khỏi mảng hiển thị ngay lập tức để tối ưu UX
  onRemoveFavorite(room: RoomResponseDTO, event: Event): void {
    event.stopPropagation(); // Ngăn chặn hành vi nổi bọt sự kiện click vào thẻ
    
    this.favoriteService.toggleFavorite(room.roomId).subscribe({
      next: () => {
        // Khử trực quan: Lọc bỏ ngay phòng vừa bấm ra khỏi danh sách hiển thị trên giao diện
        this.favoriteRooms = this.favoriteRooms.filter(r => r.roomId !== room.roomId);
      },
      error: (err: any) => {
        console.error("Lỗi khi bỏ yêu thích phòng:", err);
      }
    });
  }

  // Định dạng hiển thị tiền tệ VNĐ có dấu chấm phân cách (Ví dụ: 1.500.000đ)
  formatPrice(price: number): string {
    if (!price) return '0đ';
    return price.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ".") + 'đ';
  }
}