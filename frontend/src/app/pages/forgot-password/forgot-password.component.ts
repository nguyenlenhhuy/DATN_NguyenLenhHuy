import { Component } from '@angular/core';
import { CommonModule } from '@angular/common'; // Để sử dụng *ngIf
import { FormsModule } from '@angular/forms'; // Để sử dụng [(ngModel)]
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink], // Đảm bảo đã import đầy đủ các module này
  templateUrl: './forgot-password.component.html'
})
export class ForgotPasswordComponent {
  // Khai báo các biến dữ liệu
  email: string = '';
  otp: string = '';
  newPassword: string = '';
  confirmPassword: string = ''; 
  step: number = 1; // 1: Nhập email, 2: Nhập OTP & Pass mới
  message: string = '';
  isLoading: boolean = false; // Trạng thái chờ khi gửi mail

  constructor(private authService: AuthService, private router: Router) {}

  /**
   * Bước 1: Gửi mã OTP về Email
   */
  sendOtp() {
    if (!this.email) {
      this.message = 'Vui lòng nhập địa chỉ Email của bạn!';
      return;
    }

    this.isLoading = true;
    this.message = 'Đang gửi mã OTP, vui lòng đợi...';

    this.authService.forgotPassword(this.email).subscribe({
      next: (res) => {
        this.message = 'Mã OTP đã được gửi về Email của bạn thành công!';
        this.step = 2; // Chuyển sang giao diện nhập OTP và mật khẩu mới
        this.isLoading = false;
      },
      error: (err) => {
        this.message = err.error || 'Email không tồn tại trong hệ thống!';
        this.isLoading = false;
      }
    });
  }

  /**
   * Bước 2: Xác nhận OTP và cập nhật mật khẩu mới
   * Lưu ý: Tên hàm phải khớp với (click) trong file .html
   */
  handleResetPassword() {
    // Kiểm tra khớp mật khẩu ở phía Client trước khi gửi
    if (this.newPassword !== this.confirmPassword) {
      this.message = 'Mật khẩu xác nhận không trùng khớp!';
      return;
    }

    if (this.newPassword.length < 6) {
      this.message = 'Mật khẩu mới phải có ít nhất 6 ký tự!';
      return;
    }

    if (!this.otp) {
      this.message = 'Vui lòng nhập mã OTP gồm 6 chữ số!';
      return;
    }

    const resetData = { 
      email: this.email, 
      otp: this.otp, 
      newPassword: this.newPassword 
    };

    this.isLoading = true;
    this.authService.resetPassword(resetData).subscribe({
      next: (res) => {
        alert('Chúc mừng! Mật khẩu của bạn đã được thay đổi thành công.');
        this.router.navigate(['/login']); // Quay lại trang đăng nhập
      },
      error: (err) => {
        this.message = err.error || 'Mã OTP không đúng hoặc đã hết hạn!';
        this.isLoading = false;
      }
    });
  }
}