import { Component, OnInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { RoomService } from '../../services/room.service';
import { FavoriteService } from '../../services/favorite.service';
import { RoomHoldService } from '../../services/room-hold.service';
import { RoomResponseDTO, RoomSearchRequest } from '../../models/room.model';
import { HeaderComponent } from '../../components/header/header.component';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-rooms',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, HeaderComponent],
  templateUrl: './rooms.component.html',
  styleUrl: './rooms.component.scss'
})
export class RoomsComponent implements OnInit {
  rooms: any[] = [];
  roomTypes: any[] = [];
  floors: string[] = [];
  selectedFloor: string = 'ALL';
  isLoading = false;
  heldRoomIds: number[] = [];
  sortOption: string = 'default';

  // === BIẾN QUẢN LÝ KHOẢNG GIÁ (HỘP CHỌN + ĐIỀN TAY) ===
  minPrice: number = 0;
  maxPrice: number = 10000000;
  maxSliderLimit: number = 10000000;
  selectedPricePreset: string = '0-10000000'; // Quản lý giá trị của thẻ <select>
  onlyAvailable: boolean = false;

  urlKeyword: string = '';

  searchRequest: RoomSearchRequest = {
    checkIn: null,
    checkOut: null,
    guestCount: null,
    typeName: ''
  };

  constructor(
    private roomService: RoomService,
    private favoriteService: FavoriteService,
    private roomHoldService: RoomHoldService,
    private router: Router,
    private route: ActivatedRoute,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadRoomTypes();
    this.loadHeldRooms();

    this.route.queryParams.subscribe(params => {
      this.urlKeyword = params['q'] || '';

      if (params['typeName']) {
        this.searchRequest.typeName = params['typeName'];
      } else if (this.urlKeyword) {
        this.searchRequest.typeName = this.urlKeyword;
      }

      this.searchRequest.checkIn  = params['checkIn']  || null;
      this.searchRequest.checkOut = params['checkOut'] || null;
      this.searchRequest.guestCount = params['guestCount'] ? Number(params['guestCount']) : null;

      this.minPrice = params['minPrice'] ? Number(params['minPrice']) : 0;
      this.maxPrice = params['maxPrice'] ? Number(params['maxPrice']) : 10000000;

      const presetStr = `${this.minPrice}-${this.maxPrice}`;
      const validPresets = ['0-10000000', '0-1000000', '1000000-3000000', '3000000-5000000', '5000000-10000000'];
      this.selectedPricePreset = validPresets.includes(presetStr) ? presetStr : 'custom';

      // Auto-search nếu URL có bất kỳ filter nào; không thì tải toàn bộ phòng
      const hasFilters = params['guestCount'] || params['checkIn'] || params['checkOut']
                      || params['typeName']   || params['q']
                      || params['minPrice']   || params['maxPrice'];
      if (hasFilters) {
        this.searchRooms();
      } else {
        this.loadAllRooms();
      }
    });
  }

  // === LOGIC XỬ LÝ GIÁ KHI ĐỔI HỘP CHỌN ===
  onPresetChange(): void {
    if (this.selectedPricePreset === 'custom') return;
    
    const parts = this.selectedPricePreset.split('-');
    if (parts.length === 2) {
      this.minPrice = Number(parts[0]);
      this.maxPrice = Number(parts[1]);
    }
  }

  // === LOGIC XỬ LÝ KHI NGƯỜI DÙNG TỰ GÕ SỐ ===
  onManualPriceChange(): void {
    if (this.minPrice > this.maxPrice) {
      const temp = this.minPrice;
      this.minPrice = this.maxPrice;
      this.maxPrice = temp;
    }
    this.selectedPricePreset = 'custom'; 
  }

  loadAllRooms(): void {
    this.isLoading = true;
    this.roomService.getAllRooms().subscribe({
      next: (data: any[]) => {
        const mappedData = this.mapIncomingPromotions(data);
        this.syncFavorites(mappedData);
        this.extractFloors(mappedData);
        this.isLoading = false;
      },
      error: (err: any) => { 
        console.error('Lỗi khi tải danh sách phòng:', err);
        this.isLoading = false;
      }
    });
  }

  searchRooms(): void {
    // Nếu ngày trả nhỏ hơn ngày nhận -> Báo lỗi
    if (this.searchRequest.checkIn && this.searchRequest.checkOut) {
      if (new Date(this.searchRequest.checkOut) <= new Date(this.searchRequest.checkIn)) {
        alert("Ngày trả phòng phải sau ngày nhận phòng. Vui lòng kiểm tra lại!");
        return;
      }
    }

    this.isLoading = true;
    this.roomService.searchRooms(this.searchRequest).subscribe({
      next: (data: any[]) => {
        const mappedData = this.mapIncomingPromotions(data);
        this.syncFavorites(mappedData);
        this.extractFloors(mappedData);
        this.isLoading = false;
        
        // Cập nhật lại URL để có thể chia sẻ link
        const queryParams: any = {
          typeName: this.searchRequest.typeName,
          checkIn: this.searchRequest.checkIn,
          checkOut: this.searchRequest.checkOut,
          guestCount: this.searchRequest.guestCount
        };
        if (this.minPrice > 0) queryParams.minPrice = this.minPrice;
        if (this.maxPrice < this.maxSliderLimit) queryParams.maxPrice = this.maxPrice;

        Object.keys(queryParams).forEach(key => {
          if (!queryParams[key]) delete queryParams[key];
        });

        this.router.navigate([], { queryParams: queryParams });
      },
      error: (err: any) => { 
        console.error('Lỗi khi tìm kiếm phòng:', err);
        this.isLoading = false;
      }
    });
  }

  private extractFloors(data: any[]): void {
    const floorSet = new Set<string>();
    data.forEach(room => {
      if (room.roomNumber) {
        const numStr = room.roomNumber.toString().trim();
        const floorStr = numStr.length >= 4 ? numStr.substring(0, numStr.length - 2) : numStr.charAt(0);
        floorSet.add(floorStr);
      }
    });
    this.floors = Array.from(floorSet).sort((a, b) => parseInt(a) - parseInt(b));
  }

  // Logic lọc kết hợp (Tầng + Loại phòng + Khoảng giá + Sức chứa) + sort
  get filteredRooms(): any[] {
    const filtered = this.rooms.filter(room => {
      const numStr = room.roomNumber.toString().trim();
      const floorStr = numStr.length >= 4 ? numStr.substring(0, numStr.length - 2) : numStr.charAt(0);
      const matchFloor = this.selectedFloor === 'ALL' || floorStr === this.selectedFloor;

      const keywordToSearch = this.searchRequest.typeName || this.urlKeyword;
      const matchKeyword = !keywordToSearch ||
        room.roomNumber.toString().toLowerCase().includes(keywordToSearch.toLowerCase()) ||
        (room.typeName && room.typeName.toLowerCase().includes(keywordToSearch.toLowerCase()));

      const currentPrice = this.getEffectivePrice(room);
      const safeMin = this.minPrice || 0;
      const safeMax = this.maxPrice || 999999999;
      const matchPrice = currentPrice >= safeMin && currentPrice <= safeMax;

      const guestFilter = this.searchRequest.guestCount ? Number(this.searchRequest.guestCount) : null;
      const matchGuests = !guestFilter || !room.maxGuests || room.maxGuests >= guestFilter;

      const matchAvailable = !this.onlyAvailable || room.status === 'AVAILABLE';

      return matchFloor && matchKeyword && matchPrice && matchGuests && matchAvailable;
    });

    switch (this.sortOption) {
      case 'price-asc':  return [...filtered].sort((a, b) => this.getEffectivePrice(a) - this.getEffectivePrice(b));
      case 'price-desc': return [...filtered].sort((a, b) => this.getEffectivePrice(b) - this.getEffectivePrice(a));
      case 'rating-desc': return [...filtered].sort((a, b) => (b.averageRating || 0) - (a.averageRating || 0));
      default: return filtered;
    }
  }

  getEffectivePrice(room: any): number {
    const price = Number(room.price) || 0;
    return room.appliedPromotion
      ? price * (1 - room.appliedPromotion.discountPercentage / 100)
      : price;
  }

  get activeFilterChips(): { label: string; key: string }[] {
    const chips: { label: string; key: string }[] = [];
    if (this.searchRequest.typeName)   chips.push({ label: `Loại: ${this.searchRequest.typeName}`, key: 'typeName' });
    if (this.searchRequest.checkIn)    chips.push({ label: `Nhận: ${this.searchRequest.checkIn}`, key: 'checkIn' });
    if (this.searchRequest.checkOut)   chips.push({ label: `Trả: ${this.searchRequest.checkOut}`, key: 'checkOut' });
    if (this.searchRequest.guestCount) chips.push({ label: `${this.searchRequest.guestCount} khách`, key: 'guestCount' });
    if (this.minPrice > 0)             chips.push({ label: `Từ ${this.formatPrice(this.minPrice)}`, key: 'minPrice' });
    if (this.maxPrice < 10000000)      chips.push({ label: `Đến ${this.formatPrice(this.maxPrice)}`, key: 'maxPrice' });
    if (this.onlyAvailable)            chips.push({ label: 'Chỉ phòng trống', key: 'onlyAvailable' });
    return chips;
  }

  removeFilter(key: string): void {
    if (key === 'typeName')    this.searchRequest.typeName = '';
    else if (key === 'checkIn')     this.searchRequest.checkIn = null;
    else if (key === 'checkOut')    this.searchRequest.checkOut = null;
    else if (key === 'guestCount')  this.searchRequest.guestCount = null;
    else if (key === 'minPrice')       this.minPrice = 0;
    else if (key === 'maxPrice')       this.maxPrice = 10000000;
    else if (key === 'onlyAvailable')  this.onlyAvailable = false;
    const hasFilters = this.searchRequest.typeName || this.searchRequest.checkIn ||
                       this.searchRequest.checkOut || this.searchRequest.guestCount ||
                       this.minPrice > 0 || this.maxPrice < 10000000 || this.onlyAvailable;
    if (hasFilters) this.searchRooms();
    else { this.router.navigate(['/rooms']); this.loadAllRooms(); }
  }

  get roomsGroupedByFloor(): { floor: string, roomList: any[] }[] {
    const groups: { [key: string]: any[] } = {};
    
    this.filteredRooms.forEach(room => {
      const numStr = room.roomNumber.toString().trim();
      const floorStr = numStr.length >= 4 ? numStr.substring(0, numStr.length - 2) : numStr.charAt(0);
      
      if (!groups[floorStr]) {
        groups[floorStr] = [];
      }
      groups[floorStr].push(room);
    });

    return Object.keys(groups)
      .sort((a, b) => parseInt(a) - parseInt(b))
      .map(floor => ({
        floor: floor,
        roomList: groups[floor]
      }));
  }

  selectFloor(floor: string): void {
    this.selectedFloor = floor;
  }

  private syncFavorites(data: any[]): void {
    // 🔥 CHỐT CHẶN BẢO MẬT: Khách vãng lai không được gọi API
    if (!this.authService.isLoggedIn()) {
      // Gán thẳng trạng thái 'chưa yêu thích' (false) cho tất cả các phòng
      this.rooms = data.map(room => ({
        ...room,
        isFavorite: false
      }));
      return; // Cắt luồng ở đây
    }

    // Luồng cũ (Chỉ chạy khi đã đăng nhập)
    this.favoriteService.getMyFavorites().subscribe({
      next: (favorites: any[]) => {
        const favoriteRoomIds = favorites.map(fav => {
          if (fav && fav.room) {
            return fav.room.roomId || fav.room.id;
          }
          return fav ? (fav.roomId || fav.id) : null;
        }).filter(id => id !== null);

        this.rooms = data.map(room => {
          const currentId = room.roomId || room.id;
          return {
            ...room,
            isFavorite: favoriteRoomIds.includes(currentId)
          };
        });
      },
      error: (err) => {
        console.error('Lỗi đồng bộ danh sách phòng yêu thích:', err);
        this.rooms = data; 
      }
    });
  }

  loadRoomTypes(): void {
    this.roomService.getRoomTypes().subscribe({
      next: (data) => { this.roomTypes = data; },
      error: (err: any) => { console.error('Lỗi khi tải loại phòng:', err); }
    });
  }

  loadHeldRooms(): void {
    this.roomHoldService.getHeldRoomIds().subscribe(ids => {
      this.heldRoomIds = ids.map(id => Number(id));
    });
  }

  isRoomHeld(roomId: number): boolean {
    return this.heldRoomIds.includes(roomId);
  }

  resetSearch(): void {
    this.searchRequest = { checkIn: null, checkOut: null, guestCount: null, typeName: '' };
    this.selectedFloor = 'ALL';
    this.minPrice = 0;
    this.maxPrice = 10000000;
    this.selectedPricePreset = '0-10000000';
    this.onlyAvailable = false;
    this.urlKeyword = '';
    this.router.navigate(['/rooms']);
    this.loadAllRooms();
  }

  formatPrice(price: number | undefined): string {
    if (price === undefined || price === null) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  }

  toggleFavorite(event: Event, room: any): void {
    if (event) {
      event.preventDefault();
      event.stopPropagation(); 
    }

    // 🔥 KIỂM TRA TRẠNG THÁI ĐĂNG NHẬP
    if (!this.authService.isLoggedIn()) {
      alert('Vui lòng đăng nhập để lưu phòng vào danh sách yêu thích!');
      // Tùy chọn: Đẩy về trang đăng nhập và lưu lại URL hiện tại
      this.router.navigate(['/login'], { 
        queryParams: { returnUrl: this.router.url }
      });
      return; // Dừng hàm ngay lập tức
    }

    if (!room || (!room.roomId && !room.id)) {
      alert('Lỗi dữ liệu cấu trúc phòng!');
      return;
    }

    const targetRoomId = room.roomId || room.id;

    this.favoriteService.toggleFavorite(targetRoomId).subscribe({
      next: (res) => {
        room.isFavorite = !room.isFavorite; 
      },
      error: (err) => { 
        alert("Có lỗi xảy ra khi thực hiện lưu trạng thái yêu thích!");
      }
    });
  }

// Cập nhật hàm này vào rooms.component.ts
  // Cập nhật hàm này để xử lý đúng mảng albumImages (List<String>)
  getRoomImages(room: any): string[] {
    // 1. Nếu có album ảnh (mảng link), dùng nó
    if (room.albumImages && Array.isArray(room.albumImages) && room.albumImages.length > 0) {
      return room.albumImages;
    }
    // 2. Nếu không có album, tạo mảng chứa ảnh đại diện (để HTML vẫn chạy được)
    return room.imageUrl ? [room.imageUrl] : ['https://images.unsplash.com/photo-1582719478250-c89404bb8a0e?q=80&w=1000'];
  }

  // Hàm này chỉ dùng cho trường hợp bạn cần lấy 1 ảnh duy nhất để làm bìa
  getRoomImage(room: any): string {
    const images = this.getRoomImages(room);
    return images[0];
  } 

  viewRoomDetail(roomId: number, event: Event): void {
    event.stopPropagation();
    
    const targetRoom = this.rooms.find(r => r.roomId === roomId);
    
    if (targetRoom && targetRoom.appliedPromotions && targetRoom.appliedPromotions.length > 0) {
      this.router.navigate(['/rooms', roomId], { 
        queryParams: { promoCode: targetRoom.appliedPromotions[0].code } 
      });
    } else {
      this.router.navigate(['/rooms', roomId]);
    }
  }

  private mapIncomingPromotions(data: any[]): any[] {
    if (!data) return [];
    return data.map(room => {
      let promo = room.appliedPromotion;

      if (!promo && room.appliedPromotions && room.appliedPromotions.length > 0) {
        const activePromo = room.appliedPromotions.find((p: any) => p.isActive !== false);
        if (activePromo) {
          promo = {
            id: activePromo.id,
            code: activePromo.code,
            discountPercentage: activePromo.discountPercentage
          };
        }
      }

      return {
        ...room,
        appliedPromotion: promo 
      };
    });
  }
  
}