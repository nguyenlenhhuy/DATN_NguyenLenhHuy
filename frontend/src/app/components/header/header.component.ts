import { Component, OnInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './header.component.html'
})
export class HeaderComponent implements OnInit {
  isDropdownOpen = false;

  constructor(public authService: AuthService, private router: Router) {}

  ngOnInit(): void {}

  closeDropdown(): void {
    this.isDropdownOpen = false;
  }

  toggleDropdown(event?: Event): void {
    if (event) {
      event.stopPropagation(); 
    }
    this.isDropdownOpen = !this.isDropdownOpen;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    const target = event.target as HTMLElement;
    const dropdownElement = document.getElementById('userDropdownBlock');
    
    if (dropdownElement && !dropdownElement.contains(target)) {
      this.isDropdownOpen = false;
    }
  }

  getUserName(): string {
    return localStorage.getItem('username') || 'Người dùng';
  }

  getUserInitials(): string {
    return this.getUserName().charAt(0).toUpperCase();
  }

  logout(): void {
    this.isDropdownOpen = false;
    this.authService.logout();
    this.router.navigate(['/login']); 
  }

  onRoomsClick(): void {
    console.log("Đã click vào menu Phòng");
    this.router.navigate(['/rooms']);
  }

  /**
   * 🔥 HÀM MỚI ĐƯỢC THÊM:
   * Xử lý đóng Dropdown và ép lệnh chuyển trang sang trang Lịch sử đặt phòng
   */
  onHistoryClick(): void {
    this.isDropdownOpen = false; // Đóng menu thả xuống ngay lập tức
    console.log("Đang điều hướng sang trang Lịch sử đặt phòng...");
    this.router.navigate(['/history']); // Kích hoạt bộ định tuyến chuyển sang màn hình lịch sử
  }
}