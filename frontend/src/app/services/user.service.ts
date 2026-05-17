import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class UserService {
  private apiUrl = 'http://localhost:8080/api/users';

  constructor(private http: HttpClient) {}

  private getHeaders() {
    const token = localStorage.getItem('token');
    
    // Kiểm tra để tránh gửi chuỗi 'undefined' hoặc 'null' lên Backend
    if (!token || token === 'undefined' || token === 'null') {
      return new HttpHeaders();
    }

    // THÊM DẤU CÁCH: "Bearer " để Backend cắt chuỗi chính xác
    return new HttpHeaders().set('Authorization', `Bearer ${token}`);
  }

  getProfile(): Observable<any> {
    return this.http.get(`${this.apiUrl}/profile`, { headers: this.getHeaders() });
  }

  // Kết nối tới @PutMapping("/update")
  updateProfile(data: any): Observable<string> {
    return this.http.put(`${this.apiUrl}/update`, data, { 
      headers: this.getHeaders(), 
      responseType: 'text' 
    });
  }

  // Kết nối tới @PutMapping("/change-password")
  changePassword(data: any): Observable<string> {
    return this.http.put(`${this.apiUrl}/change-password`, data, { 
      headers: this.getHeaders(),
      responseType: 'text' 
    });
  }
}