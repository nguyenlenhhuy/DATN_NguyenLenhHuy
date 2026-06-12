import { Component, ViewChild, ElementRef, AfterViewChecked, OnInit } from '@angular/core'; // Thêm OnInit
import { CommonModule } from '@angular/common'; 
import { FormsModule } from '@angular/forms';   
import { ChatService } from '../../services/chat.service';
import { SafePipe } from '../../pipes/safe.pipe'; 

interface ChatMessage {
  text: string;
  sender: 'user' | 'bot';
}

@Component({
  selector: 'app-chat-widget',
  standalone: true,
  imports: [CommonModule, FormsModule, SafePipe], 
  templateUrl: './chat-widget.component.html',
  styleUrls: ['./chat-widget.component.scss'] 
})
export class ChatWidgetComponent implements AfterViewChecked, OnInit { // Thêm implements OnInit
  isOpen = false;
  userInput = '';
  isLoading = false;
  
  @ViewChild('chatBody') private chatBodyContainer!: ElementRef;

  // Xóa lời chào mặc định cứng ở đây
  messages: ChatMessage[] = []; 

  constructor(private chatService: ChatService) {}

  // 1. TẢI LẠI LỊCH SỬ KHI MỞ TRANG
  ngOnInit() {
    const savedChat = sessionStorage.getItem('chatHistory');
    if (savedChat) {
      this.messages = JSON.parse(savedChat);
    } else {
      // Nếu chưa có lịch sử, mới hiển thị lời chào mặc định
      this.messages = [
        { text: 'Xin chào! Tôi là lễ tân AI. Tôi có thể giúp gì cho việc đặt phòng của bạn?', sender: 'bot' }
      ];
      this.saveChat();
    }
  }

  // 2. HÀM LƯU LỊCH SỬ
  private saveChat() {
    sessionStorage.setItem('chatHistory', JSON.stringify(this.messages));
  }

  ngAfterViewChecked() {
    this.scrollToBottom();
  }

  private scrollToBottom(): void {
    try {
      this.chatBodyContainer.nativeElement.scrollTop = this.chatBodyContainer.nativeElement.scrollHeight;
    } catch(err) { }
  }

  toggleChat() {
    this.isOpen = !this.isOpen;
  }

  sendMessage() {
    if (!this.userInput.trim()) return;

    const messageToSend = this.userInput;
    this.messages.push({ text: messageToSend, sender: 'user' });
    this.saveChat(); // Lưu sau khi user nhắn

    this.userInput = '';
    this.isLoading = true;

    this.chatService.sendMessage(messageToSend).subscribe({
      next: (response) => {
        this.messages.push({ text: response, sender: 'bot' });
        this.saveChat(); // Lưu sau khi bot trả lời
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Lỗi khi gọi Chatbot:', err);
        this.messages.push({ text: 'Hệ thống đang bận, vui lòng thử lại sau!', sender: 'bot' });
        this.isLoading = false;
      }
    });
  }
}