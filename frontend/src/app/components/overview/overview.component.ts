import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BookingManagementService } from '../../services/booking-management.service';
import { Chart, registerables } from 'chart.js';
Chart.register(...registerables);

interface DashboardStats {
  totalRevenueToday: number;
  totalBookingsToday: number;
  availableRooms: number;
  totalRooms: number;
  last7DaysRevenue?: Record<string, number>;
}

@Component({
  selector: 'app-overview',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './overview.component.html'
})
export class OverviewComponent implements OnInit, OnDestroy {
  currentFilter: 'DATE' | 'WEEK' | 'MONTH' | 'YEAR' = 'WEEK';
  
  statsData: DashboardStats = {
    totalRevenueToday: 0,
    totalBookingsToday: 0,
    availableRooms: 0,
    totalRooms: 0
  };

  allBookings: any[] = [];
  filteredBookings: any[] = [];
  selectedDateLabel: string = '';

  occupancyRate: number = 0;
  averageDailyRate: number = 0;

  @ViewChild('revenueChart') revenueChart!: ElementRef;
  @ViewChild('occupancyChart') occupancyChart!: ElementRef;
  
  private revenueChartInstance: Chart | null = null;
  private occupancyChartInstance: Chart | null = null;
  private timeouts: any[] = [];

  constructor(private bookingService: BookingManagementService) {}

  ngOnInit(): void {
    this.loadStats();
    this.loadAllBookings();
  }

  ngOnDestroy(): void {
    // Dọn dẹp triệt để tránh rò rỉ tài nguyên hệ thống
    this.timeouts.forEach(t => clearTimeout(t));
    if (this.revenueChartInstance) this.revenueChartInstance.destroy();
    if (this.occupancyChartInstance) this.occupancyChartInstance.destroy();
  }

  loadStats(): void {
    this.bookingService.getDashboardStats(this.currentFilter).subscribe({
      next: (res: DashboardStats) => {
        this.statsData = res;
        this.calculateHotelKPIs();
        this.renderRevenueChart(res.last7DaysRevenue);
        this.renderOccupancyChart();
      },
      error: (err) => console.error('Lỗi tải dữ liệu báo cáo:', err)
    });
  }

  loadAllBookings(): void {
    this.bookingService.getAllBookings().subscribe({
      next: (data: any[]) => {
        this.allBookings = data;
        if (!this.selectedDateLabel) {
          this.filteredBookings = [...data];
        }
      },
      error: (err) => console.error('Lỗi tải danh sách đặt phòng:', err)
    });
  }

  calculateHotelKPIs(): void {
    const total = this.statsData.totalRooms || 1;
    const available = this.statsData.availableRooms || 0;
    const occupiedRooms = total - available;

    this.occupancyRate = (occupiedRooms / total) * 100;
    this.averageDailyRate = occupiedRooms > 0 ? (this.statsData.totalRevenueToday / occupiedRooms) : 0;
  }

  onFilterChange(filter: 'DATE' | 'WEEK' | 'MONTH' | 'YEAR'): void {
    this.currentFilter = filter;
    this.selectedDateLabel = '';
    this.loadStats();
  }

  renderRevenueChart(chartData: Record<string, number> | undefined): void {
    if (!chartData) return;
    const labels = Object.keys(chartData);
    const values = Object.values(chartData);

    if (this.revenueChartInstance) {
      this.revenueChartInstance.destroy();
    }

    const timer = setTimeout(() => {
      if (!this.revenueChart) return;
      const ctx = this.revenueChart.nativeElement.getContext('2d');
      
      this.revenueChartInstance = new Chart(ctx, {
        type: 'bar',
        data: {
          labels: labels,
          datasets: [{
            label: 'Doanh thu (VND)',
            data: values,
            backgroundColor: '#3b82f6',
            hoverBackgroundColor: '#1d4ed8',
            borderRadius: 6,
            maxBarThickness: 40
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: { legend: { display: false } },
          onClick: (event, elements) => {
            if (elements.length > 0 && this.revenueChartInstance) {
              const clickedElementIndex = elements[0].index;
              const dateLabel = this.revenueChartInstance.data.labels?.[clickedElementIndex] as string;
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
    this.timeouts.push(timer);
  }

  renderOccupancyChart(): void {
    const total = this.statsData.totalRooms || 1;
    const available = this.statsData.availableRooms || 0;
    const occupied = total - available;

    if (this.occupancyChartInstance) {
      this.occupancyChartInstance.destroy();
    }

    const timer = setTimeout(() => {
      if (!this.occupancyChart) return;
      const ctx = this.occupancyChart.nativeElement.getContext('2d');

      this.occupancyChartInstance = new Chart(ctx, {
        type: 'doughnut',
        data: {
          labels: ['Đang sử dụng', 'Còn trống'],
          datasets: [{
            data: [occupied, available],
            backgroundColor: ['#10b981', '#f1f5f9'], // Đổi màu xanh lá cho cảm giác an toàn và trống trải màu xám
            borderWidth: 2,
            borderColor: '#ffffff',
            hoverOffset: 4
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: { legend: { display: false } },
          cutout: '70%'
        }
      });
    }, 50);
    this.timeouts.push(timer);
  }

  filterBookingsByChartDate(label: string): void {
    this.selectedDateLabel = label;
    
    this.filteredBookings = this.allBookings.filter(b => {
      if (!b.checkInDate) return false;
      const bookingDate = new Date(b.checkInDate);
      
      if (this.currentFilter === 'WEEK' || this.currentFilter === 'DATE') {
        const dayStr = String(bookingDate.getDate()).padStart(2, '0');
        const monthStr = String(bookingDate.getMonth() + 1).padStart(2, '0');
        return `${dayStr}/${monthStr}` === label;
      }
      
      if (this.currentFilter === 'YEAR') {
        return `Th. ${bookingDate.getMonth() + 1}` === label;
      }

      if (this.currentFilter === 'MONTH') {
        // Giải thuật thực tế tính tuần trong tháng: Lấy ngày chia cho 7
        const date = bookingDate.getDate();
        const weekNum = Math.ceil(date / 7);
        return `Tuần ${weekNum}` === label;
      }
      
      return true;
    });
  }

  clearDateFilter(): void {
    this.selectedDateLabel = '';
    this.filteredBookings = [...this.allBookings];
  }
}