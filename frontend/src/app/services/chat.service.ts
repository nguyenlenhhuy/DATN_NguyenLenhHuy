import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private apiUrl = 'http://localhost:8080/api/chat/send';

  constructor(private http: HttpClient) { }

  sendMessage(message: string): Observable<string> {
    // Lưu ý cực kỳ quan trọng: Vì backend Spring Boot trả về String (text) 
    // chứ không phải JSON object, ta BẮT BUỘC phải khai báo responseType: 'text'
    return this.http.post(this.apiUrl, message, { responseType: 'text' });
  }
}