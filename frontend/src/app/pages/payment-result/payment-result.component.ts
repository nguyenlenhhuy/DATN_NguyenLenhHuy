import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-payment-result',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './payment-result.component.html'
})
export class PaymentResultComponent implements OnInit {
  state: 'loading' | 'success' | 'cancelled' | 'error' = 'loading';
  message = '';
  bookingId: number | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    const params = this.route.snapshot.queryParams;
    const code = params['code'];
    const cancel = params['cancel'];
    const orderCode = params['orderCode'];

    if (cancel === 'true' || code === 'CANCELLED') {
      this.state = 'cancelled';
      this.message = 'Bạn đã hủy thanh toán. Đơn đặt phòng chưa được xác nhận.';
      return;
    }

    if (code === '00' && orderCode) {
      this.http.get<any>(`${environment.apiUrl}/bookings/payment/verify?orderCode=${orderCode}`)
        .subscribe({
          next: (res) => {
            if (res.success) {
              this.state = 'success';
              this.bookingId = res.bookingId;
              this.message = 'Thanh toán thành công! Hóa đơn đã được gửi đến email của bạn.';
            } else {
              this.state = 'error';
              this.message = res.message || 'Không thể xác nhận thanh toán. Vui lòng liên hệ hỗ trợ.';
            }
          },
          error: (err) => {
            this.state = 'error';
            this.message = err.error?.message || 'Lỗi xác nhận thanh toán. Vui lòng liên hệ hỗ trợ.';
          }
        });
    } else {
      this.state = 'error';
      this.message = 'Kết quả thanh toán không hợp lệ. Vui lòng kiểm tra lại lịch sử đặt phòng.';
    }
  }

  goToHistory(): void {
    this.router.navigate(['/history']);
  }

  goToHome(): void {
    this.router.navigate(['/home']);
  }
}
