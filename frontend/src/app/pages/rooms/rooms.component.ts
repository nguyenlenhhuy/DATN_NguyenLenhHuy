import { Component, OnInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; 
import { RouterModule, Router } from '@angular/router'; 
import { RoomService } from '../../services/room.service';
import { FavoriteService } from '../../services/favorite.service';
import { RoomResponseDTO, RoomSearchRequest } from '../../models/room.model';
import { HeaderComponent } from '../../components/header/header.component';

@Component({
  selector: 'app-rooms',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, HeaderComponent],
  templateUrl: './rooms.component.html',
  styleUrl: './rooms.component.scss'
})
export class RoomsComponent implements OnInit {
  rooms: any[] = []; 
  roomTypes: any[] = []; 
  isLoading = false; 
  isTypeDropdownOpen = false; 

  searchRequest: RoomSearchRequest = {
    checkIn: null,
    checkOut: null,
    guestCount: null,
    typeName: ''
  };

  constructor(
    private roomService: RoomService,
    private favoriteService: FavoriteService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadAllRooms();
    this.loadRoomTypes();
  }

  // Tải danh sách phòng từ Backend (đã lọc isDeleted ở Backend)
  loadAllRooms(): void {
    this.isLoading = true;
    this.roomService.getAllRooms().subscribe({
      next: (data: any[]) => {
        this.syncFavorites(data);
        this.isLoading = false;
      },
      error: (err: any) => { 
        console.error('Lỗi khi tải danh sách phòng:', err);
        this.isLoading = false;
      }
    });
  }

  // Tìm kiếm phòng từ Backend (đã lọc isDeleted ở Backend)
  searchRooms(): void {
    this.isLoading = true;
    this.roomService.searchRooms(this.searchRequest).subscribe({
      next: (data: any[]) => {
        this.syncFavorites(data);
        this.isLoading = false;
      },
      error: (err: any) => { 
        console.error('Lỗi khi tìm kiếm phòng:', err);
        this.isLoading = false;
      }
    });
  }

  // Đồng bộ trạng thái yêu thích
  private syncFavorites(data: any[]): void {
    this.favoriteService.getMyFavorites().subscribe({
      next: (favorites: any[]) => {
        const favoriteRoomIds = favorites.map(fav => fav.room ? fav.room.id : null);
        this.rooms = data.map(room => ({
          ...room,
          isFavorite: favoriteRoomIds.includes(room.roomId)
        }));
      },
      error: () => {
        this.rooms = data; 
      }
    });
  }

  loadRoomTypes(): void {
    this.roomService.getRoomTypes().subscribe({
      next: (data) => { this.roomTypes = data; },
      error: (err: any) => { console.error('Lỗi khi tải loại phòng:', err); }
    });
  }

  selectType(typeName: string): void {
    this.searchRequest.typeName = typeName;
    this.isTypeDropdownOpen = false; 
  }

  resetSearch(): void {
    this.searchRequest = { checkIn: null, checkOut: null, guestCount: null, typeName: '' };
    this.loadAllRooms();
  }

  formatPrice(price: number | undefined): string {
    if (price === undefined || price === null) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  }

  toggleFavorite(event: Event, room: any): void {
    event.stopPropagation();
    this.favoriteService.toggleFavorite(room.roomId).subscribe({
      next: () => { room.isFavorite = !room.isFavorite; },
      error: () => alert("Có lỗi xảy ra!")
    });
  }

  getRoomImage(room: any): string {
    return (room.imageUrl && room.imageUrl.trim() !== '') 
      ? room.imageUrl 
      : 'https://images.unsplash.com/photo-1582719478250-c89404bb8a0e?q=80&w=1000';
  }

  getStatusLabel(status: string | null): string {
    switch(status) {
      case 'AVAILABLE': return 'Còn trống';
      case 'OCCUPIED': return 'Đã đặt';
      case 'CLEANING': return 'Đang dọn';
      case 'DIRTY': return 'Chưa dọn';
      case 'MAINTENANCE': return 'Bảo trì';
      default: return 'Không xác định';
    }
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('#customTypeSelect')) {
      this.isTypeDropdownOpen = false;
    }
  }

  viewRoomDetail(roomId: number, event: Event): void {
    event.stopPropagation();
    this.router.navigate(['/rooms', roomId]);
  }
}