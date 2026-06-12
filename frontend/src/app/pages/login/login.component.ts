import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router'; 
import { AuthService } from '../../services/auth.service';
import { LoginRequest } from '../../models/auth.model';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink], 
  templateUrl: './login.component.html'
})
export class LoginComponent implements OnInit {
  loginData: LoginRequest = { 
    username: '', 
    password: '' 
  };

  errorMessage: string = '';
  isLoading: boolean = false; 
  returnUrl: string = ''; // 1. Khai báo biến lưu trữ URL trả về

  constructor(
    private authService: AuthService, 
    private router: Router,
    private route: ActivatedRoute // 2. Inject ActivatedRoute để đọc URL
  ) {}

  ngOnInit(): void {
    // 3. Bắt tham số 'returnUrl' trên thanh địa chỉ. Nếu không có thì để chuỗi rỗng
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '';
  }

  onLogin(): void {
    this.errorMessage = '';
    this.isLoading = true;
    
    this.authService.login(this.loginData).subscribe({
      next: (res) => {
        const token = res.accessToken; 
        const role = (res.role || '').toUpperCase(); 

        if (token) {
          // Lưu các trường đơn lẻ
          localStorage.setItem('token', token);
          localStorage.setItem('role', role);
          localStorage.setItem('username', res.username);
          
          // Đóng gói thông tin tài khoản thành chuỗi JSON
          const userSession = {
            id: res.id,
            username: res.username,
            role: role
          };
          localStorage.setItem('currentUser', JSON.stringify(userSession));

          // Cập nhật trạng thái đăng nhập toàn cục
          this.authService.setLoginStatus(true);

          // 4. XỬ LÝ CHUYỂN HƯỚNG THÔNG MINH
          let targetUrl = this.returnUrl;

          // Nếu không có returnUrl (người dùng tự bấm nút Đăng nhập trên Header) -> Điều hướng theo Role
          if (!targetUrl) {
            if (role === 'ADMIN' || role === 'STAFF') {
              targetUrl = '/admin/overview'; // Đã sửa lại khớp với app.routes.ts
            } else {
              targetUrl = '/home';
            }
          }

          // Dùng navigateByUrl để xử lý mượt mà các URL chứa tham số (ví dụ: /rooms/19)
          this.router.navigateByUrl(targetUrl).then(() => {
            // Tự động tải lại trang để Header và các Component khác cập nhật dữ liệu User mới
            window.location.reload();
          });
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