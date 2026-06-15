import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RoomResponseDTO } from '../models/room.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class RoomService {
  private apiUrl = `${environment.apiUrl}/rooms`;

  constructor(private http: HttpClient) {}

  searchRooms(params: any): Observable<RoomResponseDTO[]> {
    const cleanParams: Record<string, string> = {};
    Object.keys(params).forEach(key => {
      const val = params[key];
      if (val !== null && val !== undefined && val !== '') {
        cleanParams[key] = String(val);
      }
    });
    return this.http.get<RoomResponseDTO[]>(`${this.apiUrl}/search`, { params: cleanParams });
  }

  getFeaturedRooms(): Observable<RoomResponseDTO[]> {
    return this.http.get<RoomResponseDTO[]>(`${this.apiUrl}/featured`);
  }

  getRoomTypes(): Observable<any[]> {
    return this.http.get<any[]>(`${environment.apiUrl}/v1/management/room-types`);
  }

  getAllRooms(): Observable<RoomResponseDTO[]> {
    return this.http.get<RoomResponseDTO[]>(`${this.apiUrl}/all`); // Chỉnh URL khớp với API của bạn
  }

  // FIX CHUẨN ĐƯỜNG DẪN: Trỏ trực tiếp sang Controller quản lý thực thể Favorites 
  getFavoriteRooms(): Observable<RoomResponseDTO[]> {
    // Không dùng ${this.apiUrl}/favorites vì API gốc nằm ở /api/favorites
    return this.http.get<RoomResponseDTO[]>(`${environment.apiUrl}/favorites`);
  }
  getRoomById(id: number): Observable<RoomResponseDTO> {
    return this.http.get<RoomResponseDTO>(`${this.apiUrl}/${id}`);
  }

  getRoomAvailability(roomId: number, from?: string, to?: string): Observable<{ checkIn: string; checkOut: string }[]> {
    const params: Record<string, string> = {};
    if (from) params['from'] = from;
    if (to)   params['to']   = to;
    return this.http.get<{ checkIn: string; checkOut: string }[]>(
      `${this.apiUrl}/${roomId}/availability`, { params }
    );
  }
}