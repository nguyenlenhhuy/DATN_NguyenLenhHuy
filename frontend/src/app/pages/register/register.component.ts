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
  registerForm!: FormGroup;
  isWaitingOtp = false;
  isLoading = false;
  userEmail = '';
  otpValue = '';     // Đảm bảo có biến này cho ngModel
  errorMsg = '';     // Đảm bảo có biến này cho *ngIf="errorMsg"
  isEditingEmail = false;
  newEmailInput = '';

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
      phone: ['', [Validators.required, Validators.pattern('^[0-9]{10}$')]]
    });
  }

  onRegister(): void {
    if (this.registerForm.valid) {
      this.isLoading = true;
      this.errorMsg = '';
      this.authService.register(this.registerForm.value).subscribe({
        next: () => {
          this.userEmail = this.registerForm.value.email;
          this.isWaitingOtp = true;
          this.isLoading = false;
        },
        error: (err: any) => {
          this.errorMsg = err.error || 'Đã có lỗi xảy ra khi đăng ký';
          this.isLoading = false;
        }
      });
    }
  }

  // SỬA TẠI ĐÂY: Đổi tên thành onVerify() để khớp với (click)="onVerify()" trong HTML
  onVerify(): void {
    if (!this.otpValue) {
      this.errorMsg = 'Vui lòng nhập mã OTP';
      return;
    }
    this.isLoading = true;
    this.authService.verifyOtp({ email: this.userEmail, otpCode: this.otpValue }).subscribe({
      next: () => {
        alert('Xác thực thành công! Mời bạn đăng nhập.');
        this.router.navigate(['/login']);
      },
      error: (err: any) => {
        this.errorMsg = err.error || 'Mã OTP không chính xác';
        this.isLoading = false;
      }
    });
  }

  updateEmail(): void {
    if (!this.newEmailInput) return;
    const payload = { 
      phone: this.registerForm.value.phone, 
      oldEmail: this.userEmail, 
      newEmail: this.newEmailInput 
    };
    this.authService.updateEmail(payload).subscribe({
      next: () => {
        this.userEmail = this.newEmailInput;
        this.isEditingEmail = false;
        alert('Đã cập nhật email và gửi mã OTP mới!');
      },
      error: (err: any) => this.errorMsg = err.error
    });
  }
}