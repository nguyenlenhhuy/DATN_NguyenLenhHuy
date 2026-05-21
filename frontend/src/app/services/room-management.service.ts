import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RoomRequest, RoomTypeRequest, RoomResponseDTO, RoomType, RoomStatus } from '../models/room-management.model';

@Injectable({
  providedIn: 'root'
})
export class RoomManagementService {
  private baseUrl = 'http://localhost:8080/api/v1/management';

  constructor(private http: HttpClient) {}

  // --- Các APIs dành cho Loại phòng (CHỈ ADMIN) ---
  createRoomType(request: RoomTypeRequest): Observable<RoomType> {
    return this.http.post<RoomType>(`${this.baseUrl}/room-types`, request);
  }

  deleteRoomType(id: number): Observable<string> {
    return this.http.delete(`${this.baseUrl}/room-types/${id}`, { responseType: 'text' });
  }

  // --- Các APIs dành cho Phòng (ADMIN & STAFF) ---
  createRoom(request: RoomRequest): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/rooms`, request);
  }

  deleteRoom(id: number): Observable<string> {
    return this.http.delete(`${this.baseUrl}/rooms/${id}`, { responseType: 'text' });
  }

  // API lấy danh sách sơ đồ phòng theo khách sạn hiện tại
  getRoomsByHotel(hotelId: number): Observable<RoomResponseDTO[]> {
    return this.http.get<RoomResponseDTO[]>(`${this.baseUrl}/hotels/${hotelId}/rooms`);
  }

  // API cập nhật nhanh trạng thái dành cho cả Staff (Sử dụng RequestParam chuỗi)
  updateRoomStatus(id: number, status: RoomStatus): Observable<string> {
    const params = new HttpParams().set('status', status);
    return this.http.patch(`${this.baseUrl}/rooms/${id}/status`, null, { 
      params, 
      responseType: 'text' 
    });
  }
  // Thêm đoạn này vào file room-management.service.ts của Angular

// Đảm bảo đã import Observable ở đầu file: import { Observable } from 'rxjs';

createBulkRooms(payload: any): Observable<string> {
    // SỬA TẠI ĐÂY: Ghép đúng baseUrl (8080) vào trước endpoint /rooms/batch
    return this.http.post(`${this.baseUrl}/rooms/batch`, payload, { 
      responseType: 'text' 
    });
  }
  updateRoom(roomId: number, payload: any): Observable<any> {
  return this.http.put(`${this.baseUrl}/rooms/${roomId}`, payload);
}
}