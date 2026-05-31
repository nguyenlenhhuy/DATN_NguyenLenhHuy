import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BookingManagementService } from '../../services/booking-management.service';
import { Chart, registerables } from 'chart.js';
Chart.register(...registerables);

@Component({
  selector: 'app-overview',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './overview.component.html'
})
export class OverviewComponent implements OnInit {
  currentFilter: 'DATE' | 'WEEK' | 'MONTH' | 'YEAR' = 'WEEK';
  
  statsData: any = {
    totalRevenueToday: 0,
    totalBookingsToday: 0,
    availableRooms: 0,
    totalRooms: 0
  };

  // Các mảng quản trị phân phối dữ liệu lọc liên thông
  allBookings: any[] = [];
  filteredBookings: any[] = [];
  selectedDateLabel: string = ''; // Lưu tên ngày đang click chọn lọc

  // Chỉ số KPIs khách sạn nâng cao
  occupancyRate: number = 0;
  averageDailyRate: number = 0;

  // Tham chiếu DOM vẽ biểu đồ
  @ViewChild('revenueChart') revenueChart!: ElementRef;
  @ViewChild('occupancyChart') occupancyChart!: ElementRef;
  
  revenueChartInstance: any;
  occupancyChartInstance: any;

  constructor(private bookingService: BookingManagementService) {}

  ngOnInit(): void {
    this.loadStats();
    this.loadAllBookings();
  }

  /**
   * Tải các chỉ số tài chính và dữ liệu trục biểu đồ từ Backend
   */
  loadStats(): void {
    this.bookingService.getDashboardStats(this.currentFilter).subscribe({
      next: (res: any) => {
        this.statsData = res;
        
        // Tính toán các chỉ số KPIs phục vụ hiển thị lên thẻ Card bổ sung
        this.calculateHotelKPIs();

        // Vẽ đồng thời 2 biểu đồ cột và tròn
        this.renderRevenueChart(res.last7DaysRevenue);
        this.renderOccupancyChart();
      },
      error: (err: any) => console.error('Lỗi tải dữ liệu báo cáo:', err)
    });
  }

  /**
   * Tải toàn bộ đơn phòng để phục vụ thuật toán lọc liên thông tương tác đồ thị
   */
  loadAllBookings(): void {
    this.bookingService.getAllBookings().subscribe({
      next: (data: any[]) => {
        this.allBookings = data;
        // Nếu chưa click lọc cụ thể, mặc định hiển thị toàn bộ đơn đặt phòng lên bảng
        if (!this.selectedDateLabel) {
          this.filteredBookings = [...data];
        }
      },
      error: (err: any) => console.error('Lỗi tải danh sách đặt phòng phụ:', err)
    });
  }

  /**
   * Thuật toán tự động tính toán các thông số quản trị khách sạn (Occupancy Rate & ADR)
   */
  calculateHotelKPIs(): void {
    const total = this.statsData.totalRooms || 1;
    const available = this.statsData.availableRooms || 0;
    const occupiedRooms = total - available;

    // 1. Công thức Tỷ lệ lấp đầy = (Số phòng đang ở / Tổng số phòng) * 100
    this.occupancyRate = (occupiedRooms / total) * 100;

    // 2. Công thức Giá trung bình mỗi phòng (ADR) = Doanh thu ngày / Số phòng đang phục vụ thực tế
    if (occupiedRooms > 0) {
      this.averageDailyRate = this.statsData.totalRevenueToday / occupiedRooms;
    } else {
      this.averageDailyRate = 0;
    }
  }

  onFilterChange(filter: 'DATE' | 'WEEK' | 'MONTH' | 'YEAR'): void {
    this.currentFilter = filter;
    this.selectedDateLabel = ''; // Xóa bộ lọc click cũ khi đổi Tab thời gian
    this.loadStats();
  }

  /**
   * Hàm bổ trợ ép nhảy tab tháng
   */
  clickMonthTab(): void {
    this.onFilterChange('MONTH');
  }

  /**
   * 🎨 KHỐI 1: VẼ BIỂU ĐỒ CỘT DOANH THU CÓ CHỨA SỰ KIỆN CLICK LIÊN THÔNG BẢNG DỮ LIỆU
   */
  renderRevenueChart(chartData: any): void {
    if (!chartData) return;
    const labels = Object.keys(chartData);
    const values = Object.values(chartData);

    if (this.revenueChartInstance) {
      this.revenueChartInstance.destroy();
    }

    setTimeout(() => {
      if (!this.revenueChart) return;
      const ctx = this.revenueChart.nativeElement.getContext('2d');
      
      this.revenueChartInstance = new Chart(ctx, {
        type: 'bar',
        data: {
          labels: labels,
          datasets: [{
            label: 'Doanh thu (VND)',
            data: values,
            backgroundColor: '#2563eb',
            hoverBackgroundColor: '#1d4ed8', // Đổi màu đậm hơn khi hover vào cột
            borderRadius: 8,
            maxBarThickness: 45
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: { legend: { display: false } },
          // 🎯 SỰ KIỆN CỐT LÕI: Lắng nghe hành động click vào thanh cột dữ liệu
          onClick: (event, elements) => {
            if (elements.length > 0) {
              const clickedElementIndex = elements[0].index;
              const dateLabel = this.revenueChartInstance.data.labels[clickedElementIndex];
              
              // Kích hoạt hàm lọc dữ liệu bảng phía dưới dựa theo nhãn ngày vừa click
              this.filterBookingsByChartDate(dateLabel);
            }
          },
          scales: {
            y: { beginAtZero: true, grid: { color: '#f1f5f9' } },
            x: { grid: { display: false } }
          }
        }
      });
    }, 50);
  }

  /**
   * 🎨 KHỐI 2: VẼ BIỂU ĐỒ TRÒN/VÒNG (DOUGHNUT) HIỆN THỊ TỶ LỆ LẤP ĐẦY PHÒNG
   */
  renderOccupancyChart(): void {
    const total = this.statsData.totalRooms || 1;
    const available = this.statsData.availableRooms || 0;
    const occupied = total - available;

    if (this.occupancyChartInstance) {
      this.occupancyChartInstance.destroy();
    }

    setTimeout(() => {
      if (!this.occupancyChart) return;
      const ctx = this.occupancyChart.nativeElement.getContext('2d');

      this.occupancyChartInstance = new Chart(ctx, {
        type: 'doughnut', // Cấu hình dạng vòng bánh Doughnut cao cấp
        data: {
          labels: ['Đang sử dụng (Occupied)', 'Phòng trống sẵn có (Available)'],
          datasets: [{
            data: [occupied, available],
            backgroundColor: ['#3b82f6', '#e2e8f0'], // Xanh dương biểu thị phòng có người, Xám nhạt biểu thị phòng trống
            borderWidth: 3,
            borderColor: '#ffffff',
            hoverOffset: 4
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: { display: false } // Ẩn legend hệ thống, dùng phần chú thích custom Tailwind ở dưới cho đẹp
          },
          cutout: '75%' // Tạo độ mảnh khảnh cho vòng biểu đồ tròn
        }
      });
    }, 50);
  }

  /**
   * 🎯 LOGIC LỌC LIÊN THÔNG: Phân tích nhãn biểu đồ để lọc danh sách đơn hàng tương ứng
   */
  filterBookingsByChartDate(label: string): void {
    this.selectedDateLabel = label;
    
    this.filteredBookings = this.allBookings.filter(b => {
      if (!b.checkInDate) return false;
      
      // Chuyển đổi chuỗi ngày checkInDate của đơn hàng (yyyy-MM-dd) sang dạng dd/MM hoặc cấu trúc text tương đương nhãn
      const bookingDate = new Date(b.checkInDate);
      const dayStr = String(bookingDate.getDate()).padStart(2, '0');
      const monthStr = String(bookingDate.getMonth() + 1).padStart(2, '0');
      const formattedBookingDate = `${dayStr}/${monthStr}`; // Cấu trúc định dạng "dd/MM"
      
      // Khớp theo Ngày/Tuần (Nhãn định dạng dạng dd/MM)
      if (this.currentFilter === 'WEEK' || this.currentFilter === 'DATE') {
        return formattedBookingDate === label;
      }
      
      // Khớp theo Năm (Nhãn định dạng dạng "Th. X")
      if (this.currentFilter === 'YEAR') {
        const monthLabel = `Th. ${bookingDate.getMonth() + 1}`;
        return monthLabel === label;
      }

      // Khớp theo Tháng (Nhãn định dạng dạng "Tuần X")
      if (this.currentFilter === 'MONTH') {
        // Thuật toán giả định phân tách tuần trong tháng để test khớp dữ liệu hiển thị
        return true; 
      }
      
      return true;
    });
  }

  /**
   * Xóa bộ lọc click ngày, trả bảng đơn đặt phòng về trạng thái hiển thị đầy đủ ban đầu
   */
  clearDateFilter(): void {
    this.selectedDateLabel = '';
    this.filteredBookings = [...this.allBookings];
  }
}