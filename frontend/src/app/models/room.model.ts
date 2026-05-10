export interface RoomType {
  id: number;
  typeName: string;
  basePrice: number;
  maxOccupancy: number;
  images: string[]; // Từ bảng room_type_images
  hotelName: string; // Join từ bảng hotels
}

export interface BookingRequest {
  userId: number;
  checkInDate: string;
  checkOutDate: string;
  roomIds: number[];
  totalPrice: number;
}
export interface RoomResponseDTO {
  roomId: number;
  roomNumber: string;
  typeName: string;
  price: number;
  hotelName: string;
}