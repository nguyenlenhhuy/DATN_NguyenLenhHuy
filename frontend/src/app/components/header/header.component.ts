import { Component, OnInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './header.component.html'
})
export class HeaderComponent implements OnInit {
  isDropdownOpen = false;
  isMobileMenuOpen = false;
  isScrolled = false;

  constructor(public authService: AuthService, private router: Router) {}

  ngOnInit(): void {}

  @HostListener('window:scroll')
  onWindowScroll(): void {
    this.isScrolled = window.scrollY > 30;
  }

  closeDropdown(): void {
    this.isDropdownOpen = false;
  }

  toggleDropdown(event?: Event): void {
    if (event) event.stopPropagation();
    this.isDropdownOpen = !this.isDropdownOpen;
    if (this.isDropdownOpen) this.isMobileMenuOpen = false;
  }

  toggleMobileMenu(): void {
    this.isMobileMenuOpen = !this.isMobileMenuOpen;
    if (this.isMobileMenuOpen) this.isDropdownOpen = false;
  }

  closeMobileMenu(): void {
    this.isMobileMenuOpen = false;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    const target = event.target as HTMLElement;
    const dropdownEl = document.getElementById('userDropdownBlock');
    if (dropdownEl && !dropdownEl.contains(target)) {
      this.isDropdownOpen = false;
    }
    const headerEl = document.querySelector('header');
    if (headerEl && !headerEl.contains(target)) {
      this.isMobileMenuOpen = false;
    }
  }

  getUserName(): string {
    return localStorage.getItem('username') || 'Người dùng';
  }

  getUserInitials(): string {
    return this.getUserName().charAt(0).toUpperCase();
  }

  logout(): void {
    if (!confirm('Bạn có chắc chắn muốn đăng xuất?')) return;
    this.isDropdownOpen = false;
    this.isMobileMenuOpen = false;
    this.authService.logout();
  }

  onRoomsClick(): void {
    this.router.navigate(['/rooms']);
    this.isMobileMenuOpen = false;
  }

  onHistoryClick(): void {
    this.isDropdownOpen = false;
    this.isMobileMenuOpen = false;
    this.router.navigate(['/history']);
  }
}