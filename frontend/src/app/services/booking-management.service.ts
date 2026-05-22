import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// BẮT BUỘC CÓ TỪ KHÓA EXPORT: Để file component có thể import được cấu trúc dữ liệu
export interface BookingResponseDTO {
  bookingId: number;
  roomNumber: string;
  customerName: string;
  customerPhone: string;
  customerCccd: string;
  checkInDate: string;
  checkOutDate: string;
  originalPrice: number;
  discountAmount: number;
  finalAmount: number;
  bookingStatus: string;
  paymentStatus: string;
  appliedCode?: string;
  paymentMethod?: string;
}

export interface WalkInBookingRequestDTO {
  roomNumber: string;
  customerName: string;
  customerPhone: string;
  customerCccd: string;
  checkInDate: string;
  checkOutDate: string;
  appliedCode?: string;
}

@Injectable({
  providedIn: 'root'
})
// BẮT BUỘC CÓ TỪ KHÓA EXPORT: Định nghĩa đây là một Module Service để tiêm (Inject) vào Component
export class BookingManagementService {
  
  private apiUrl = 'http://localhost:8080/api/bookings/management/bookings';

  constructor(private http: HttpClient) {}

  getAllBookings(): Observable<BookingResponseDTO[]> {
    return this.http.get<BookingResponseDTO[]>(this.apiUrl);
  }

  getAvailableRooms(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/available-rooms`);
  }

  createWalkInBooking(request: WalkInBookingRequestDTO): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/walk-in`, request);
  }

  processCheckIn(bookingId: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/${bookingId}/check-in`, {});
  }

  processCheckOut(bookingId: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/${bookingId}/check-out`, {});
  }
  getAvailablePromotions(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/available-promotions`);
  }
}