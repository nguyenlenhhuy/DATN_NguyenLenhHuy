import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface BookingRequestDTO {
  userId?: number;
  roomId: number;
  checkIn: string;
  checkOut: string;
  paymentMethod: string;
}

@Injectable({
  providedIn: 'root'
})
export class BookingService {
  // Thay đổi URL theo cấu hình Backend của bạn
  private apiUrl = `${environment.apiUrl}/bookings`;

  constructor(private http: HttpClient) {}

  // =========================================================================
  // GỌI API KHÁCH HÀNG TẠO ĐƠN
  // =========================================================================

  createBooking(data: BookingRequestDTO): Observable<any> {
    // Nếu bạn đang dùng Token xác thực, interceptor sẽ tự động đính kèm
    // hoặc bạn có thể thủ công truyền token tại đây nếu chưa cấu hình interceptor
    return this.http.post<any>(this.apiUrl, data);
  }

  // (Optional) Lịch sử đơn hàng của người dùng hiện tại
  getMyHistory(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/history`);
  }
}