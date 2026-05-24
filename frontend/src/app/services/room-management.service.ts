import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RoomRequest, RoomTypeRequest, RoomResponseDTO, RoomType, RoomStatus } from '../models/room-management.model';

@Injectable({
  providedIn: 'root'
})
export class RoomManagementService {
  // Tiền tố dẫn vào các Endpoint quản lý của Spring Boot
  private baseUrl = 'http://localhost:8080/api/v1/management';

  constructor(private http: HttpClient) {}

  // =========================================================================
  // --- PHÂN HỆ APIs DÀNH CHO LOẠI PHÒNG (CHỈ ADMIN) ---
  // =========================================================================

  // 1. API lấy toàn bộ danh sách loại phòng động từ DB để render lên bảng phụ và thẻ Select
  getRoomTypes(): Observable<RoomType[]> {
    return this.http.get<RoomType[]>(`${this.baseUrl}/room-types`);
  }

  // 2. API khởi tạo phân loại phòng mới
  createRoomType(request: RoomTypeRequest | any): Observable<RoomType> {
    return this.http.post<RoomType>(`${this.baseUrl}/room-types`, request);
  }

  // 3. API xóa loại phòng theo ID (Backend trả về JSON chứa trường 'message')
  deleteRoomType(id: number): Observable<any> {
    return this.http.delete<any>(`${this.baseUrl}/room-types/${id}`);
  }

  // =========================================================================
  // --- PHÂN HỆ APIs DÀNH CHO PHÒNG VẬT LÝ (ADMIN & STAFF) ---
  // =========================================================================

  // 1. API khởi tạo 1 phòng đơn lẻ (Có tích hợp bẫy lỗi Tái sinh phòng cũ tại Java Service)
  createRoom(request: RoomRequest): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/rooms`, request);
  }

  // 2. API khởi tạo hàng loạt chuỗi phòng theo tầng kèm bộ sưu tập ảnh mẫu
  createBulkRooms(payload: any): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/rooms/batch`, payload);
  }

  // 3. API cập nhật thông tin chi tiết phòng và ghi đè album ảnh mới
  updateRoom(roomId: number, payload: any): Observable<any> {
    return this.http.put<any>(`${this.baseUrl}/rooms/${roomId}`, payload);
  }

  // 4. API xóa mềm phòng vật lý (Chuyển trạng thái hoạt động vào vùng ẩn ngầm)
  deleteRoom(id: number): Observable<any> {
    return this.http.delete<any>(`${this.baseUrl}/rooms/${id}`);
  }

  // 5. API lấy danh sách sơ đồ lưới phòng trực quan (Chỉ lấy các phòng is_deleted = false)
  getRoomsByHotel(hotelId: number): Observable<RoomResponseDTO[]> {
    return this.http.get<RoomResponseDTO[]>(`${this.baseUrl}/hotels/${hotelId}/rooms`);
  }

  // 6. API cập nhật nhanh trạng thái ngoài màn hình (Gửi chuỗi Enum sạch qua RequestParam)
  updateRoomStatus(id: number, status: RoomStatus): Observable<any> {
    const params = new HttpParams().set('status', status);
    return this.http.patch<any>(`${this.baseUrl}/rooms/${id}/status`, null, { params });
  }
}