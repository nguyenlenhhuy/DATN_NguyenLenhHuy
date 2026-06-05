import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

// Định nghĩa DTO gửi lên Backend
export interface ReviewRequestDTO {
  bookingId: number;
  rating: number;
  comment: string;
  mediaUrls?: string[];
}

// 🔥 CẬP NHẬT: Định nghĩa trực tiếp và EXPORT sang cho các bên dùng chung
export interface ReviewResponseDTO {
  id?: number;
  userName: string;
  rating: number;
  comment: string;
  replyContent?: string;
  createdAt: string | Date;
  mediaUrls?: string[];
  hidden: boolean; // 🔥 Đã thêm thuộc tính quản lý ẩn/hiện đồng bộ Backend
}

@Injectable({
  providedIn: 'root'
})
export class ReviewService {
  private apiUrl = 'http://localhost:8080/api/reviews';

  constructor(private http: HttpClient) {}

  // --- API DÀNH CHO CUSTOMER ---
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
}