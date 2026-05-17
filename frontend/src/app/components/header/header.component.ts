import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
  <header class="fixed w-full z-50 bg-white/90 backdrop-blur-md shadow-sm border-b border-gray-100">
    <div class="container mx-auto px-4 py-4 flex justify-between items-center">
      <div routerLink="/" class="text-2xl font-bold text-blue-600 cursor-pointer flex items-center">
        <span class="mr-2">🏨</span> LuxeHotel
      </div>

      <nav class="hidden md:flex space-x-8">
        <a routerLink="/" routerLinkActive="text-blue-600" [routerLinkActiveOptions]="{exact: true}" class="text-gray-700 hover:text-blue-600 font-medium transition">Trang chủ</a>
        <a routerLink="/rooms" routerLinkActive="text-blue-600" class="text-gray-700 hover:text-blue-600 font-medium transition">Phòng</a>
        <a href="#about" class="text-gray-700 hover:text-blue-600 font-medium transition">Giới thiệu</a>
        <a href="#contact" class="text-gray-700 hover:text-blue-600 font-medium transition">Liên hệ</a>
      </nav>

      <div class="flex items-center">
        <ng-container *ngIf="!(authService.isLoggedIn$ | async)">
          <div class="flex space-x-3">
            <button routerLink="/login" class="px-5 py-2 text-blue-600 font-semibold hover:bg-blue-50 rounded-xl transition">Đăng nhập</button>
            <button routerLink="/register" class="px-5 py-2 bg-blue-600 text-white font-semibold rounded-xl hover:bg-blue-700 shadow-md shadow-blue-200 transition active:scale-95">Đăng ký</button>
          </div>
        </ng-container>

        <ng-container *ngIf="authService.isLoggedIn$ | async">
          <div class="relative group">
            <button (click)="toggleDropdown()" class="flex items-center space-x-3 p-1 pr-3 bg-gray-50 hover:bg-gray-100 rounded-full transition border border-transparent hover:border-gray-200">
              <div class="w-9 h-9 bg-gradient-to-tr from-blue-600 to-indigo-600 rounded-full flex items-center justify-center text-white font-bold shadow-sm">
                {{ getUserInitials() }}
              </div>
              <span class="hidden sm:inline font-medium text-gray-700">{{ getUserName() }}</span>
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
              </svg>
            </button>

            <div *ngIf="isDropdownOpen" class="absolute right-0 mt-2 w-56 bg-white border border-gray-100 rounded-2xl shadow-xl py-2 z-[60]">
              <div class="px-4 py-2 border-b border-gray-50 mb-1">
                <p class="text-xs font-bold text-gray-400 uppercase">Tài khoản</p>
              </div>
              <a routerLink="/profile" class="flex items-center px-4 py-3 text-gray-700 hover:bg-blue-50 hover:text-blue-600 transition">
                <span class="mr-3">👤</span> Thông tin cá nhân
              </a>
              <a routerLink="/profile" [queryParams]="{tab: 'password'}" class="flex items-center px-4 py-3 text-gray-700 hover:bg-blue-50 hover:text-blue-600 transition">
                <span class="mr-3">🔑</span> Đổi mật khẩu
              </a>
              <a *ngIf="authService.getRole() === 'ADMIN'" routerLink="/admin" class="flex items-center px-4 py-3 text-gray-700 hover:bg-blue-50 transition">
                <span class="mr-3">🛡️</span> Quản trị hệ thống
              </a>
              <div class="border-t border-gray-50 mt-1">
                <button (click)="logout()" class="w-full flex items-center px-4 py-3 text-red-600 hover:bg-red-50 transition">
                  <span class="mr-3">🚪</span> Đăng xuất
                </button>
              </div>
            </div>
          </div>
        </ng-container>
      </div>
    </div>
  </header>
  `
})
export class HeaderComponent implements OnInit {
  isDropdownOpen = false;

  constructor(public authService: AuthService) {}

  ngOnInit(): void {}

  getUserName(): string {
    return localStorage.getItem('username') || 'Người dùng';
  }

  getUserInitials(): string {
    return this.getUserName().charAt(0).toUpperCase();
  }

  toggleDropdown(): void {
    this.isDropdownOpen = !this.isDropdownOpen;
  }

  logout(): void {
    this.isDropdownOpen = false;
    this.authService.logout();
  }
}