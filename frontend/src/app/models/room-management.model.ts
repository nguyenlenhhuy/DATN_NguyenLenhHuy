// Enum khớp hoàn toàn với RoomStatus.java trong hệ thống của bạn
export enum RoomStatus {
  AVAILABLE = 'AVAILABLE',
  OCCUPIED = 'OCCUPIED',
  CLEANING = 'DIRTY', // SỬA TẠI ĐÂY: Đồng bộ giá trị mapping khớp với từ khóa của Java DB
  MAINTENANCE = 'MAINTENANCE'
}
// Model soi chiếu cấu trúc DTO nhận vào của Phòng
export interface RoomImageRequest {
  imageUrl: string;
  isPrimary: boolean;
}

export interface RoomRequest {
  hotelId: number;
  roomTypeId: number;
  roomNumber: string;
  floor: number;
  images?: RoomImageRequest[];
}

// Model soi chiếu cấu trúc DTO nhận vào của Loại phòng
export interface RoomTypeImageRequest {
  imageUrl: string;
  isPrimary: boolean;
}

export interface RoomTypeRequest {
  hotelId: number;
  typeName: string;
  basePrice: number;
  maxOccupancy: number;
  isFeatured: boolean;
  images?: RoomTypeImageRequest[];
}

// Model khớp 100% với Design Pattern Builder RoomResponseDTO trả về từ API sơ đồ phòng
export interface RoomResponseDTO {
  roomId: number;
  roomNumber: string;
  floor: number;
  status: RoomStatus;
  typeName: string;
  price: number;
  hotelName: string;
  appliedPromotions?: Array<{
    id: number;
    code: string;
    discountPercentage: number;
    endDate: string;
  }>;
}

// Cấu trúc thực thể Loại phòng (Trả về khi tạo mới thành công)
export interface RoomType {
  id: number;
  typeName: string;
  basePrice: number;
  maxOccupancy: number;
  isFeatured: boolean;
}
export interface RoomResponseDTO {
  roomId: number;
  roomNumber: string;
  floor: number;
  status: RoomStatus; // SỬA TẠI ĐÂY: Đổi từ string sang RoomStatus để đồng nhất hệ thống
  typeName: string;
  price: number;
  hotelName: string;
  imageUrl?: string;        
  albumImages?: string[];   
}
export interface CreateBookingRequest {
  roomId: number;
  customerName: string;
  customerPhone: string;
  checkInDate: string;  // Định dạng YYYY-MM-DD
  checkOutDate: string; // Định dạng YYYY-MM-DD
  appliedCode?: string; // Mã giảm giá (nếu có)
}

export interface QuickBookingDetailDTO {
  bookingId: number;
  customerName: string;
  customerPhone: string;
  checkInDate: string;
  checkOutDate: string;
  finalAmount: number;
  paymentStatus: 'PENDING' | 'PAID' | 'CANCELLED';
}