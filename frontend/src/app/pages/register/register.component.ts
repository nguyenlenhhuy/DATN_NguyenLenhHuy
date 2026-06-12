import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterModule],
  templateUrl: './register.component.html'
})
export class RegisterComponent implements OnInit {
  // --- Biến trạng thái (UI State) ---
  registerForm!: FormGroup;
  isWaitingOtp = false;     // Chuyển đổi giữa Form đăng ký và màn hình OTP
  isLoading = false;        // Trạng thái loading ngăn chặn spam request
  errorMsg = '';            // Hiển thị thông báo lỗi lên UI
  userEmail = '';           // Lưu trữ email hiện tại để xác thực OTP
  otpValue = '';            // Luôn gán giá trị mặc định tránh lỗi undefined .length
  isEditingEmail = false;   // Ẩn/hiện input đổi email tại màn hình OTP
  newEmailInput = '';       // Ràng buộc với ô nhập email mới

  constructor(
    private fb: FormBuilder, 
    private authService: AuthService, 
    private router: Router
  ) {}

  ngOnInit(): void {
    this.registerForm = this.fb.group({
      fullName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      phone: ['', [Validators.required, Validators.pattern('^[0-9]{10}$')]],
      terms: [false, Validators.requiredTrue] 
    });
  }

  /**
   * BƯỚC 1: Xử lý Đăng ký tài khoản (Gửi thông tin lên Backend sinh OTP)
   */
  onRegister(): void {
    if (this.registerForm.invalid || this.isLoading) {
      return;
    }

    this.isLoading = true;
    this.errorMsg = ''; 

    this.authService.register(this.registerForm.value).subscribe({
      next: (response: any) => {
        this.userEmail = this.registerForm.value.email;
        this.isWaitingOtp = true;
        this.isLoading = false;
        this.otpValue = ''; // Làm sạch ô nhập mã OTP cho lượt mới
      },
      error: (err: any) => {
        this.isLoading = false; // Đảm bảo mở khóa nút bấm nếu lỗi xảy ra
        this.handleError(err);
      }
    });
  }

  /**
   * BƯỚC 2: Xác thực mã OTP kích hoạt tài khoản
   */
  onVerify(): void {
    if (!this.otpValue || this.otpValue.length < 6) {
      this.errorMsg = 'Vui lòng nhập đầy đủ mã xác thực 6 chữ số.';
      return;
    }
    
    this.isLoading = true;
    this.errorMsg = '';

    this.authService.verifyOtp({ email: this.userEmail, otpCode: this.otpValue }).subscribe({
      next: (response: any) => {
        this.isLoading = false;
        alert('Tài khoản LuxeHotel của bạn đã được kích hoạt thành công!');
        this.router.navigate(['/login']);
      },
      error: (err: any) => {
        this.isLoading = false;
        this.handleError(err);
      }
    });
  }

  /**
   * BƯỚC PHỤ: Thay đổi Email nhận mã nếu nhập sai ban đầu
   */
  updateEmail(): void {
    const emailPattern = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
    if (!this.newEmailInput || !emailPattern.test(this.newEmailInput)) {
      this.errorMsg = 'Địa chỉ email mới không đúng định dạng.';
      return;
    }

    this.isLoading = true;
    this.errorMsg = '';

    const payload = { 
      phone: this.registerForm.value.phone, 
      oldEmail: this.userEmail, 
      newEmail: this.newEmailInput 
    };

    this.authService.updateEmail(payload).subscribe({
      next: (response: any) => {
        this.userEmail = this.newEmailInput;
        this.isEditingEmail = false;
        this.isLoading = false;
        this.newEmailInput = '';
        this.otpValue = ''; 
        alert('LuxeHotel đã cập nhật Email và gửi mã OTP mới thành công!');
      },
      error: (err: any) => {
        this.isLoading = false;
        this.handleError(err);
      }
    });
  }

  /**
   * Phân tích và hiển thị lỗi từ REST API
   */
  private handleError(err: any): void {
    console.error('Backend Error Response:', err);
    let finalMessage = 'Hệ thống LuxeHotel đang gặp sự cố. Vui lòng thử lại sau.';

    if (err.error) {
      if (typeof err.error === 'object') {
        finalMessage = err.error.message || err.error.error || finalMessage;
      } else if (typeof err.error === 'string') {
        try {
          const parsedError = JSON.parse(err.error);
          finalMessage = parsedError.message || err.error;
        } catch (e) {
          finalMessage = err.error;
        }
      }
    } else if (err.message) {
      finalMessage = err.message;
    }

    this.errorMsg = finalMessage;
    // Tăng thời gian hiển thị lên 8 giây để người dùng kịp đọc các thông báo dài
    setTimeout(() => this.errorMsg = '', 8000);
  }
}