import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router } from '@angular/router';
import { HeaderComponent } from '../../components/header/header.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { RoomService } from '../../services/room.service';
import { RoomResponseDTO } from '../../models/room.model';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, HeaderComponent, FooterComponent],
  templateUrl: './home.component.html',
})
export class HomeComponent implements OnInit {
  searchForm!: FormGroup;
  featuredRooms: RoomResponseDTO[] = [];
  roomTypes: any[] = [];

  promotions = [
    { title: 'Giảm 20% mùa hè', code: 'SUMMER20' },
    { title: 'Combo Gia đình', code: 'FAMILYFUN' }
  ];

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private roomService: RoomService
  ) {}

  ngOnInit(): void {
    this.initSearchForm();
    this.loadFeaturedRooms();
    this.loadRoomTypes();
  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      roomType: ['', Validators.required],
      checkIn: ['', Validators.required],
      checkOut: ['', Validators.required]
    }, { validators: this.dateValidator });
  }

  dateValidator(control: AbstractControl): ValidationErrors | null {
    const checkIn = control.get('checkIn')?.value;
    const checkOut = control.get('checkOut')?.value;
    if (checkIn && checkOut && new Date(checkOut) <= new Date(checkIn)) {
      return { dateInvalid: true };
    }
    return null;
  }

  loadRoomTypes(): void {
    this.roomService.getRoomTypes().subscribe({
      next: (types) => this.roomTypes = types,
      error: () => this.roomTypes = [{ typeName: 'Deluxe' }, { typeName: 'Suite' }]
    });
  }

  loadFeaturedRooms(): void {
    this.roomService.getFeaturedRooms().subscribe({
      next: (rooms) => this.featuredRooms = rooms
    });
  }

  // Điều hướng tìm kiếm từ thanh search
  onSearch(): void {
    if (this.searchForm.valid) {
      this.router.navigate(['/rooms'], { queryParams: this.searchForm.value });
    } else {
      this.searchForm.markAllAsTouched();
    }
  }

  // Điều hướng nhanh khi click loại phòng
  onSearchByRoomType(typeName: string): void {
    this.router.navigate(['/rooms'], { 
      queryParams: { roomType: typeName } 
    });
  }
}