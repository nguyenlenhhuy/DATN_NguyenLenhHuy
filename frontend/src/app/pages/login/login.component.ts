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
  loginData: LoginRequest = { 
    username: '', 
    password: '' 
  };

  errorMessage: string = '';
  isLoading: boolean = false; 

  constructor(
    private authService: AuthService, 
    private router: Router
  ) {}

  onLogin(): void {
    this.errorMessage = '';
    this.isLoading = true;
    
    this.authService.login(this.loginData).subscribe({
      next: (res) => {
        const token = res.accessToken; 
        const role = (res.role || '').toUpperCase(); 

        if (token) {
          // 1. Lưu các trường đơn lẻ phục vụ luồng bảo mật cũ của bạn
          localStorage.setItem('token', token);
          localStorage.setItem('role', role);
          localStorage.setItem('username', res.username);
          
          // 2. ĐỒNG BỘ CHÍNH YẾU: Đóng gói thông tin tài khoản thành chuỗi JSON
          // Ép kiểu Object chứa ID thực tế trả về từ backend (res.id hoặc res.userId)
          const userSession = {
            id: res.id, // Đảm bảo Backend DTO Login của bạn có trả về trường id này nhé!
            username: res.username,
            role: role
          };
          localStorage.setItem('currentUser', JSON.stringify(userSession));

          // 3. Cập nhật trạng thái đăng nhập toàn cục
          this.authService.setLoginStatus(true);

          // 4. Điều hướng phân quyền (Role-based Routing)
          if (role === 'ADMIN' || role === 'STAFF') {
            this.router.navigate(['/admin/dashboard']);
          } else {
            this.router.navigate(['/home']);
          }
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || err.error || 'Tài khoản hoặc mật khẩu không chính xác.';
        this.isLoading = false;
        setTimeout(() => this.errorMessage = '', 5000);
      }
    });
  }

  togglePassword(input: HTMLInputElement): void {
    input.type = input.type === 'password' ? 'text' : 'password';
  }
}