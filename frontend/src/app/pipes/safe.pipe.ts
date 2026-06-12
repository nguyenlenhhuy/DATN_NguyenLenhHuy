import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Pipe({
  name: 'safeHtml',
  standalone: true // Nếu project dùng Standalone Component
})
export class SafePipe implements PipeTransform {

  constructor(private sanitizer: DomSanitizer) {}

  transform(value: string): SafeHtml {
    // Bỏ qua kiểm duyệt của Angular và render thành HTML thật
    return this.sanitizer.bypassSecurityTrustHtml(value);
  }
}