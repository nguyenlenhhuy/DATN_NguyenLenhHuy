import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';

export interface ChatRequestDTO {
  message: string;
  userRole: string;
  imageBase64?: string;
  imageMimeType?: string;
}

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly baseUrl = `${environment.apiUrl}/chat`;

  constructor(private http: HttpClient) {}

  /** REST đồng bộ — dùng khi stream thất bại hoặc có ảnh đính kèm */
  sendMessageSync(dto: ChatRequestDTO): Observable<string> {
    return this.http.post(`${this.baseUrl}/send`, dto, { responseType: 'text' });
  }

  /**
   * SSE Streaming via Fetch API.
   * - Thử endpoint /stream trước (SSE từ Gemini)
   * - Nếu không nhận được chunk nào hoặc lỗi → tự fallback sang /send (REST)
   * - Timeout 25 giây (AbortController)
   */
  async streamMessage(
    message: string,
    userRole: string,
    onChunk: (text: string) => void,
    onDone: () => void,
    onError: () => void,
    imageBase64?: string,
    imageMimeType?: string
  ): Promise<void> {
    // Tin nhắn có ảnh → dùng REST ngay (không stream)
    if (imageBase64) {
      await this.fallbackToRest({ message, userRole, imageBase64, imageMimeType }, onChunk, onDone, onError);
      return;
    }

    const token = localStorage.getItem('token');
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token && token !== 'undefined' && token !== 'null') {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 25000);

    try {
      const response = await fetch(`${this.baseUrl}/stream`, {
        method: 'POST',
        headers,
        body: JSON.stringify({ message, userRole }),
        signal: controller.signal
      });
      clearTimeout(timeoutId);

      if (!response.ok || !response.body) {
        await this.fallbackToRest({ message, userRole }, onChunk, onDone, onError);
        return;
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';
      let receivedAnyChunk = false;

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() ?? '';

        for (const line of lines) {
          if (line.startsWith('data:')) {
            const text = line.substring(5).trim();
            if (text && text !== '[DONE]') {
              onChunk(text);
              receivedAnyChunk = true;
            }
          }
        }
      }

      // Xử lý phần còn trong buffer
      if (buffer.startsWith('data:')) {
        const text = buffer.substring(5).trim();
        if (text && text !== '[DONE]') {
          onChunk(text);
          receivedAnyChunk = true;
        }
      }

      // Không nhận được chunk nào → SSE endpoint không hoạt động, fallback REST
      if (!receivedAnyChunk) {
        await this.fallbackToRest({ message, userRole }, onChunk, onDone, onError);
        return;
      }

      onDone();
    } catch (e: any) {
      clearTimeout(timeoutId);
      if (e?.name === 'AbortError') {
        console.warn('[ChatService] Stream timeout — fallback to REST');
      } else {
        console.error('[ChatService] Stream error:', e);
      }
      await this.fallbackToRest({ message, userRole }, onChunk, onDone, onError);
    }
  }

  private async fallbackToRest(
    dto: ChatRequestDTO,
    onChunk: (text: string) => void,
    onDone: () => void,
    onError: () => void
  ): Promise<void> {
    try {
      const text = await firstValueFrom(this.sendMessageSync(dto));
      onChunk(text || 'Xin lỗi, tôi không thể trả lời câu hỏi này lúc này.');
      onDone();
    } catch {
      onError();
    }
  }
}
