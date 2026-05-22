// --- PHÂN HỆ QUẢN LÝ CHƯƠNG TRÌNH KHUYẾN MÃI (PROMOTIONS) ---
export interface PromotionRequestDTO {
  code: string;
  discountPercentage: number;
  startDate: string; 
  endDate: string;
}

export interface PromotionResponseDTO {
  id: number;
  code: string;
  discountPercentage: number;
  startDate: string;
  endDate: string;
  isActive: boolean;
}

// --- PHÂN HỆ NGHIỆP VỤ ĐẶT PHÒNG KHÁCH SẠN (BOOKINGS) ---
export interface WalkInBookingRequestDTO {
  roomNumber: string;
  customerName: string;
  customerPhone: string;
  checkInDate: string;
  checkOutDate: string;
  appliedCode: string; // Áp dụng mã Voucher tại quầy cho đơn Walk-in
}

export interface BookingResponseDTO {
  bookingId: number;
  roomNumber: string;
  customerName: string;
  customerPhone: string;
  checkInDate: string;
  checkOutDate: string;
  originalPrice: number;    
  discountAmount: number;   
  finalAmount: number;      // Số tiền thu ngân cuối cùng khớp chuẩn bảng invoices
  appliedCode?: string;     
  paymentStatus: 'UNPAID' | 'PAID' | 'REFUNDED'; // Đồng bộ cấu trúc bảng invoices
  paymentMethod: 'VNPAY' | 'CASH' | 'TRANSFER';  // Đồng bộ phương thức thanh toán bảng invoices
  bookingStatus: 'PENDING' | 'CONFIRMED' | 'CHECK_IN' | 'CHECK_OUT' | 'CANCELLED'; // Khớp trạng thái bảng bookings
}