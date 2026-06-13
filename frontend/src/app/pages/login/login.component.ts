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
      next: () => {
        // auth.service.ts tap() đã xử lý: lưu token/username/role, gọi setLoginStatus(true)
        const role = this.authService.getRole()?.toUpperCase() ?? '';

        const targetUrl = this.returnUrl ||
          ((role === 'ADMIN' || role === 'STAFF') ? '/admin/overview' : '/home');

        this.router.navigateByUrl(targetUrl);
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