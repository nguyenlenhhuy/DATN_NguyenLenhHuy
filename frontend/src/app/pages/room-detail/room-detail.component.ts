import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule, Router } from '@angular/router';
import { RoomService } from '../../services/room.service';
import { RoomResponseDTO } from '../../models/room.model';
import { HeaderComponent } from '../../components/header/header.component';

@Component({
  selector: 'app-room-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, HeaderComponent],
  templateUrl: './room-detail.component.html',
  styleUrl: './room-detail.component.scss'
})
export class RoomDetailComponent implements OnInit {
  room!: RoomResponseDTO;
  isLoading = true;
  selectedImage: string = ''; 
  currentIndex: number = 0; // Thêm biến để quản lý chỉ số ảnh hiện tại

  constructor(
    private route: ActivatedRoute,
    private roomService: RoomService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const roomIdStr = this.route.snapshot.paramMap.get('id');
    if (roomIdStr) {
      const roomId = Number(roomIdStr);
      this.loadRoomDetail(roomId);
    } else {
      this.router.navigate(['/rooms']);
    }
  }

  loadRoomDetail(id: number): void {
    this.isLoading = true;
    this.roomService.getRoomById(id).subscribe({
      next: (data: RoomResponseDTO) => {
        this.room = data;
        
        // Sử dụng albumImages (từ DTO mới) để hiển thị danh sách ảnh
        if ((this.room as any).albumImages && (this.room as any).albumImages.length > 0) {
          this.selectedImage = (this.room as any).albumImages[0];
          this.currentIndex = 0;
        } else {
          this.selectedImage = this.room.imageUrl || 'https://images.unsplash.com/photo-1582719478250-c89404bb8a0e?q=80&w=1000&auto=format&fit=crop';
        }
        
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Lỗi không thể tải chi tiết phòng:', err);
        this.isLoading = false;
      }
    });
  }

  // Cập nhật hàm này để nhận thêm index
  changePreviewImage(url: string, index: number): void {
    this.selectedImage = url;
    this.currentIndex = index;
  }

  // Logic mũi tên Next
  nextImage(): void {
    const album = (this.room as any).albumImages;
    if (album && this.currentIndex < album.length - 1) {
      this.currentIndex++;
      this.selectedImage = album[this.currentIndex];
    }
  }

  // Logic mũi tên Previous
  prevImage(): void {
    const album = (this.room as any).albumImages;
    if (album && this.currentIndex > 0) {
      this.currentIndex--;
      this.selectedImage = album[this.currentIndex];
    }
  }

  formatPrice(price: number | undefined): string {
    if (price === undefined || price === null) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  }

  goBack(): void {
    this.router.navigate(['/rooms']);
  }
  // Các biến mới
checkInDate: string = '2026-08-20'; 
checkOutDate: string = '2026-08-21';
numberOfNights: number = 1;
couponCode: string = '';
discount: number = 0;
totalPrice: number = 0;

// Hàm tính toán logic
calculateTotal(): void {
  const start = new Date(this.checkInDate);
  const end = new Date(this.checkOutDate);
  const diffDays = Math.max(1, Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)));
  
  this.numberOfNights = diffDays;
  this.updateFinalPrice();
}

applyCoupon(): void {
  // Logic giả định: mã "GIAM100" giảm 100k
  if (this.couponCode === 'GIAM100') {
    this.discount = 100000;
  } else {
    this.discount = 0;
    alert('Mã không hợp lệ!');
  }
  this.updateFinalPrice();
}

updateFinalPrice(): void {
  this.totalPrice = (this.room.price * this.numberOfNights) - this.discount;
}
}