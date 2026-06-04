import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

// Định nghĩa DTO gửi lên (Khớp 100% với Backend)
export interface ReviewRequestDTO {
  bookingId: number;
  rating: number;
  comment: string;
  mediaUrls?: string[];
}

@Injectable({
  providedIn: 'root'
})
export class ReviewService {
  // Thay đổi URL này theo domain thực tế của backend
  private apiUrl = 'http://localhost:8080/api/reviews';

  constructor(private http: HttpClient) {}

  submitReview(data: ReviewRequestDTO): Observable<any> {
    return this.http.post(`${this.apiUrl}/submit`, data).pipe(
      catchError(this.handleError)
    );
  }

  // Hàm xử lý lỗi chung để báo về Component
  private handleError(error: HttpErrorResponse) {
    let errorMessage = 'Đã có lỗi xảy ra, vui lòng thử lại sau.';
    
    // Nếu Backend trả về message lỗi cụ thể (ví dụ: "Bạn chỉ có thể đánh giá sau khi check-out")
    if (error.error && typeof error.error === 'string') {
      errorMessage = error.error;
    } else if (error.error && error.error.message) {
      errorMessage = error.error.message;
    }
    
    return throwError(() => new Error(errorMessage));
  }
}