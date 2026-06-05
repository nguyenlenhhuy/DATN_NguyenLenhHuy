import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReviewService, ReviewResponseDTO } from '../../services/review.service';
import { RoomService } from '../../services/room.service'; 

@Component({
  selector: 'app-review-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './review-management.component.html'
})
export class ReviewManagementComponent implements OnInit {
  reviews: ReviewResponseDTO[] = [];
  rooms: any[] = []; 
  selectedRoomId: string = ''; // Lưu giữ ID dưới dạng chuỗi thu từ template select HTML

  // Quản lý trạng thái khung phản hồi
  replyInputs: { [key: number]: string } = {};
  activeReplyId: number | null = null; 

  constructor(
    private reviewService: ReviewService,
    private roomService: RoomService 
  ) {}

  ngOnInit(): void {
    this.loadAllRooms(); 
    this.loadAllReviewsForAdmin(); 
  }

  // Nạp danh sách tất cả phòng vật lý làm bộ lọc
  loadAllRooms(): void {
    this.roomService.getAllRooms().subscribe({ 
      next: (res: any) => {
        // In log ra tab Console (F12) xem phòng có thuộc tính "id" hay "roomId", "roomNumber" để gài HTML cho đúng
        console.log('Dữ liệu phòng nạp vào bộ lọc:', res);
        this.rooms = Array.isArray(res) ? res : (res.content || []);
      },
      error: (err: any) => console.error('Lỗi tải danh sách phòng:', err)
    });
  }

  // 🔥 ĐÃ ĐIỀU CHỈNH: Sửa điều kiện bẫy lỗi để nhận diện chính xác khi nào lọc, khi nào lấy TẤT CẢ
 loadAllReviewsForAdmin(): void {
    const roomIdClean = this.selectedRoomId ? this.selectedRoomId.trim() : '';

    // 🔥 SỬA ĐIỀU KIỆN: Kiểm tra rỗng VÀ phải chắc chắn chuỗi đó KHÔNG PHẢI là "NaN" hoặc "undefined"
    if (roomIdClean !== '' && roomIdClean !== 'NaN' && roomIdClean !== 'undefined' && !isNaN(Number(roomIdClean))) {
      console.log('Đang kích hoạt lọc đánh giá cho Room ID hợp lệ:', roomIdClean);
      
      this.reviewService.getReviewsByRoomId(Number(roomIdClean)).subscribe({
        next: (data: ReviewResponseDTO[]) => {
          this.reviews = data;
        },
        error: (err: any) => console.error('Lỗi tải review theo phòng:', err)
      });
    } else {
      console.log('ID không hợp lệ hoặc chọn tất cả -> Nạp toàn bộ đánh giá tổng quan');
      
      this.reviewService.findAllForAdmin(0, 100).subscribe({
        next: (res: any) => {
          this.reviews = res.content;
        },
        error: (err: any) => console.error('Lỗi tải tất cả đánh giá:', err)
      });
    }
  }

  // Sự kiện kích hoạt khi Admin thay đổi lựa chọn phòng trên Dropdown
  onRoomFilterChange(): void {
    this.activeReplyId = null; 
    this.loadAllReviewsForAdmin();
  }

  // Xử lý bấm nút Ẩn/Hiện trên Table
  onToggleVisibility(review: ReviewResponseDTO): void {
    if (confirm(`Bạn có chắc chắn muốn thay đổi trạng thái hiển thị của bình luận này?`)) {
      this.reviewService.toggleReviewVisibility(review.id!).subscribe({
        next: (updatedReview) => {
          review.hidden = updatedReview.hidden;
          alert(updatedReview.hidden ? '🚫 Đã ẩn bình luận thành công!' : '✅ Đã hiển thị lại bình luận công khai!');
        },
        error: (err: any) => alert('Lỗi hệ thống: Không thể thao tác ẩn/hiện.')
      });
    }
  }

  // Mở hoặc Đóng khung nhập liệu phản hồi
  toggleReplyForm(reviewId: number, currentReply?: string): void {
    this.activeReplyId = this.activeReplyId === reviewId ? null : reviewId;
    this.replyInputs[reviewId] = currentReply || ''; 
  }

  // Gửi nội dung phản hồi lên Backend
  submitAdminReply(review: ReviewResponseDTO): void {
    const content = this.replyInputs[review.id!]?.trim();
    if (!content) {
      alert('Vui lòng điền nội dung phản hồi trước khi gửi!');
      return;
    }

    this.reviewService.replyReview(review.id!, content).subscribe({
      next: (updatedReview) => {
        review.replyContent = updatedReview.replyContent;
        this.activeReplyId = null; 
        alert('🏨 Đã gửi phản hồi chính thức của khách sạn thành công!');
      },
      error: (err: any) => alert('Hệ thống: Không thể gửi phản hồi lúc này.')
    });
  }
}