import { Component, OnInit } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './admin-layout.component.html',
})
export class AdminLayoutComponent implements OnInit {
  currentUsername: string = 'Quản trị viên';
  userRole: string = 'STAFF'; // ĐÃ THÊM: Biến lưu trữ Role để đồng bộ ẩn/hiện menu bên HTML

  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit(): void {
    // Lấy thông tin Username từ localStorage để hiển thị trên Topbar
    this.currentUsername = localStorage.getItem('username') || 'Admin';

    // ĐÃ THÊM: Lấy quyền tài khoản (ADMIN/STAFF) từ hệ thống để phân rã Sidebar
    this.userRole = localStorage.getItem('role') || 'STAFF';
  }

  logout(): void {
    // 1. Gọi service để xóa sạch token/session lưu trữ
    this.authService.logout(); 
    
    // 2. Điều hướng an toàn về trang chủ của khách hàng
    this.router.navigate(['/']); 
  }
}