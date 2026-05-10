import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RoomResponseDTO } from '../models/room.model';

@Injectable({ providedIn: 'root' })
export class RoomService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/rooms';

  getFeaturedRooms() {
    return this.http.get<RoomResponseDTO[]>(`${this.apiUrl}/featured`);
  }
}