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
  isLoading = false;        // Trạng thái loading cho các nút bấm
  errorMsg = '';            // Hiển thị thông báo lỗi lên UI
  userEmail = '';           // Lưu trữ email hiện tại để xác thực OTP
  otpValue = '';            // Ràng buộc với ô nhập mã xác thực
  isEditingEmail = false;   // Ẩn/hiện input đổi email tại màn hình OTP
  newEmailInput = '';       // Ràng buộc với ô nhập email mới

  constructor(
    private fb: FormBuilder, 
    private authService: AuthService, 
    private router: Router
  ) {}

  ngOnInit(): void {
    this.registerForm = this.fb.group({
      fullName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      phone: ['', [Validators.required, Validators.pattern('^[0-9]{10}$')]],
      terms: [false, Validators.requiredTrue] // Fix lỗi nút bị khóa
    });
  }

  /**
   * BƯỚC 1: Xử lý Đăng ký tài khoản
   */
  onRegister(): void {
    if (this.registerForm.invalid) return;

    this.isLoading = true;
    this.errorMsg = ''; 

    this.authService.register(this.registerForm.value).subscribe({
      next: () => {
        this.userEmail = this.registerForm.value.email;
        this.isWaitingOtp = true;
        this.isLoading = false;
      },
      error: (err: any) => {
        this.isLoading = false;
        this.handleError(err); // Bóc tách lỗi từ Spring Boot
      }
    });
  }

  /**
   * BƯỚC 2: Xác thực mã OTP để kích hoạt tài khoản
   */
  onVerify(): void {
    if (!this.otpValue || this.otpValue.length < 6) {
      this.errorMsg = 'Vui lòng nhập đầy đủ mã xác thực 6 chữ số.';
      return;
    }
    
    this.isLoading = true;
    this.errorMsg = '';

    this.authService.verifyOtp({ email: this.userEmail, otpCode: this.otpValue }).subscribe({
      next: () => {
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
   * BƯỚC PHỤ: Cập nhật lại Email và gửi lại OTP mới
   */
  updateEmail(): void {
    if (!this.newEmailInput || !this.newEmailInput.includes('@')) {
      this.errorMsg = 'Địa chỉ email mới không hợp lệ.';
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
      next: () => {
        this.userEmail = this.newEmailInput;
        this.isEditingEmail = false;
        this.isLoading = false;
        this.newEmailInput = '';
        alert('LuxeHotel đã cập nhật Email và gửi mã OTP mới!');
      },
      error: (err: any) => {
        this.isLoading = false;
        this.handleError(err);
      }
    });
  }

  /**
   * Xử lý lỗi từ Backend một cách chuyên nghiệp
   */
  private handleError(err: any): void {
    console.log('Backend Error Response:', err);

    let finalMessage = 'Hệ thống LuxeHotel đang gặp sự cố. Vui lòng thử lại sau.';

    // Trường hợp 1: Angular tự parse được thành Object JSON
    if (err.error && typeof err.error === 'object') {
      finalMessage = err.error.message || err.error.error || finalMessage;
    } 
    // Trường hợp 2: Backend trả về JSON nhưng dưới dạng String (Đúng như ảnh chụp của bạn)
    else if (err.error && typeof err.error === 'string') {
      try {
        // Thử ép kiểu chuỗi đó về lại JSON Object để lấy trường message
        const parsedError = JSON.parse(err.error);
        finalMessage = parsedError.message || err.error;
      } catch (e) {
        // Nếu không parse được (VD chỉ là text bình thường), lấy luôn chuỗi đó
        finalMessage = err.error;
      }
    } 
    // Trường hợp 3: Các lỗi mạng cơ bản (Network Error)
    else if (err.message) {
      finalMessage = err.message;
    }

    // Hiển thị thông báo sạch sẽ lên UI
    this.errorMsg = finalMessage;

    // Tự động ẩn thông báo lỗi sau 7 giây
    setTimeout(() => this.errorMsg = '', 7000);
  }
}