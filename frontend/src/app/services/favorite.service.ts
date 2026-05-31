import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RoomResponseDTO } from '../models/room-management.model';

@Injectable({
  providedIn: 'root'
})
export class FavoriteService {
  private apiUrl = 'http://localhost:8080/api/favorites'; // Điều chỉnh port cho đúng backend của bạn

  constructor(private http: HttpClient) { }

  toggleFavorite(roomId: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/toggle/${roomId}`, {});
  }
  getMyFavorites(): Observable<RoomResponseDTO[]> {
    return this.http.get<RoomResponseDTO[]>(`${this.apiUrl}`);
  }
}