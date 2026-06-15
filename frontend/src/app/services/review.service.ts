import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

// Định nghĩa DTO gửi lên Backend
export interface ReviewRequestDTO {
  bookingId: number;
  roomId: number;
  rating: number;
  comment: string;
  mediaUrls?: string[];
}

// 🔥 CẬP NHẬT: Định nghĩa trực tiếp và EXPORT sang cho các bên dùng chung
export interface ReviewResponseDTO {
  id?: number;
  userName: string;
  roomNumber?: string;
  rating: number;
  comment: string;
  replyContent?: string;
  createdAt: string | Date;
  mediaUrls?: string[];
  hidden: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ReviewService {
  private apiUrl = `${environment.apiUrl}/reviews`;

  constructor(private http: HttpClient) {}

  // --- API DÀNH CHO CUSTOMER ---
  checkCanReview(bookingId: number, roomId: number): Observable<{ canReview: boolean; reason?: string }> {
    return this.http.get<{ canReview: boolean; reason?: string }>(`${this.apiUrl}/can-review`, {
      params: { bookingId: bookingId.toString(), roomId: roomId.toString() }
    });
  }

  submitReview(data: ReviewRequestDTO): Observable<any> {
    return this.http.post(`${this.apiUrl}/submit`, data);
  }

  getReviewsByRoomId(roomId: number): Observable<ReviewResponseDTO[]> {
    return this.http.get<ReviewResponseDTO[]>(`${this.apiUrl}/room/${roomId}`);
  }

  // --- 🔥 THÊM MỚI: API DÀNH CHO ADMIN ---
  
  // 1. Lấy danh sách phân trang cho Admin
  findAllForAdmin(page: number, size: number): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<any>(`${this.apiUrl}/admin/all`, { params });
  }

  // 2. Phản hồi bình luận
  replyReview(reviewId: number, replyContent: string): Observable<ReviewResponseDTO> {
    return this.http.put<ReviewResponseDTO>(`${this.apiUrl}/admin/${reviewId}/reply`, { replyContent });
  }

  // 3. Thay đổi trạng thái Ẩn/Hiện
  toggleReviewVisibility(reviewId: number): Observable<ReviewResponseDTO> {
    return this.http.put<ReviewResponseDTO>(`${this.apiUrl}/admin/${reviewId}/toggle-visibility`, {});
  }

  // 4. Xóa đánh giá (admin)
  deleteReview(reviewId: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/admin/${reviewId}`);
  }
}