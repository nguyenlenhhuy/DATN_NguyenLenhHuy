import { Component, OnInit } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterModule], // Sử dụng RouterModule để hỗ trợ routerLink và router-outlet
  templateUrl: './admin-layout.component.html',
  styles: [`
    /* Hiệu ứng fade-in mượt mà khi các Component con render vào router-outlet */
    .fade-in {
      animation: fadeIn 0.25s ease-out forwards;
    }
    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(4px); }
      to { opacity: 1; transform: translateY(0); }
    }
  `]
})
export class AdminLayoutComponent implements OnInit {
  currentUsername: string = 'Quản trị viên';
  userRole: string = 'STAFF'; // Biến lưu trữ Role để đồng bộ ẩn/hiện menu bên HTML

  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit(): void {
    // Lấy thông tin Username từ localStorage để hiển thị trên Topbar
    this.currentUsername = localStorage.getItem('username') || 'Admin';

    // Lấy quyền tài khoản (ADMIN/STAFF) từ hệ thống để phân rã Sidebar
    this.userRole = localStorage.getItem('role') || 'STAFF';
  }

  logout(): void {
    if (confirm('Bạn có chắc chắn muốn đăng xuất khỏi hệ thống quản trị?')) {
      // 1. Gọi service để xóa sạch token/session lưu trữ
      this.authService.logout(); 
      
      // 2. Điều hướng an toàn về trang chủ của khách hàng
      this.router.navigate(['/']); 
    }
  }
}