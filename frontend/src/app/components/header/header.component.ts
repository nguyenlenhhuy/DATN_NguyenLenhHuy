import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
  <header class="fixed w-full z-50 bg-white/90 backdrop-blur-md shadow-sm">
    <div class="container mx-auto px-4 py-4 flex justify-between items-center">
      <div routerLink="/" class="text-2xl font-bold text-blue-600 cursor-pointer">LuxeHotel</div>
      <nav class="hidden md:flex space-x-6">
        <a routerLink="/" class="text-gray-700 hover:text-blue-600 font-medium">Trang chủ</a>
        <a routerLink="/rooms" class="text-gray-700 hover:text-blue-600 font-medium">Phòng</a>
        <a href="#about" class="text-gray-700 hover:text-blue-600 font-medium">Giới thiệu</a>
        <a href="#contact" class="text-gray-700 hover:text-blue-600 font-medium">Liên hệ</a>
      </nav>
      <div class="flex space-x-4 items-center">
        <ng-container *ngIf="!isLoggedIn">
          <button routerLink="/login" class="px-4 py-2 text-blue-600 font-medium hover:bg-blue-50 rounded-md">Đăng nhập</button>
          <button class="px-4 py-2 bg-blue-600 text-white font-medium rounded-md hover:bg-blue-700">Đăng ký</button>
        </ng-container>
        <ng-container *ngIf="isLoggedIn">
          <div class="flex items-center space-x-3 cursor-pointer">
            <div class="w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center text-blue-600 font-bold">U</div>
            <button (click)="logout()" class="text-sm text-red-500 hover:underline">Đăng xuất</button>
          </div>
        </ng-container>
      </div>
    </div>
  </header>
  `
})
export class HeaderComponent implements OnInit {
  isLoggedIn = false;
  ngOnInit(): void { this.isLoggedIn = !!localStorage.getItem('accessToken'); }
  logout(): void {
    localStorage.removeItem('accessToken');
    this.isLoggedIn = false;
    window.location.reload();
  }
}