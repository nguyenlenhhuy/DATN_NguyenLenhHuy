import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RoomService } from '../../services/room.service';
import { RoomResponseDTO } from '../../models/room.model';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent implements OnInit {
  private roomService = inject(RoomService);
  featuredRooms = signal<RoomResponseDTO[]>([]);

  ngOnInit() {
    this.roomService.getFeaturedRooms().subscribe(data => this.featuredRooms.set(data));
  }
}