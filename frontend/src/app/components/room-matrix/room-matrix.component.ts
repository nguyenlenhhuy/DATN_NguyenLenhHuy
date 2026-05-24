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
  isTypeModalOpen: boolean = false; // Trạng thái Modal cấu hình Loại phòng
  activeTab: 'single' | 'floor' = 'single'; 

  selectedStatusFilter: string = 'ALL';
  selectedFloorFilter: string = 'ALL'; 
  floors: number[] = [];
  statusOptions = Object.values(RoomStatus);

  imageItems: { source: 'local' | 'web', url: string, isPrimary: boolean }[] = [];
  currentImgIndex: number = 0; 

  // Cấu trúc DTO đồng bộ hoàn chỉnh khớp với RoomTypeRequest của Java Backend
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
    }
  }

  // ĐÃ CẢI TIẾN: Giữ nguyên bộ lọc tầng hiện tại sau khi tải lại sơ đồ lưới phòng
  loadRoomMatrix(): void {
    this.roomService.getRoomsByHotel(this.currentHotelId).subscribe({
      next: (data) => {
        this.rooms = data;
        this.floors = Array.from(new Set(data.map(r => r.floor))).sort((a, b) => a - b);
        this.applyFilters(); // Đảm bảo cố định tầng đang chọn
      },
      error: (err: any) => alert('Thông báo hệ thống: ' + (err.error?.message || err.message))
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

  openTypeModal(): void {
    this.isTypeModalOpen = true;
    this.newTypeData = { hotelId: this.currentHotelId, typeName: '', basePrice: undefined, maxOccupancy: 2, isFeatured: false }; 
  }

  closeTypeModal(): void {
    this.isTypeModalOpen = false;
  }

  // ĐÃ SỬA: Đọc thông điệp Object JSON (res.message) trả về từ Backend
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

  // ĐÃ SỬA: Đọc sạch cấu trúc res.message từ Object JSON phản hồi
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

    this.roomService.updateRoom(this.selectedRoomId, this.editRoom).subscribe({
      next: (res: any) => {
        alert(res.message || 'Cập nhật thông tin phòng và ảnh đại diện thành công!');
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

  // ĐÃ BỔ SUNG: Khối bẫy lỗi gõ trùng số phòng đang hoạt động hoặc nằm trong kho ẩn lịch sử gửi từ Custom Exception Backend
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
}