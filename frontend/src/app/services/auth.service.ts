import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { Router } from '@angular/router';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private apiUrl = 'http://localhost:8080/api/auth';
  
  // Quản lý trạng thái đăng nhập toàn cục
  private loggedIn = new BehaviorSubject<boolean>(this.isLoggedIn());
  isLoggedIn$ = this.loggedIn.asObservable();

  constructor(private http: HttpClient, private router: Router) {}

  // Hàm phát tín hiệu đăng nhập/đăng xuất
  setLoginStatus(status: boolean): void {
    this.loggedIn.next(status);
  }

  // Đăng nhập: Tự động lưu Token và thông tin User
  login(data: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/login`, data).pipe(
      tap(res => {
        // Hỗ trợ cả trường 'token' hoặc 'accessToken' tùy Backend
        const jwtToken = res.token || res.accessToken;
        if (jwtToken) {
          localStorage.setItem('token', jwtToken);
          localStorage.setItem('role', res.role);
          localStorage.setItem('username', res.username);
          this.setLoginStatus(true);
        }
      })
    );
  }

  // Đăng ký tài khoản
  register(data: any): Observable<string> {
    return this.http.post(`${this.apiUrl}/register`, data, { responseType: 'text' });
  }

  // Xác thực mã OTP
  verifyOtp(data: { email: string, otpCode: string }): Observable<string> {
    return this.http.post(`${this.apiUrl}/verify-otp`, data, { responseType: 'text' });
  }

  // Cập nhật lại Email khi cần thiết
  updateEmail(data: any): Observable<string> {
    return this.http.put(`${this.apiUrl}/update-email`, data, { responseType: 'text' });
  }

  // Gửi yêu cầu lấy lại mật khẩu
  forgotPassword(email: string): Observable<string> {
    return this.http.post(`${this.apiUrl}/forgot-password`, { email }, { responseType: 'text' });
  }

  // Đặt lại mật khẩu mới bằng mã xác nhận
  resetPassword(data: any): Observable<string> {
    return this.http.post(`${this.apiUrl}/reset-password`, data, { responseType: 'text' });
  }

  // Kiểm tra đăng nhập (có lọc 'undefined' và 'null')
  isLoggedIn(): boolean {
    const token = localStorage.getItem('token');
    return !!token && token !== 'undefined' && token !== 'null';
  }

  getRole(): string | null {
    return localStorage.getItem('role');
  }

  // Đăng xuất: Xóa bộ nhớ và về trang chủ
  logout(): void {
    if (confirm('Bạn có chắc chắn muốn đăng xuất?')) {
      localStorage.clear();
      this.setLoginStatus(false);
      this.router.navigate(['/home']).then(() => {
        window.location.reload(); 
      });
    }
  }
}