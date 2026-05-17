import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './forgot-password.component.html'
})
export class ForgotPasswordComponent { // QUAN TRỌNG: Phải có chữ 'export'
  email = '';
  otp = '';
  newPassword = '';
  step = 1;

  constructor(private authService: AuthService, private router: Router) {}

  sendOtp() {
    this.authService.forgotPassword(this.email).subscribe({
      next: () => this.step = 2,
      error: (err: any) => alert(err.error) // Thêm kiểu :any để hết lỗi TS7006
    });
  }

  resetPassword() {
    const payload = { email: this.email, otp: this.otp, newPassword: this.newPassword };
    this.authService.resetPassword(payload).subscribe({
      next: () => {
        alert('Đổi mật khẩu thành công!');
        this.router.navigate(['/login']);
      },
      error: (err: any) => alert(err.error)
    });
  }
}