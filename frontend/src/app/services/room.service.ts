import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RoomResponseDTO } from '../models/room.model';

@Injectable({
  providedIn: 'root'
})
export class RoomService {
  private apiUrl = 'http://localhost:8080/api/rooms'; // Khớp với @RequestMapping("/api/rooms")

  constructor(private http: HttpClient) {}

  // Thêm hàm searchRooms
  searchRooms(params: any): Observable<RoomResponseDTO[]> {
    return this.http.get<RoomResponseDTO[]>(`${this.apiUrl}/search`, { params });
  }

  getFeaturedRooms(): Observable<RoomResponseDTO[]> {
    return this.http.get<RoomResponseDTO[]>(`${this.apiUrl}/featured`);
  }

  getRoomTypes(): Observable<any[]> {
    return this.http.get<any[]>(`http://localhost:8080/api/v1/management/room-types`);
  }

  getAllRooms(): Observable<RoomResponseDTO[]> {
    return this.http.get<RoomResponseDTO[]>(`${this.apiUrl}/all`); // Chỉnh URL khớp với API của bạn
  }

  // FIX CHUẨN ĐƯỜNG DẪN: Trỏ trực tiếp sang Controller quản lý thực thể Favorites 
  getFavoriteRooms(): Observable<RoomResponseDTO[]> {
    // Không dùng ${this.apiUrl}/favorites vì API gốc nằm ở /api/favorites
    return this.http.get<RoomResponseDTO[]>('http://localhost:8080/api/favorites');
  }
  getRoomById(id: number): Observable<RoomResponseDTO> {
    return this.http.get<RoomResponseDTO>(`${this.apiUrl}/${id}`);
  }
}