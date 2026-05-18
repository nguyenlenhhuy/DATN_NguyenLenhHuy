import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router'; 
import { AuthService } from '../../services/auth.service';
import { LoginRequest } from '../../models/auth.model';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink], 
  templateUrl: './login.component.html'
})
export class LoginComponent {
  // Đối tượng chứa dữ liệu form
  loginData: LoginRequest = { 
    username: '', 
    password: '' 
  };

  // Biến để quản lý trạng thái UI
  errorMessage: string = '';
  isLoading: boolean = false; 

  constructor(
    private authService: AuthService, 
    private router: Router
  ) {}

  /**
   * Xử lý đăng nhập
   */
  onLogin(): void {
    // Reset trạng thái trước khi gửi yêu cầu
    this.errorMessage = '';
    this.isLoading = true;
    
    // Gọi service xử lý đăng nhập
    this.authService.login(this.loginData).subscribe({
      next: (res) => {
        const token = res.accessToken; 
        const role = (res.role || '').toUpperCase(); 

        if (token) {
          // Lưu thông tin vào LocalStorage
          localStorage.setItem('token', token);
          localStorage.setItem('role', role);
          localStorage.setItem('username', res.username);
          
          // Cập nhật trạng thái đăng nhập cho toàn ứng dụng
          this.authService.setLoginStatus(true);

          // Điều hướng dựa trên vai trò (Role-based Routing)
          if (role === 'ADMIN' || role === 'STAFF') {
            this.router.navigate(['/admin/dashboard']);
          } else {
            this.router.navigate(['/home']);
          }
        }
        this.isLoading = false;
      },
      error: (err) => {
        // Ưu tiên hiển thị lỗi từ server trả về, nếu không có thì dùng câu thông báo mặc định
        this.errorMessage = err.error?.message || err.error || 'Tài khoản hoặc mật khẩu không chính xác.';
        this.isLoading = false;
        
        // Tự động xóa thông báo lỗi sau 5 giây để giao diện sạch sẽ hơn (Option)
        setTimeout(() => this.errorMessage = '', 5000);
      }
    });
  }

  /**
   * Phương thức hỗ trợ hiển thị/ẩn mật khẩu (Dùng cho icon mắt trên giao diện)
   * @param input Element input mật khẩu
   */
  togglePassword(input: HTMLInputElement): void {
    input.type = input.type === 'password' ? 'text' : 'password';
  }
}