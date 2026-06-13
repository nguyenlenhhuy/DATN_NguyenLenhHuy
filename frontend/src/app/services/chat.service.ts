import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private apiUrl = `${environment.apiUrl}/chat/send`;

  constructor(private http: HttpClient) { }

  sendMessage(message: string): Observable<string> {
    // Lưu ý cực kỳ quan trọng: Vì backend Spring Boot trả về String (text) 
    // chứ không phải JSON object, ta BẮT BUỘC phải khai báo responseType: 'text'
    return this.http.post(this.apiUrl, message, { responseType: 'text' });
  }
}