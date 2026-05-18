import { Component, OnInit } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './admin-layout.component.html',
})
export class AdminLayoutComponent implements OnInit {
  currentUsername: string = 'Quản trị viên';

  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit(): void {
    this.currentUsername = localStorage.getItem('username') || 'Admin';
  }

  logout(): void {
    // 1. Gọi service để xóa token/session
    this.authService.logout(); 
    
    // 2. Điều hướng về trang chủ thay vì '/login'
    this.router.navigate(['/']); 
  }
}