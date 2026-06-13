import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const adminGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Lấy dữ liệu thô từ AuthService
  const rawRole = authService.getRole();
  const isLoggedIn = authService.isLoggedIn();

  // --- PHẦN NÂNG CẤP: Xử lý an toàn dữ liệu ---
  let role = '';

  if (rawRole) {
    // Nếu rawRole là một chuỗi JSON (ví dụ: "[object Object]" hoặc "{"roleType":"ADMIN"}")
    if (typeof rawRole === 'string' && rawRole.includes('{')) {
      try {
        const parsed = JSON.parse(rawRole);
        role = parsed.roleType || parsed.name || rawRole;
      } catch (e) {
        role = rawRole;
      }
    } else {
      role = String(rawRole); // Đảm bảo luôn là chuỗi
    }
  }

  // --- LOGIC KIỂM TRA QUYỀN ---
  if (isLoggedIn && role) {
    const upperRole = role.toUpperCase();
    
    // Kiểm tra nếu chứa ADMIN hoặc STAFF (Chấp nhận cả ADMIN, ROLE_ADMIN, STAFF, ...)
    if (upperRole.includes('ADMIN') || upperRole.includes('STAFF')) {
      return true;
    }
  }

  router.navigate(['/login']);
  return false;
};