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
  
  currentHotelId: number = 1; 
  userRole: string = 'ADMIN'; 
  
  isModalOpen: boolean = false;      
  isEditModalOpen: boolean = false;  
  activeTab: 'single' | 'floor' = 'single'; 

  selectedStatusFilter: string = 'ALL';
  selectedFloorFilter: string = 'ALL'; // Biến lưu giữ bộ lọc tầng hiện tại
  floors: number[] = [];
  statusOptions = Object.values(RoomStatus);

  imageItems: { source: 'local' | 'web', url: string, isPrimary: boolean }[] = [];
  currentImgIndex: number = 0;

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
    }
  }

  // ĐÃ SỬA GIỮ BỘ LỌC: Hàm load data không tự ý reset bộ lọc về 'ALL' nữa
  loadRoomMatrix(): void {
    this.roomService.getRoomsByHotel(this.currentHotelId).subscribe({
      next: (data) => {
        this.rooms = data;
        // Thu thập danh sách các tầng để vẽ thẻ select
        this.floors = Array.from(new Set(data.map(r => r.floor))).sort((a, b) => a - b);
        
        // Kích hoạt hàm apply bộ lọc để giữ đúng tầng người dùng đang chọn trước đó
        this.applyFilters();
      },
      error: (err: any) => alert('Thông báo hệ thống: ' + (err.error?.message || err.message))
    });
  }

  loadRoomTypes(): void {
    this.roomTypes = [
      { id: 1, typeName: 'VIP Single', basePrice: 1200000, maxOccupancy: 2, isFeatured: true },
      { id: 2, typeName: 'Standard Double', basePrice: 650000, maxOccupancy: 4, isFeatured: false },
      { id: 3, typeName: 'VIP King', basePrice: 2500000, maxOccupancy: 2, isFeatured: true }
    ];
    if (this.roomTypes.length > 0) {
      this.newRoom.roomTypeId = this.roomTypes[0].id;
      this.floorBatchConfig.roomTypeId = this.roomTypes[0].id;
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
      alert(`Đã đặt ảnh số ${this.currentImgIndex + 1} làm hình đại diện chính thành công!`);
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

  // ĐÃ SỬA CHỨC NĂNG ĐỔI ẢNH: Gọi API PUT gửi toàn bộ cấu hình ảnh đại diện lên Server
  onUpdateRoom(): void {
    if (!this.editRoom.roomNumber.trim() || !this.editRoom.floor) {
      alert('Vui lòng nhập đầy đủ Số phòng và Tầng!');
      return;
    }

    const validImages = this.imageItems.filter(img => img.url.trim() !== '');
    this.editRoom.images = validImages.map(img => ({
      imageUrl: img.url,
      isPrimary: img.isPrimary
    }));

    // Thực hiện gọi hàm API lưu dữ liệu thực tế xuống DB
    this.roomService.updateRoom(this.selectedRoomId, this.editRoom).subscribe({
      next: (res: any) => {
        alert(res.message || 'Cập nhật thông tin phòng và ảnh đại diện thành công!');
        this.closeEditModal();
        this.loadRoomMatrix(); // Gọi lại hàm nạp, hàm này sẽ tự động giữ nguyên filter tầng cho bạn
      },
      error: (err: any) => alert('Cập nhật thất bại: ' + (err.error?.message || err.message))
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
        alert(`Thêm thành công phòng ${this.newRoom.roomNumber}!`);
        this.closeAddRoomModal();
        this.loadRoomMatrix();
      },
      error: (err: any) => alert('Thất bại: ' + (err.error?.message || err.message))
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
        next: (msg: string) => {
          alert(msg || 'Khởi tạo tầng hàng loạt thành công!');
          this.closeAddRoomModal();
          this.loadRoomMatrix();
        },
        error: (err: any) => alert('Lỗi khởi tạo hàng loạt: ' + (err.error?.message || err.message))
      });
    }
  }

  // ĐÃ SỬA GIỮ BỘ LỌC: Logic lọc dữ liệu null-safe, bám sát các biến filter trên giao diện
  applyFilters(): void {
    this.filteredRooms = this.rooms.filter(room => {
      const matchStatus = this.selectedStatusFilter === 'ALL' || room.status === this.selectedStatusFilter;
      const matchFloor = this.selectedFloorFilter === 'ALL' || room.floor === +this.selectedFloorFilter;
      return matchStatus && matchFloor;
    });
  }

 onStatusChange(roomId: number, event: Event): void {
  const selectElement = event.target as HTMLSelectElement;
  const newStatus = selectElement.value as RoomStatus;

  this.roomService.updateRoomStatus(roomId, newStatus).subscribe({
    next: (msg: any) => {
      // Thông báo và reload lại ma trận, bộ lọc giữ tầng vẫn được bảo toàn nguyên vẹn
      alert(msg.message || 'Cập nhật trạng thái thành công!');
      this.loadRoomMatrix(); 
    },
    error: (err: any) => {
      alert('Cập nhật thất bại: ' + (err.error?.message || err.message));
      this.loadRoomMatrix(); // Nếu lỗi thì ép nạp lại dữ liệu cũ từ DB để reset select về đúng trạng thái gốc
    }
  });
}

  onDeleteRoom(roomId: number): void {
    if (confirm('Bạn có chắc chắn muốn xóa phòng này khỏi hệ thống?')) {
      this.roomService.deleteRoom(roomId).subscribe({
        next: (msg: any) => {
          alert(msg.message || 'Xóa phòng thành công');
          this.closeEditModal();
          this.loadRoomMatrix();
        },
        error: (err: any) => alert('Không thể xóa: ' + (err.error?.message || err.message))
      });
    }
  }
}