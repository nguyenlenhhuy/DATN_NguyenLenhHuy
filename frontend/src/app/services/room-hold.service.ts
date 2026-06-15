import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface HoldResponse {
  success: boolean;
  holdToken?: string;
  expiresInSeconds?: number;
  message?: string;
  error?: string;
}

export interface HoldStatus {
  isHeld: boolean;
  expiresInSeconds?: number;
}

@Injectable({ providedIn: 'root' })
export class RoomHoldService {

  private readonly apiUrl = `${environment.apiUrl}/rooms`;

  constructor(private http: HttpClient) {}

  /**
   * Chiếm giữ phòng — gọi khi user vào trang room-detail.
   * Trả về HoldResponse gồm holdToken và expiresInSeconds.
   */
  acquireHold(roomId: number): Observable<HoldResponse> {
    return this.http.post<HoldResponse>(`${this.apiUrl}/${roomId}/hold`, {}).pipe(
      catchError(err => {
        const msg = err?.error?.message || err?.error?.error || 'Phòng đang được người khác xử lý.';
        return of({ success: false, message: msg } as HoldResponse);
      })
    );
  }

  /**
   * Gia hạn hold (gọi định kỳ mỗi 5 phút để tránh hết hạn khi user còn trên trang).
   */
  renewHold(roomId: number, holdToken: string): Observable<HoldResponse> {
    return this.http.put<HoldResponse>(
      `${this.apiUrl}/${roomId}/hold/renew`, {},
      { params: { holdToken } }
    ).pipe(catchError(() => of({ success: false } as HoldResponse)));
  }

  /**
   * Giải phóng hold — gọi khi user rời trang hoặc đặt phòng xong.
   */
  releaseHold(roomId: number, holdToken: string): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/${roomId}/hold`,
      { params: { holdToken } }
    ).pipe(catchError(() => of(undefined as any)));
  }

  /**
   * Kiểm tra trạng thái hold của một phòng (dùng trên trang danh sách).
   */
  getHoldStatus(roomId: number): Observable<HoldStatus> {
    return this.http.get<HoldStatus>(`${this.apiUrl}/${roomId}/hold-status`).pipe(
      catchError(() => of({ isHeld: false }))
    );
  }

  /**
   * Lấy toàn bộ roomId đang bị giữ — dùng để render badge trên danh sách phòng.
   */
  getHeldRoomIds(): Observable<number[]> {
    return this.http.get<number[]>(`${this.apiUrl}/held`).pipe(
      catchError(() => of([]))
    );
  }
}
