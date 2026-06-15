export interface PromotionDTO {
  id: number;
  code: string;
  discountPercentage: number;
  startDate: string;
  endDate: string;
}

export interface RoomResponseDTO {
  roomId: number;
  roomNumber: string;
  typeName: string;
  price: any;
  hotelName: string;
  imageUrl?: string;
  imageUrls?: string[]; 
  isFavorite?: boolean;
  floor: number | null;
  status: string | null;
  maxGuests?: number;
  rating?: number;
  hotelAddress?: string;
  hotelStarRating?: number;
  averageRating?: number;
  reviewCount?: number;

  // SỬA Ở ĐÂY: Dùng mảng appliedPromotions thay vì promotionCode
  appliedPromotions?: PromotionDTO[]; 
}
export interface RoomSearchRequest {
  checkIn: string | null;
  checkOut: string | null;
  guestCount: number | null;
  typeName: string | null;
  minPrice?: number | null;
  maxPrice?: number | null;
}