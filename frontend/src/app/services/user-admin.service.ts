import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { User } from '../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class UserAdminService {
  private http = inject(HttpClient);
  private readonly API_URL = 'http://localhost:8080/api/admin/users';

  // Lấy toàn bộ danh sách người dùng
  getAllUsers(): Observable<User[]> {
    return this.http.get<User[]>(this.API_URL);
  }

  // Tạo nhân viên mới (Khớp với @PostMapping("/create-staff"))
  createStaff(staffData: any): Observable<string> {
    return this.http.post(`${this.API_URL}/create-staff`, staffData, { 
      responseType: 'text' 
    });
  }

  // Cập nhật vai trò (Khớp với @PatchMapping("/{id}/role"))
  updateRole(id: number, newRole: string): Observable<string> {
    const params = new HttpParams().set('newRole', newRole);
    return this.http.patch(`${this.API_URL}/${id}/role`, null, { 
      params, 
      responseType: 'text' 
    });
  }

  // Cập nhật trạng thái Khóa/Mở (Khớp với @PatchMapping("/{id}/status"))
  updateStatus(id: number, status: string): Observable<string> {
    const params = new HttpParams().set('status', status);
    return this.http.patch(`${this.API_URL}/${id}/status`, null, { 
      params, 
      responseType: 'text' 
    });
  }

  // Xóa mềm tài khoản (Khớp với @DeleteMapping("/{id}"))
  deactivateUser(id: number): Observable<string> {
    return this.http.delete(`${this.API_URL}/${id}`, { 
      responseType: 'text' 
    });
  }
}