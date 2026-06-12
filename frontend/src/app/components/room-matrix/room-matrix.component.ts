import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RoomManagementService } from '../../services/room-management.service';
import { RoomResponseDTO, RoomStatus, RoomRequest, RoomType } from '../../models/room-management.model';

@Component({
  selector: 'app-room-matrix',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './room-matrix.component.html',
  styleUrls: ['./room-matrix.component.scss']
})
export class RoomMatrixComponent implements OnInit {
  rooms: RoomResponseDTO[] = [];
  filteredRooms: RoomResponseDTO[] = [];
  roomTypes: RoomType[] = [];
  
  promotions: any[] = []; 
  selectedPromoIdForRoom: number | null = null; 
  maxDiscountPercentage: number = 0;
  calculatedFinalPrice: number = 0;
  
  currentHotelId: number = 1; 
  userRole: string = 'ADMIN'; 
  
  isModalOpen: boolean = false;      
  isEditModalOpen: boolean = false;  
  isTypeModalOpen: boolean = false; 
  activeTab: 'single' | 'floor' = 'single'; 

  selectedStatusFilter: string = 'ALL';
  selectedFloorFilter: string = 'ALL'; 
  floors: number[] = [];
  statusOptions = Object.values(RoomStatus);

  imageItems: { source: 'local' | 'web', url: string, isPrimary: boolean }[] = [];
  currentImgIndex: number = 0; 

  newTypeData = {
    hotelId: this.currentHotelId,
    typeName: '',
    basePrice: undefined as number | undefined,
    maxOccupancy: 2,
    isFeatured: false
  };

  newRoom: RoomRequest & { customPrice?: number } = {
    hotelId: this.currentHotelId,
    roomTypeId: 1,
    roomNumber: '101',
    floor: 1,
    customPrice: undefined,
    images: []
  };

  floorBatchConfig = {
    floor: 1,
    roomTypeId: 1,
    startRoomNumber: 101,
    totalRooms: 10,
    applyCustomPrice: undefined
  };

  selectedRoomId!: number;
  editRoom: RoomRequest & { customPrice?: number } = {
    hotelId: this.currentHotelId,
    roomTypeId: 1,
    roomNumber: '',
    floor: 1,
    customPrice: undefined,
    images: []
  };

  constructor(private roomService: RoomManagementService) {}

  ngOnInit(): void {
    this.userRole = localStorage.getItem('role') || 'ADMIN';
    this.loadRoomMatrix();
    if (this.userRole === 'ADMIN') {
      this.loadRoomTypes();
      this.loadAllPromotions();
    }
  }

  loadRoomMatrix(): void {
    this.roomService.getRoomsByHotel(this.currentHotelId).subscribe({
      next: (data) => {
        this.rooms = data;
        this.floors = Array.from(new Set(data.map(r => r.floor))).sort((a, b) => a - b);
        this.applyFilters();
      },
      error: (err: any) => alert('Thông báo lỗi hệ thống: ' + (err.error?.message || err.message))
    });
  }

  loadRoomTypes(): void {
    this.roomService.getRoomTypes().subscribe({
      next: (types: RoomType[]) => {
        this.roomTypes = types;
        if (this.roomTypes.length > 0) {
          this.newRoom.roomTypeId = this.roomTypes[0].id;
          this.floorBatchConfig.roomTypeId = this.roomTypes[0].id;
        }
      },
      error: (err: any) => console.error('Không thể nạp danh sách loại phòng:', err)
    });
  }

  loadAllPromotions(): void {
    this.roomService.getActivePromotions().subscribe({
      next: (res: any[]) => {
        this.promotions = res;
      },
      error: (err: any) => console.error('Hệ thống: Không thể nạp danh sách khuyến mãi:', err)
    });
  }

  // 🔥 SỬA LỖI LOGIC ÁP MÃ KHUYẾN MÃI VÀ ĐỒNG BỘ UI
  onApplyPromotion(): void {
    if (!this.selectedRoomId) {
      alert('Vui lòng chọn phòng trước khi thực hiện thao tác!');
      return;
    }

    // Nếu chọn null (bỏ áp dụng mã) thì gửi null, ngược lại ép kiểu số rõ ràng
    const promoIdToSend = this.selectedPromoIdForRoom ? Number(this.selectedPromoIdForRoom) : null;

    this.roomService.applyPromotionToRoom(this.selectedRoomId, promoIdToSend!).subscribe({
      next: (res: any) => {
        alert(res.message || 'Gán mã giảm giá vào phòng thành công!');
        
        // Gọi lại danh sách phòng mới nhất từ Database sau khi dữ liệu DB đã đổi thay
        this.roomService.getRoomsByHotel(this.currentHotelId).subscribe({
          next: (data) => {
            this.rooms = data;
            
            // Ép bộ lọc chạy lại để render lập tức giá tiền mới ra thẻ phòng nền bên ngoài
            this.applyFilters();

            // Tìm kiếm thông tin phòng hiện tại vừa được gán mã thành công
            const updatedRoom = this.rooms.find(r => r.roomId === this.selectedRoomId);
            
            if (updatedRoom) {
              this.editRoom.customPrice = updatedRoom.price;

              if (updatedRoom.appliedPromotions && updatedRoom.appliedPromotions.length > 0) {
                // Bốc chương trình ưu đãi lớn nhất áp dụng lên phòng
                const bestPromo = updatedRoom.appliedPromotions.reduce((max, p) => 
                  p.discountPercentage > max.discountPercentage ? p : max, updatedRoom.appliedPromotions[0]
                );
                
                // Đồng bộ biến hiển thị khung giảm giá màu hồng trên Modal ngay lập tức
                this.maxDiscountPercentage = bestPromo.discountPercentage;
                this.calculatedFinalPrice = updatedRoom.price * (1 - this.maxDiscountPercentage / 100);
                this.selectedPromoIdForRoom = Number(bestPromo.id);
              } else {
                // Reset trạng thái nếu phòng không có chương trình ưu đãi nào
                this.maxDiscountPercentage = 0;
                this.calculatedFinalPrice = updatedRoom.price;
                this.selectedPromoIdForRoom = null;
              }
            }
          },
          error: (refreshErr) => console.error('Lỗi đồng bộ danh sách phòng:', refreshErr)
        });
      },
      error: (err: any) => {
        console.error(err);
        alert('Có lỗi xảy ra khi áp dụng khuyến mãi: ' + (err.error?.message || err.message));
      }
    });
  }

  openTypeModal(): void {
    this.isTypeModalOpen = true;
    this.newTypeData = { hotelId: this.currentHotelId, typeName: '', basePrice: undefined, maxOccupancy: 2, isFeatured: false };
  }

  closeTypeModal(): void {
    this.isTypeModalOpen = false;
  }

  onSaveRoomType(): void {
    if (!this.newTypeData.typeName.trim() || !this.newTypeData.basePrice) {
      alert('Vui lòng điền tên loại phòng và giá tiền gốc niêm yết!');
      return;
    }
    this.newTypeData.hotelId = this.currentHotelId;
    this.roomService.createRoomType(this.newTypeData).subscribe({
      next: (savedType: any) => {
        alert(`Khởi tạo phân loại phòng "${savedType.typeName}" thành công!`);
        this.loadRoomTypes();
        this.newTypeData = { hotelId: this.currentHotelId, typeName: '', basePrice: undefined, maxOccupancy: 2, isFeatured: false };
      },
      error: (err: any) => alert('Thêm phân loại thất bại: ' + (err.error?.message || err.message))
    });
  }

  onDeleteRoomType(typeId: number): void {
    if (confirm('Cảnh báo hệ thống: Nếu xóa loại phòng này, các phòng vật lý liên đới thuộc loại này sẽ bị ảnh hưởng. Tiếp tục?')) {
      this.roomService.deleteRoomType(typeId).subscribe({
        next: (res: any) => {
          alert(res.message || 'Xóa loại phòng khỏi hệ thống thành công!');
          this.loadRoomTypes();
          this.loadRoomMatrix();
        },
        error: (err: any) => alert('Không thể xóa loại phòng: ' + (err.error?.message || err.message))
      });
    }
  }

  onFloorChange(mode: 'single' | 'batch'): void {
    if (mode === 'single') {
      const currentFloor = this.newRoom.floor;
      if (currentFloor !== null && currentFloor !== undefined && currentFloor >= 0) {
        this.newRoom.roomNumber = (currentFloor * 100 + 1).toString();
      }
    } else {
      const batchFloor = this.floorBatchConfig.floor;
      if (batchFloor !== null && batchFloor !== undefined && batchFloor >= 0) {
        this.floorBatchConfig.startRoomNumber = batchFloor * 100 + 1;
      }
    }
  }

  nextImage(event: MouseEvent): void {
    event.stopPropagation();
    if (this.imageItems.length > 0) {
      this.currentImgIndex = (this.currentImgIndex + 1) % this.imageItems.length;
    }
  }

  prevImage(event: MouseEvent): void {
    event.stopPropagation();
    if (this.imageItems.length > 0) {
      this.currentImgIndex = (this.currentImgIndex - 1 + this.imageItems.length) % this.imageItems.length;
    }
  }

  setAsPrimaryFromSlide(event: MouseEvent): void {
    event.stopPropagation();
    if (this.imageItems.length > 0) {
      this.imageItems.forEach((img, idx) => {
        img.isPrimary = idx === this.currentImgIndex;
      });
      alert(`Đã chọn ảnh số ${this.currentImgIndex + 1} làm hình đại diện chính thành công!`);
    }
  }

  setPrimaryImage(selectedIndex: number): void {
    this.imageItems.forEach((img, idx) => {
      img.isPrimary = idx === selectedIndex;
    });
    this.currentImgIndex = selectedIndex;
  }

  addImageRow(type: 'local' | 'web'): void {
    const isFirst = this.imageItems.length === 0;
    this.imageItems.push({ source: type, url: '', isPrimary: isFirst });
    this.currentImgIndex = this.imageItems.length - 1;
  }

  onFileSelected(event: Event, index: number): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      const reader = new FileReader();
      reader.onload = () => {
        this.imageItems[index].url = reader.result as string;
      };
      reader.readAsDataURL(file);
    }
  }

  removeImageRow(index: number): void {
    this.imageItems.splice(index, 1);
    this.currentImgIndex = 0;
    if (this.imageItems.length > 0 && !this.imageItems.some(img => img.isPrimary)) {
      this.imageItems[0].isPrimary = true;
    }
  }

openAddRoomModal(): void {
    this.isModalOpen = true;
    this.newRoom.floor = 1;
    this.newRoom.roomNumber = '101';
    this.newRoom.customPrice = undefined;
    
    // 🔥 ĐÃ BỔ SUNG: Gán ID loại phòng mặc định dựa trên danh sách roomTypes có sẵn
    if (this.roomTypes && this.roomTypes.length > 0) {
      this.newRoom.roomTypeId = this.roomTypes[0].id;
      this.floorBatchConfig.roomTypeId = this.roomTypes[0].id;
    }

    this.imageItems = [];
    this.currentImgIndex = 0;
    this.addImageRow('local');
  }

  closeAddRoomModal(): void {
    this.isModalOpen = false;
  }

  switchTab(tab: 'single' | 'floor'): void {
    this.activeTab = tab;
    this.imageItems = [];
    this.currentImgIndex = 0;
    this.addImageRow('local');
    if (tab === 'floor') {
      this.floorBatchConfig.floor = 1;
      this.floorBatchConfig.startRoomNumber = 101;
    }
  }

  onUpdateRoom(): void {
    if (!this.editRoom.roomNumber.trim() || !this.editRoom.floor) {
      alert('Vui lòng nhập đầy đủ Số phòng và Tầng!');
      return;
    }

    const validImages = this.imageItems.filter(img => img.url && img.url.trim() !== '');
    if (validImages.length > 0 && !validImages.some(img => img.isPrimary)) {
      validImages[0].isPrimary = true;
    }

    this.editRoom.images = validImages.map(img => ({
      imageUrl: img.url,
      isPrimary: img.isPrimary
    }));

    this.roomService.updateRoom(this.selectedRoomId, this.editRoom).subscribe({
      next: (res: any) => {
        alert(res.message || 'Cập nhật thông tin phòng và danh sách album ảnh thành công!');
        this.closeEditModal();
        this.loadRoomMatrix();
      },
      error: (err: any) => {
        const errMsg = err.error?.message || err.message || 'Cập nhật thất bại';
        alert('Cập nhật thất bại: ' + errMsg);
      }
    });
  }

  closeEditModal(): void {
    this.isEditModalOpen = false;
  }

  onSaveRoom(): void {
    if (!this.newRoom.roomNumber.trim() || !this.newRoom.floor) {
      alert('Vui lòng nhập đầy đủ Số phòng và Tầng!');
      return;
    }

    const validImages = this.imageItems.filter(img => img.url.trim() !== '');
    this.newRoom.images = validImages.map(img => ({
      imageUrl: img.url,
      isPrimary: img.isPrimary
    }));
    this.newRoom.hotelId = this.currentHotelId;

    this.roomService.createRoom(this.newRoom).subscribe({
      next: () => {
        alert('Thêm thành công phòng mới vào hệ thống!');
        this.closeAddRoomModal();
        this.loadRoomMatrix();
      },
      error: (err: any) => {
        const errMsg = err.error?.message || err.message || 'Thất bại';
        alert('Không thể tạo phòng: ' + errMsg);
      }
    });
  }

  onSaveBatchFloorRooms(): void {
    if (!this.floorBatchConfig.totalRooms || this.floorBatchConfig.totalRooms <= 0) {
      alert('Số lượng phòng cần khởi tạo phải lớn hơn 0!');
      return;
    }

    const validImages = this.imageItems.filter(img => img.url.trim() !== '');
    const batchImagesPayload = validImages.map(img => ({
      imageUrl: img.url,
      isPrimary: img.isPrimary
    }));

    const batchPayload = {
      hotelId: this.currentHotelId,
      roomTypeId: this.floorBatchConfig.roomTypeId,
      floor: this.floorBatchConfig.floor,
      startRoomNumber: this.floorBatchConfig.startRoomNumber,
      totalRooms: this.floorBatchConfig.totalRooms,
      customPrice: this.floorBatchConfig.applyCustomPrice,
      images: batchImagesPayload
    };

    if (confirm(`Hệ thống sẽ tự động khởi tạo ${this.floorBatchConfig.totalRooms} phòng tại Tầng ${this.floorBatchConfig.floor}. Xác nhận?`)) {
      this.roomService.createBulkRooms(batchPayload).subscribe({
        next: (res: any) => {
          alert(res.message || 'Khởi tạo tầng hàng loạt thành công!');
          this.closeAddRoomModal();
          this.loadRoomMatrix();
        },
        error: (err: any) => alert('Lỗi khởi tạo hàng loạt: ' + (err.error?.message || err.message))
      });
    }
  }

  applyFilters(): void {
    this.filteredRooms = this.rooms.filter(room => {
      const matchStatus = this.selectedStatusFilter === 'ALL' || room.status === this.selectedStatusFilter;
      const matchFloor = this.selectedFloorFilter === 'ALL' || room.floor === +this.selectedFloorFilter;
      return matchStatus && matchFloor;
    });
  }

  onStatusChange(roomId: number, newStatus: RoomStatus): void {
    this.roomService.updateRoomStatus(roomId, newStatus).subscribe({
      next: (res: any) => {
        alert(res.message || 'Cập nhật trạng thái phòng thành công!');
        this.loadRoomMatrix();
      },
      error: (err: any) => {
        alert('Cập nhật trạng thái thất bại: ' + (err.error?.message || err.message));
        this.loadRoomMatrix();
      }
    });
  }

  onDeleteRoom(roomId: number): void {
    if (confirm('Bạn có chắc chắn muốn xóa phòng này khỏi hệ thống? (Hệ thống sẽ thực hiện chuyển vào kho lưu trữ ẩn để giữ vững lịch sử đặt phòng)')) {
      this.roomService.deleteRoom(roomId).subscribe({
        next: (res: any) => {
          alert(res.message || 'Xóa phòng thành công');
          this.closeEditModal();
          this.loadRoomMatrix();
        },
        error: (err: any) => alert('Không thể xóa phòng: ' + (err.error?.message || err.message))
      });
    }
  }

  onCardClick(room: RoomResponseDTO, event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (target.tagName === 'SELECT' || target.tagName === 'OPTION' || target.classList.contains('btn-delete')) {
      return;
    }

    if (this.userRole !== 'ADMIN') {
      alert('Chỉ tài khoản Quản trị viên mới có quyền chỉnh sửa thông tin phòng!');
      return;
    }

    this.selectedRoomId = room.roomId;
    const matchedType = this.roomTypes.find(t => t.typeName === room.typeName);

    this.editRoom = {
      hotelId: this.currentHotelId,
      roomTypeId: matchedType ? matchedType.id : 1,
      roomNumber: room.roomNumber,
      floor: room.floor,
      customPrice: room.price,
      images: []
    };

    this.maxDiscountPercentage = 0;
    this.calculatedFinalPrice = room.price;
    this.selectedPromoIdForRoom = null;

    if (room.appliedPromotions && room.appliedPromotions.length > 0) {
      const bestPromo = room.appliedPromotions.reduce((max, p) => 
        p.discountPercentage > max.discountPercentage ? p : max, room.appliedPromotions[0]
      );
      
      this.maxDiscountPercentage = bestPromo.discountPercentage;
      this.calculatedFinalPrice = room.price * (1 - this.maxDiscountPercentage / 100);
      this.selectedPromoIdForRoom = Number(bestPromo.id); // Ép sang kiểu số nguyên để so khớp ngValue
    }

    this.imageItems = [];
    this.currentImgIndex = 0;

    if (room.albumImages && room.albumImages.length > 0) {
      room.albumImages.forEach((imgUrl: string) => {
        const isLocalFile = imgUrl.startsWith('data:image');
        this.imageItems.push({
          source: isLocalFile ? 'local' : 'web',
          url: imgUrl,
          isPrimary: imgUrl === room.imageUrl
        });
      });
      
      const primaryIdx = this.imageItems.findIndex(img => img.isPrimary);
      if (primaryIdx !== -1) {
        this.currentImgIndex = primaryIdx;
      }
    } else if (room.imageUrl) {
      const isLocalFile = room.imageUrl.startsWith('data:image');
      this.imageItems.push({ source: isLocalFile ? 'local' : 'web', url: room.imageUrl, isPrimary: true });
    }

    this.isEditModalOpen = true;
  }
}