import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// 🔥 INTERFACE MỚI: Khớp 100% với DTO phẳng xử lý lịch sử của Backend Java
export interface BookingHistoryResponseDTO {
  bookingId: number;
  hotelName: string;
  hotelAddress: string;
  checkInDate: string;
  checkOutDate: string;
  totalPrice: number;   // Số tiền thực trả sau khi trừ voucher
  status: string;       // Hệ trạng thái: PENDING, CONFIRMED, CHECK_IN, CHECK_OUT, CANCELLED
  canReview: boolean;   // Trạng thái bật/ẩn nút viết đánh giá
  hotelImage: string;   // Giữ nguyên thuộc tính theo cấu trúc file cũ của bạn
  roomNumber: string;   // Sắp xếp gọn gàng
  roomType: string;     // Sắp xếp gọn gàng
  roomId: number;       // Sắp xếp gọn gàng
}

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
export class BookingManagementService {
  
  private apiUrl = 'http://localhost:8080/api/bookings/management/bookings';
  // 🔥 Khai báo endpoint riêng kết nối trực tiếp đến ReviewController của bạn
  private reviewApiUrl = 'http://localhost:8080/api/reviews'; 

  constructor(private http: HttpClient) {}

  getAllBookings(): Observable<BookingResponseDTO[]> {
    return this.http.get<BookingResponseDTO[]>(this.apiUrl);
  }

  getAvailableRooms(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/available-rooms`);
  }

  getAvailablePromotions(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/available-promotions`);
  }

  getPreviewPrice(roomNumber: string, checkInDate: string, checkOutDate: string, appliedCode?: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/preview-price`, {
      params: { roomNumber, checkInDate, checkOutDate, appliedCode: appliedCode || '' }
    });
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

  getDashboardStats(filterType: string): Observable<any> {
    return this.http.get<any>(`http://localhost:8080/api/bookings/management/dashboard-stats`, {
      params: { filterType }
    });
  }

  cancelBooking(bookingId: number, staffId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/${bookingId}/cancel?staffId=${staffId}`, {});
  }

  /**
   * ✔️ ĐÃ KHỚP KIỂU DỮ LIỆU:
   * Gửi yêu cầu lấy lịch sử bằng trường định danh duy nhất Username
   */
  getCustomerHistory(): Observable<BookingHistoryResponseDTO[]> {
    return this.http.get<BookingHistoryResponseDTO[]>(`http://localhost:8080/api/bookings/customer/history`);
  }

  // =========================================================================
  // ⚡ ĐÃ THÊM: HÀM GỬI ĐÁNH GIÁ CHUẨN ĐỒNG BỘ VỚI REVIEWREQUESTDTO CỦA BACKEND
  // =========================================================================
  submitReview(bookingId: number, rating: number, comment: string): Observable<any> {
    const payload = {
      bookingId: bookingId,
      rating: rating,
      comment: comment,
      mediaUrls: [] // Đóng gói mảng rỗng để khớp với List<String> của ReviewRequestDTO dưới Java
    };
    
    // Bắn gói tin HTTP POST gọi trực tiếp đến endpoint xử lý lưu đánh giá thời gian thực
    return this.http.post(`${this.reviewApiUrl}/submit`, payload);
  }
}