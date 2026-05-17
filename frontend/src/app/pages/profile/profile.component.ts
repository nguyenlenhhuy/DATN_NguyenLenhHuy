import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { UserService } from '../../services/user.service';
import { RouterLink } from '@angular/router';
@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterLink],
  templateUrl: './profile.component.html'
})
export class ProfileComponent implements OnInit {
  // Khởi tạo đối tượng user với các giá trị rỗng để tránh lỗi "fullName of undefined" ở HTML
  user: any = { fullName: '', email: '', phone: '', username: '', roleName: '' };
  
  // Khởi tạo Form ngay lập tức để tránh lỗi "Cannot read properties of undefined (reading 'get')"
  passwordForm: FormGroup;
  
  message = '';
  isError = false;
  isLoading = false;

  constructor(private userService: UserService, private fb: FormBuilder) {
    // Khởi tạo cấu trúc form ngay trong constructor
    this.passwordForm = this.fb.group({
      oldPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required]
    }, { validators: this.passwordMatchValidator });
  }

  ngOnInit() {
    this.loadProfile();
  }

  // Validator kiểm tra khớp mật khẩu
  passwordMatchValidator(g: FormGroup) {
    const newPass = g.get('newPassword')?.value;
    const confirmPass = g.get('confirmPassword')?.value;
    return newPass === confirmPass ? null : { mismatch: true };
  }

  loadProfile() {
    this.isLoading = true;
    this.userService.getProfile().subscribe({
      next: (data) => {
        this.user = data;
        this.isLoading = false;
        console.log("Dữ liệu profile đã tải:", data);
      },
      error: (err) => {
        console.error("Lỗi lấy profile:", err);
        this.isLoading = false;
      }
    });
  }

  onUpdateInfo() {
    this.isLoading = true;
    this.message = '';
    this.userService.updateProfile(this.user).subscribe({
      next: (res) => {
        this.message = "Cập nhật thông tin thành công!";
        this.isError = false;
        this.isLoading = false;
        this.loadProfile(); 
      },
      error: (err) => {
        this.message = err.error || 'Cập nhật thông tin thất bại';
        this.isError = true;
        this.isLoading = false;
      }
    });
  }

  onChangePassword() {
    if (this.passwordForm.valid) {
      this.isLoading = true;
      this.message = '';
      this.userService.changePassword(this.passwordForm.value).subscribe({
        next: (res) => {
          this.message = "Đổi mật khẩu thành công!";
          this.isError = false;
          this.passwordForm.reset();
          this.isLoading = false;
        },
        error: (err) => {
          this.message = err.error || 'Mật khẩu cũ không chính xác';
          this.isError = true;
          this.isLoading = false;
        }
      });
    }
  }
}