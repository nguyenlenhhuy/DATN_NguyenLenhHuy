import { Component } from '@angular/core';

@Component({
  selector: 'app-footer',
  standalone: true,
  template: `
  <footer class="bg-gray-900 text-white pt-16 pb-8">
    <div class="container mx-auto px-4 text-center">
      <div class="grid grid-cols-1 md:grid-cols-2 gap-8 mb-8 text-left">
        <div>
          <h3 class="text-2xl font-bold text-blue-500 mb-4">LuxeHotel</h3>
          <p class="text-gray-400">Nền tảng đặt phòng tiện lợi và an toàn.</p>
        </div>
        <div>
          <h4 class="font-bold mb-4 uppercase">Liên hệ</h4>
          <p class="text-gray-400">Email: support&#64;luxehotel.com | Hotline: 1900 1234</p>
        </div>
      </div>
      <div class="border-t border-gray-800 pt-8 text-gray-500 text-sm">
        &copy; 2026 LuxeHotel. Tất cả các quyền được bảo lưu.
      </div>
    </div>
  </footer>
  `
})
export class FooterComponent {}