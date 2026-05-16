import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
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
  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      location: [''],
      checkIn: ['', Validators.required],
      checkOut: ['', Validators.required],
      guestCount: [2, [Validators.required, Validators.min(1)]]
    });
  }

  loadFeaturedRooms(): void {
    this.roomService.getFeaturedRooms().subscribe({
      next: (rooms) => this.featuredRooms = rooms,
      error: (err) => {
        console.error('Lỗi API', err);
        // Dữ liệu giả định nếu Backend chưa chạy
        this.featuredRooms = [
          { roomId: 1, roomNumber: '101', typeName: 'Deluxe Room', price: 1500000, hotelName: 'Luxe Hotel Đà Nẵng', imageUrl: 'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=500' }
        ];
      }
    });
  }

  onSearch(): void {
    if (this.searchForm.valid) {
      this.router.navigate(['/rooms'], { queryParams: this.searchForm.value });
    }
  }
}