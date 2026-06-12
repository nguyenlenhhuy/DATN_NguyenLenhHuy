import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('token');
  const router = inject(Router);

  // 1. Gắn Token vào request nếu có
  let authReq = req;
  if (token) {
    authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  // 2. Xử lý response trả về
  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // Nếu Backend báo lỗi không có quyền (401) hoặc bị cấm (403)
      if (error.status === 401 || error.status === 403) {
        console.warn('Truy cập bị từ chối hoặc Token hết hạn.');
        
        // Dọn dẹp dữ liệu cũ rác
        localStorage.removeItem('token');
        localStorage.removeItem('username');
        localStorage.removeItem('role');
        
        // Điều hướng về trang login
        router.navigate(['/login']);
      }
      return throwError(() => error);
    })
  );
};