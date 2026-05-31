export interface RoomResponseDTO {
  roomId: number;
  roomNumber: string;
  typeName: string;
  price: number;
  hotelName: string;
  imageUrl?: string;
  imageUrls?: string[]; // Thêm trường này để chứa danh sách ảnh từ Admin
  isFavorite?: boolean;
  floor: number | null;
  status: string | null;
  maxGuests?: number;
}

export interface RoomSearchRequest {
  checkIn: string | null;
  checkOut: string | null;
  guestCount: number | null;
  typeName: string | null;
  minPrice?: number | null;
  maxPrice?: number | null;
}