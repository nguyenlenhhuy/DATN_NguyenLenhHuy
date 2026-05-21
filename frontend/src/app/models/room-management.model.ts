// Enum khớp hoàn toàn với RoomStatus.java trong hệ thống của bạn
export enum RoomStatus {
  AVAILABLE = 'AVAILABLE',
  OCCUPIED = 'OCCUPIED',
  CLEANING = 'CLEANING',
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