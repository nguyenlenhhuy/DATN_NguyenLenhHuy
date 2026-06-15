import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface AuditLogDTO {
  action: string;
  actionLabel: string;
  description: string;
  operatorName: string;
  createdAt: string;
}

export interface RoomInfo {
  roomId: number;
  roomNumber: string;
  roomType: string;
}

export interface BookingHistoryResponseDTO {
  bookingId: number;
  hotelName: string;
  hotelAddress: string;
  checkInDate: string;
  checkOutDate: string;
  totalPrice: number;
  status: string;
  canReview: boolean;
  hotelImage?: string;
  // Phòng đầu tiên (backward-compat, dùng cho re-book và review context)
  roomNumber: string;
  roomType: string;
  roomId: number;
  // Danh sách đầy đủ tất cả phòng (batch booking)
  rooms?: RoomInfo[];
  roomCount?: number;
}

export interface BookingResponseDTO {
  bookingId: number;
  roomNumber: string;
  roomNumbers?: string[];
  roomCount?: number;
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

export interface DashboardStatsDTO {
  totalRevenueToday: number;
  totalRevenuePeriod: number;   // Tổng doanh thu của kỳ đang xem
  totalBookingsToday: number;
  availableRooms: number;
  totalRooms: number;
  last7DaysRevenue?: Record<string, number>;
}

export interface AvailableRoomInfo {
  roomNumber: string;
  floor: number;
  typeName: string;
  price: number;
}

export interface WalkInBookingRequestDTO {
  roomNumber: string;
  roomNumbers?: string[];
  customerName: string;
  customerPhone: string;
  customerCccd: string;
  customerEmail?: string;
  checkInDate: string;
  checkOutDate: string;
  appliedCode?: string;
}

@Injectable({
  providedIn: 'root'
})
export class BookingManagementService {
  
  private apiUrl = `${environment.apiUrl}/bookings/management/bookings`;
  private reviewApiUrl = `${environment.apiUrl}/reviews`;

  constructor(private http: HttpClient) {}

  getAllBookings(): Observable<BookingResponseDTO[]> {
    return this.http.get<BookingResponseDTO[]>(this.apiUrl);
  }

  getAvailableRooms(checkIn?: string, checkOut?: string): Observable<string[]> {
    const params: Record<string, string> = {};
    if (checkIn)  params['checkIn']  = checkIn;
    if (checkOut) params['checkOut'] = checkOut;
    return this.http.get<string[]>(`${this.apiUrl}/available-rooms`, { params });
  }

  getAvailableRoomsDetail(checkIn?: string, checkOut?: string): Observable<AvailableRoomInfo[]> {
    const params: Record<string, string> = {};
    if (checkIn)  params['checkIn']  = checkIn;
    if (checkOut) params['checkOut'] = checkOut;
    return this.http.get<AvailableRoomInfo[]>(`${this.apiUrl}/available-rooms-detail`, { params });
  }

  getPreviewPriceMulti(roomNumbers: string[], checkInDate: string, checkOutDate: string, appliedCode?: string): Observable<any> {
    let params = new HttpParams().set('checkInDate', checkInDate).set('checkOutDate', checkOutDate);
    if (appliedCode) params = params.set('appliedCode', appliedCode);
    roomNumbers.forEach(r => { params = params.append('roomNumbers', r); });
    return this.http.get<any>(`${this.apiUrl}/preview-price-multi`, { params });
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

  getDashboardStats(filterType: string): Observable<DashboardStatsDTO> {
    return this.http.get<DashboardStatsDTO>(`${environment.apiUrl}/bookings/management/dashboard-stats`, {
      params: { filterType }
    });
  }

  getOccupancyCalendar(year: number, month: number): Observable<Record<string, number>> {
    return this.http.get<Record<string, number>>(
      `${environment.apiUrl}/bookings/management/occupancy-calendar`,
      { params: { year: year.toString(), month: month.toString() } }
    );
  }

  getOccupancyDayDetail(date: string): Observable<any> {
    return this.http.get<any>(
      `${environment.apiUrl}/bookings/management/occupancy-calendar/day-detail`,
      { params: { date } }
    );
  }

  cancelBooking(bookingId: number, staffId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/${bookingId}/cancel?staffId=${staffId}`, {});
  }

  getBookingAuditLogs(bookingId: number): Observable<AuditLogDTO[]> {
    return this.http.get<AuditLogDTO[]>(`${this.apiUrl}/${bookingId}/audit-logs`);
  }

  processRefund(bookingId: number, operatorId: number, operatorName: string, reason: string): Observable<any> {
    const params = new HttpParams()
      .set('operatorId', operatorId.toString())
      .set('operatorName', operatorName)
      .set('reason', reason);
    return this.http.post(`${this.apiUrl}/${bookingId}/refund`, {}, { params });
  }

  /**
   * ✔️ ĐÃ KHỚP KIỂU DỮ LIỆU:
   * Gửi yêu cầu lấy lịch sử bằng trường định danh duy nhất Username
   */
  getCustomerHistory(): Observable<BookingHistoryResponseDTO[]> {
    return this.http.get<BookingHistoryResponseDTO[]>(`${environment.apiUrl}/bookings/customer/history`);
  }

  // =========================================================================
  // ⚡ ĐÃ THÊM: HÀM GỬI ĐÁNH GIÁ CHUẨN ĐỒNG BỘ VỚI REVIEWREQUESTDTO CỦA BACKEND
  // =========================================================================
  submitReview(bookingId: number, roomId: number, rating: number, comment: string): Observable<any> {
    const payload = { bookingId, roomId, rating, comment, mediaUrls: [] };
    return this.http.post(`${this.reviewApiUrl}/submit`, payload);
  }
}