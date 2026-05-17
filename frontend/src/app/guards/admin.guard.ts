import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const adminGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const role = authService.getRole();
  
  if (authService.isLoggedIn() && (role === 'ADMIN' || role === 'STAFF')) {
    return true;
  }

  // Nếu không có quyền, điều hướng về trang đăng nhập
  router.navigate(['/login']);
  return false;
};