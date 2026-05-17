import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { LoginRequest } from '../../models/auth.model';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html'
})
export class LoginComponent {
  loginData: LoginRequest = { username: '', password: '' };
  errorMessage: string = '';

  constructor(private authService: AuthService, private router: Router) {}

 onLogin(): void {
  this.errorMessage = '';
  this.authService.login(this.loginData).subscribe({
    next: (res) => {
      // DÒNG NÀY CỰC KỲ QUAN TRỌNG: Huy hãy mở F12 -> Console để xem kết quả này
      console.log('Dữ liệu thực tế từ Backend:', res); 

      // Kiểm tra xem tên biến có đúng là 'token' không hay là 'accessToken', 'jwt',...
      if (res && (res.token || res.accessToken)) { 
        const jwtToken = res.token || res.accessToken; // Linh hoạt lấy token
        
        localStorage.setItem('token', jwtToken);
        localStorage.setItem('role', res.role);
        localStorage.setItem('username', res.username);
        
        this.authService.setLoginStatus(true);

        if (res.role === 'ADMIN' || res.role === 'STAFF') {
          this.router.navigate(['/admin/dashboard']);
        } else {
          this.router.navigate(['/home']);
        }
      } else {
        // Đây chính là dòng gây ra thông báo lỗi trên hình của bạn
        this.errorMessage = 'Dữ liệu phản hồi từ máy chủ không hợp lệ (Thiếu Token).';
      }
    },
    error: (err) => {
      this.errorMessage = err.error || 'Đăng nhập thất bại.';
    }
  });
  }
}