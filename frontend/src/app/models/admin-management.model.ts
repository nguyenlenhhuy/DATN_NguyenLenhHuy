// Model Quản lý Khuyến mãi
export interface Promotion {
  id?: number;
  code: string;
  discountPercentage: number; // Ví dụ: 10 nghĩa là giảm 10%
  startDate: string;
  endDate: string;
  isActive: boolean;
}

// Model Quản lý Đặt phòng liên kết dữ liệu PayOS
export interface BookingResponseDTO {
  bookingId: number;
  roomNumber: string;
  customerName: string;
  customerPhone: string;
  checkInDate: string;
  checkInTime?: string;
  checkOutDate: string;
  originalPrice: number;       // Giá phòng gốc
  discountAmount: number;      // Số tiền được giảm
  finalAmount: number;         // Số tiền thực tế khách phải trả
  appliedCode?: string;        // Mã khuyến mãi đã dùng (nếu có)
  paymentStatus: 'PENDING' | 'PAID' | 'CANCELLED'; // Trạng thái đồng bộ từ Webhook PayOS
  payosOrderCode?: number;     // Mã đơn hàng định danh của PayOS
}