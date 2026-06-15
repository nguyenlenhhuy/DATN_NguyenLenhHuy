import {
  Component, ViewChild, ElementRef,
  AfterViewChecked, OnInit, NgZone, OnDestroy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatService } from '../../services/chat.service';
import { AuthService } from '../../services/auth.service';
import { SafePipe } from '../../pipes/safe.pipe';

interface ChatMessage {
  text: string;
  sender: 'user' | 'bot';
  isStreaming?: boolean;
  imagePreview?: string; // Chỉ dùng để hiển thị, không lưu sessionStorage
}

@Component({
  selector: 'app-chat-widget',
  standalone: true,
  imports: [CommonModule, FormsModule, SafePipe],
  templateUrl: './chat-widget.component.html',
  styleUrls: ['./chat-widget.component.scss']
})
export class ChatWidgetComponent implements AfterViewChecked, OnInit, OnDestroy {
  isOpen = false;
  userInput = '';
  isLoading = false;
  messages: ChatMessage[] = [];
  showHandoffBanner = false;

  // Voice
  isRecording = false;
  voiceSupported = false;
  private recognition: any = null;

  // Image
  selectedImage: { base64: string; mimeType: string; preview: string } | null = null;

  // Quick replies
  readonly quickReplies = [
    '🛏️ Còn phòng trống không?',
    '💰 Giá phòng bao nhiêu?',
    '🕐 Check-in mấy giờ?',
    '❌ Chính sách hủy?'
  ];

  private consecutiveFailures = 0;
  private readonly HANDOFF_THRESHOLD = 2;

  @ViewChild('chatBody')  private chatBodyRef!: ElementRef;
  @ViewChild('fileInput') private fileInputRef!: ElementRef<HTMLInputElement>;

  constructor(
    private chatService: ChatService,
    private authService: AuthService,
    private ngZone: NgZone           // ← FIX: NgZone để callback fetch chạy trong Angular zone
  ) {}

  // ---- Lifecycle ----
  ngOnInit(): void {
    this.voiceSupported = !!(
      (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition
    );

    const saved = sessionStorage.getItem('chatHistory');
    if (saved) {
      try {
        this.messages = JSON.parse(saved);
        // Lọc bỏ message đang stream dở (từ session cũ bị ngắt)
        this.messages = this.messages.map(m => ({ ...m, isStreaming: false }));
      } catch {
        this.initGreeting();
      }
    } else {
      this.initGreeting();
    }
  }

  ngAfterViewChecked(): void {
    this.scrollBottom();
  }

  ngOnDestroy(): void {
    this.recognition?.abort();
  }

  // ---- Role ----
  get userRole(): string {
    if (!this.authService.isLoggedIn()) return 'GUEST';
    const role = this.authService.getRole()?.toUpperCase() ?? '';
    if (role.includes('ADMIN')) return 'ADMIN';
    if (role.includes('STAFF')) return 'STAFF';
    return 'CUSTOMER';
  }

  get roleLabel(): string {
    const labels: Record<string, string> = {
      ADMIN:    '📊 Trợ lý Quản trị',
      STAFF:    '🛎️ Trợ lý Nhân viên',
      CUSTOMER: '🏨 Lễ tân AI - LuxeHotel',
      GUEST:    '🏨 Lễ tân AI - LuxeHotel'
    };
    return labels[this.userRole] ?? labels['GUEST'];
  }

  get showQuickReplies(): boolean {
    return this.messages.length <= 1 && !this.isLoading && !this.selectedImage;
  }

  // ---- UI ----
  toggleChat(): void { this.isOpen = !this.isOpen; }

  clearChat(): void {
    sessionStorage.removeItem('chatHistory');
    this.consecutiveFailures = 0;
    this.showHandoffBanner = false;
    this.selectedImage = null;
    this.recognition?.abort();
    this.isRecording = false;
    this.initGreeting();
  }

  selectQuickReply(text: string): void {
    this.userInput = text.replace(/^[^\s]+ /, ''); // Bỏ emoji prefix
    this.sendMessage();
  }

  // ---- Voice Input ----
  toggleVoice(): void {
    if (this.isRecording) {
      this.recognition?.stop();
      return;
    }

    const SpeechRecognition =
      (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SpeechRecognition) return;

    this.recognition = new SpeechRecognition();
    this.recognition.lang = 'vi';
    this.recognition.continuous = false;
    this.recognition.interimResults = true;
    this.recognition.maxAlternatives = 1;

    this.recognition.onstart = () =>
      this.ngZone.run(() => (this.isRecording = true));

    this.recognition.onresult = (event: any) => {
      const transcript = Array.from(event.results as SpeechRecognitionResultList)
        .map((r: SpeechRecognitionResult) => r[0].transcript)
        .join('');
      this.ngZone.run(() => (this.userInput = transcript));
    };

    this.recognition.onend = () =>
      this.ngZone.run(() => (this.isRecording = false));

    this.recognition.onerror = (event: any) => {
      const errCode: string = event?.error ?? 'unknown';
      if (errCode === 'not-allowed' || errCode === 'service-not-allowed') {
        console.warn('[Voice] Microphone permission denied:', errCode);
      } else if (errCode === 'language-not-supported') {
        console.warn('[Voice] Language not supported, falling back to en-US');
        this.ngZone.run(() => {
          this.recognition.lang = 'en-US';
          this.isRecording = false;
        });
        return;
      } else if (errCode !== 'no-speech' && errCode !== 'aborted') {
        console.error('[Voice] SpeechRecognition error:', errCode);
      }
      this.ngZone.run(() => (this.isRecording = false));
    };

    this.recognition.start();
  }

  // ---- Image Upload ----
  openFileInput(): void {
    this.fileInputRef?.nativeElement.click();
  }

  onImageSelect(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;

    const file = input.files[0];
    if (!file.type.startsWith('image/')) {
      alert('Chỉ hỗ trợ file ảnh (JPG, PNG, WEBP, GIF).');
      input.value = '';
      return;
    }
    if (file.size > 4 * 1024 * 1024) {
      alert('Ảnh quá lớn! Vui lòng chọn ảnh nhỏ hơn 4MB.');
      input.value = '';
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      const dataUrl = reader.result as string;
      const base64 = dataUrl.split(',')[1];
      this.ngZone.run(() => {
        this.selectedImage = { base64, mimeType: file.type, preview: dataUrl };
      });
    };
    reader.readAsDataURL(file);
    input.value = '';
  }

  removeImage(): void {
    this.selectedImage = null;
  }

  // ---- Send Message ----
  async sendMessage(): Promise<void> {
    const text = this.userInput.trim();
    if ((!text && !this.selectedImage) || this.isLoading) return;

    const imgSnap = this.selectedImage;
    this.selectedImage = null;
    this.userInput = '';
    this.isLoading = true;
    this.showHandoffBanner = false;

    // Thêm tin nhắn người dùng (kèm ảnh preview nếu có)
    this.messages.push({
      text: text || '📎 [Ảnh đính kèm]',
      sender: 'user',
      imagePreview: imgSnap?.preview
    });
    this.persist();

    // Tạo placeholder bot message cho streaming
    const botIdx = this.messages.length;
    this.messages.push({ text: '', sender: 'bot', isStreaming: true });

    // ============================================================
    // FIX NgZone: bọc tất cả state-mutation callbacks trong ngZone.run()
    // để Angular change detection nhận biết và re-render UI
    // ============================================================
    await this.chatService.streamMessage(
      text || '[Xem ảnh đính kèm và tư vấn phòng phù hợp]',
      this.userRole,

      // onChunk
      (chunk) => this.ngZone.run(() => {
        this.messages[botIdx].text += chunk;
      }),

      // onDone
      () => this.ngZone.run(() => {
        this.messages[botIdx].isStreaming = false;
        this.isLoading = false;
        this.evaluateHandoff(this.messages[botIdx].text);
        this.persist();
      }),

      // onError
      () => this.ngZone.run(() => {
        this.messages[botIdx].text =
          'Hiện tại hệ thống đang bận. Vui lòng thử lại sau hoặc liên hệ hotline!';
        this.messages[botIdx].isStreaming = false;
        this.isLoading = false;
        this.consecutiveFailures++;
        if (this.consecutiveFailures >= this.HANDOFF_THRESHOLD) {
          this.showHandoffBanner = true;
        }
        this.persist();
      }),

      imgSnap?.base64,
      imgSnap?.mimeType
    );
  }

  // ---- Human Handoff ----
  requestHumanSupport(): void {
    this.showHandoffBanner = false;
    this.consecutiveFailures = 0;
    this.messages.push({
      text: '✅ Đã ghi nhận yêu cầu kết nối nhân viên. Nhân viên sẽ liên hệ bạn qua email hoặc điện thoại trong giờ làm việc (8:00–22:00). Cảm ơn bạn đã kiên nhẫn!',
      sender: 'bot'
    });
    this.persist();
  }

  // ---- Helpers ----
  private evaluateHandoff(responseText: string): void {
    const lower = responseText.toLowerCase();
    const isVague =
      lower.includes('không thể') || lower.includes('xin lỗi') ||
      lower.includes('không rõ')  || lower.includes('không biết') ||
      lower.includes('tạm thời bận');
    if (isVague) {
      this.consecutiveFailures++;
      if (this.consecutiveFailures >= this.HANDOFF_THRESHOLD) this.showHandoffBanner = true;
    } else {
      this.consecutiveFailures = 0;
    }
  }

  private initGreeting(): void {
    const greetings: Record<string, string> = {
      ADMIN:    'Xin chào Quản trị viên! Tôi có thể cung cấp số liệu vận hành nhanh. Bạn muốn xem gì?',
      STAFF:    'Xin chào nhân viên! Tôi hỗ trợ tra cứu tình trạng phòng và thông tin vận hành. Bạn cần gì?',
      CUSTOMER: 'Xin chào quý khách! Tôi là lễ tân AI của LuxeHotel. Tôi có thể giúp bạn tìm phòng hoặc kiểm tra đơn đặt phòng.',
      GUEST:    'Xin chào! Tôi là lễ tân AI của LuxeHotel. Tôi có thể giúp gì cho bạn hôm nay?'
    };
    this.messages = [{ text: greetings[this.userRole] ?? greetings['GUEST'], sender: 'bot' }];
    this.persist();
  }

  // Không lưu imagePreview vào sessionStorage (quá lớn, vượt 5MB limit)
  private persist(): void {
    const toSave = this.messages.map(({ text, sender, isStreaming }) =>
      ({ text, sender, isStreaming: false })
    );
    sessionStorage.setItem('chatHistory', JSON.stringify(toSave));
  }

  private scrollBottom(): void {
    try {
      this.chatBodyRef.nativeElement.scrollTop =
        this.chatBodyRef.nativeElement.scrollHeight;
    } catch {}
  }
}
