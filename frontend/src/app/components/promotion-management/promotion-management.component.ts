import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { PromotionRequestDTO, PromotionResponseDTO } from '../../models/admin-management.model';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-promotion-management',
  standalone: true,
  imports: [CommonModule, FormsModule], // Bắt buộc giữ cụm imports này để chạy được [(ngModel)] và *ngIf
  templateUrl: './promotion-management.component.html',
  styles: []
})
export class PromotionManagementComponent implements OnInit {
  promotions: PromotionResponseDTO[] = [];
  isPromoModalOpen: boolean = false; // Biến cờ ẩn/hiển thị Popup Form
  
  // Khởi tạo Object mẫu rỗng để lưu trữ tạm thời dữ liệu người dùng gõ vào form
  newPromo: PromotionRequestDTO = { code: '', discountPercentage: 10, startDate: '', endDate: '' };

  private baseApiUrl = `${environment.apiUrl}/bookings/management/promotions`;

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.loadPromotions();
  }

  /**
   * Tải danh sách Khuyến mãi từ MySQL
   */
  loadPromotions(): void {
    this.http.get<PromotionResponseDTO[]>(this.baseApiUrl).subscribe({
      next: (data: PromotionResponseDTO[]) => this.promotions = data,
      error: (err: any) => console.error('Lỗi tải danh mục voucher:', err)
    });
  }

  /**
   * Lưu mã khuyến mãi mới phát hành xuống Cơ sở dữ liệu
   */
  onSavePromotion(): void {
    // 1. Kiểm tra tính hợp lệ dữ liệu đầu vào (Validation)
    if (!this.newPromo.code.trim() || !this.newPromo.discountPercentage || !this.newPromo.startDate || !this.newPromo.endDate) {
      alert('Vui lòng nhập đầy đủ toàn bộ các thông tin bắt buộc (*)!');
      return;
    }

    if (this.newPromo.discountPercentage < 1 || this.newPromo.discountPercentage > 100) {
      alert('Tỷ lệ chiết khấu phòng hợp lệ nằm trong khoảng từ 1% đến 100%!');
      return;
    }

    // 2. Tiến hành đẩy request POST lên cổng API Backend của Spring Boot
    this.http.post(this.baseApiUrl, this.newPromo).subscribe({
      next: (res: any) => { 
        alert(res.message || 'Phát hành chương trình voucher thành công!'); 
        this.isPromoModalOpen = false; // Đóng Form popup mượt mà
        this.loadPromotions(); // Tự động reload lại bảng để hiển thị dòng mới lên top
        this.newPromo = { code: '', discountPercentage: 10, startDate: '', endDate: '' }; // Làm sạch form
      },
      error: (err: any) => alert('Thất bại: ' + (err.error?.message || err.message))
    });
  }
  onTogglePromotionStatus(promoId: number, currentStatus: boolean): void {
    const actionText = currentStatus ? 'dừng áp dụng' : 'kích hoạt lại';
    
    // Hiển thị hộp thoại xác nhận nhanh trước khi gửi yêu cầu về Server
    if (confirm(`Bạn có chắc chắn muốn ${actionText} mã khuyến mãi này không?`)) {
      
      // Gửi request PATCH lên Backend để cập nhật ngược trạng thái isActive
      this.http.patch(`${this.baseApiUrl}/${promoId}/toggle-status`, {}).subscribe({
        next: (res: any) => {
          alert(res.message || 'Cập nhật trạng thái voucher thành công!');
          this.loadPromotions(); // Tải lại bảng dữ liệu mới nhất từ MySQL
        },
        error: (err: any) => alert('Lỗi hệ thống: ' + (err.error?.message || err.message))
      });
    }
  }
}