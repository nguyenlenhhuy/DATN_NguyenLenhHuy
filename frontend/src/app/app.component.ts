import { Component } from '@angular/core';
import { CommonModule } from '@angular/common'; // Thêm CommonModule để bổ sung các directive cơ bản
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from './components/header/header.component'; // Import HeaderComponent vào cấu trúc tổng

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,   // Khai báo CommonModule
    RouterOutlet,   // Khai báo vùng hiển thị Route động
    HeaderComponent // Khai báo HeaderComponent để Angular nhận diện được thẻ <app-header>
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  title = 'frontend';
}