import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BookingManagementService, DashboardStatsDTO } from '../../services/booking-management.service';
import { Chart, registerables } from 'chart.js';
Chart.register(...registerables);

@Component({
  selector: 'app-overview',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './overview.component.html'
})
export class OverviewComponent implements OnInit, OnDestroy {
  currentFilter: 'DATE' | 'WEEK' | 'MONTH' | 'YEAR' = 'WEEK';

  statsData: DashboardStatsDTO = {
    totalRevenueToday: 0,
    totalRevenuePeriod: 0,
    totalBookingsToday: 0,
    availableRooms: 0,
    totalRooms: 0
  };

  allBookings: any[] = [];
  filteredBookings: any[] = [];
  selectedDateLabel: string = '';

  occupancyRate: number = 0;
  averageDailyRate: number = 0;

  // Occupancy calendar
  occupancyCalendar: Record<string, number> = {};
  calendarYear: number = new Date().getFullYear();
  calendarMonth: number = new Date().getMonth() + 1;
  calendarDays: { date: string; pct: number; day: number }[] = [];
  isLoadingCalendar = false;

  // Day detail panel
  selectedCalendarDate: string | null = null;
  dayDetail: any = null;
  isLoadingDayDetail = false;

  @ViewChild('revenueChart') revenueChart!: ElementRef;
  @ViewChild('occupancyChart') occupancyChart!: ElementRef;
  @ViewChild('dayDetailChart') dayDetailChart!: ElementRef;

  private revenueChartInstance: Chart | null = null;
  private occupancyChartInstance: Chart | null = null;
  private dayDetailChartInstance: Chart | null = null;
  private dayDetailTimer: ReturnType<typeof setTimeout> | null = null;
  private timeouts: ReturnType<typeof setTimeout>[] = [];

  constructor(private bookingService: BookingManagementService) {}

  ngOnInit(): void {
    this.loadStats();
    this.loadAllBookings();
    this.loadOccupancyCalendar();
  }

  ngOnDestroy(): void {
    this.timeouts.forEach(t => clearTimeout(t));
    if (this.dayDetailTimer) clearTimeout(this.dayDetailTimer);
    this.revenueChartInstance?.destroy();
    this.occupancyChartInstance?.destroy();
    this.dayDetailChartInstance?.destroy();
  }

  // ── Stats & charts ─────────────────────────────────────────────────────────

  loadStats(): void {
    this.bookingService.getDashboardStats(this.currentFilter).subscribe({
      next: (res) => {
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
      next: (data) => {
        this.allBookings = data;
        if (!this.selectedDateLabel) this.filteredBookings = [...data];
      },
      error: (err) => console.error('Lỗi tải danh sách đặt phòng:', err)
    });
  }

  calculateHotelKPIs(): void {
    const total = this.statsData.totalRooms || 1;
    const available = this.statsData.availableRooms || 0;
    const occupied = total - available;
    this.occupancyRate = (occupied / total) * 100;
    // Fix Bug 4: ADR dùng totalRevenuePeriod thay vì totalRevenueToday cố định
    this.averageDailyRate = occupied > 0 ? (this.statsData.totalRevenuePeriod / occupied) : 0;
  }

  onFilterChange(filter: 'DATE' | 'WEEK' | 'MONTH' | 'YEAR'): void {
    this.currentFilter = filter;
    this.selectedDateLabel = '';
    this.filteredBookings = [...this.allBookings];
    this.loadStats();
  }

  renderRevenueChart(chartData: Record<string, number> | undefined): void {
    if (!chartData) return;
    this.revenueChartInstance?.destroy();
    const labels = Object.keys(chartData);
    const values = Object.values(chartData);

    const timer = setTimeout(() => {
      if (!this.revenueChart) return;
      const ctx = this.revenueChart.nativeElement.getContext('2d');
      this.revenueChartInstance = new Chart(ctx, {
        type: 'bar',
        data: {
          labels,
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
          onClick: (_e, elements) => {
            if (elements.length > 0 && this.revenueChartInstance) {
              const idx = elements[0].index;
              const label = this.revenueChartInstance.data.labels?.[idx] as string;
              this.filterBookingsByChartDate(label);
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

    this.occupancyChartInstance?.destroy();
    const timer = setTimeout(() => {
      if (!this.occupancyChart) return;
      const ctx = this.occupancyChart.nativeElement.getContext('2d');
      this.occupancyChartInstance = new Chart(ctx, {
        type: 'doughnut',
        data: {
          labels: ['Đang sử dụng', 'Còn trống'],
          datasets: [{
            data: [occupied, available],
            backgroundColor: ['#10b981', '#f1f5f9'],
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
      if (!b.checkInDate || !b.checkOutDate) return false;
      const checkIn  = new Date(b.checkInDate);
      const checkOut = new Date(b.checkOutDate);

      if (this.currentFilter === 'WEEK' || this.currentFilter === 'DATE') {
        // label = "dd/MM" — booking đang hoạt động (checkIn ≤ ngày ≤ checkOut)
        const [dd, mm] = label.split('/').map(Number);
        const labelDate = new Date(new Date().getFullYear(), mm - 1, dd);
        return checkIn <= labelDate && checkOut > labelDate;
      }

      if (this.currentFilter === 'YEAR') {
        // label = "Th. N" — booking overlap tháng đó (12 tháng gần nhất)
        const monthNum = parseInt(label.replace('Th. ', ''), 10);
        const today = new Date();
        const currentMonth = today.getMonth() + 1;
        const year = monthNum <= currentMonth ? today.getFullYear() : today.getFullYear() - 1;
        const mStart = new Date(year, monthNum - 1, 1);
        const mEnd   = new Date(year, monthNum, 0); // ngày cuối tháng
        return checkIn <= mEnd && checkOut >= mStart;
      }

      if (this.currentFilter === 'MONTH') {
        // label = "Tuần N" — booking overlap tuần calendar (đồng bộ backend)
        const weekNum = parseInt(label.replace('Tuần ', ''), 10);
        const year = new Date().getFullYear();
        const month = new Date().getMonth();
        const daysInMonth = new Date(year, month + 1, 0).getDate();
        const weekStart = new Date(year, month, (weekNum - 1) * 7 + 1);
        const weekEnd   = weekNum === 4
          ? new Date(year, month, daysInMonth)
          : new Date(year, month, weekNum * 7);
        return checkIn <= weekEnd && checkOut >= weekStart;
      }
      return true;
    });
  }

  clearDateFilter(): void {
    this.selectedDateLabel = '';
    this.filteredBookings = [...this.allBookings];
  }

  // ── Occupancy calendar ─────────────────────────────────────────────────────

  loadOccupancyCalendar(): void {
    this.isLoadingCalendar = true;
    this.bookingService.getOccupancyCalendar(this.calendarYear, this.calendarMonth).subscribe({
      next: (data) => {
        this.occupancyCalendar = data;
        this.buildCalendarDays();
        this.isLoadingCalendar = false;
      },
      error: () => { this.isLoadingCalendar = false; }
    });
  }

  buildCalendarDays(): void {
    // Sort để đảm bảo thứ tự ngày đúng bất kể thứ tự key trong JSON
    this.calendarDays = Object.entries(this.occupancyCalendar)
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([date, pct]) => ({
        date,
        pct,
        day: parseInt(date.split('-')[2], 10)
      }));
  }

  selectCalendarDay(cell: { date: string; pct: number; day: number }): void {
    // Luôn dọn sạch chart cũ và timer trước khi đổi trạng thái
    if (this.dayDetailTimer) {
      clearTimeout(this.dayDetailTimer);
      this.dayDetailTimer = null;
    }
    this.dayDetailChartInstance?.destroy();
    this.dayDetailChartInstance = null;

    if (this.selectedCalendarDate === cell.date) {
      this.selectedCalendarDate = null;
      this.dayDetail = null;
      return;
    }
    this.selectedCalendarDate = cell.date;
    this.dayDetail = null;
    this.isLoadingDayDetail = true;
    this.bookingService.getOccupancyDayDetail(cell.date).subscribe({
      next: (data) => {
        this.dayDetail = data;
        this.isLoadingDayDetail = false;
        // Canvas luôn có trong DOM (dùng [hidden] thay vì *ngIf) — không cần detectChanges
        this.renderDayDetailChart(data);
      },
      error: () => { this.isLoadingDayDetail = false; }
    });
  }

  closeDayDetail(): void {
    if (this.dayDetailTimer) {
      clearTimeout(this.dayDetailTimer);
      this.dayDetailTimer = null;
    }
    this.dayDetailChartInstance?.destroy();
    this.dayDetailChartInstance = null;
    this.selectedCalendarDate = null;
    this.dayDetail = null;
  }

  renderDayDetailChart(data: any): void {
    if (this.dayDetailTimer) {
      clearTimeout(this.dayDetailTimer);
      this.dayDetailTimer = null;
    }
    // Canvas đã có trong DOM do [hidden] — chờ Angular unhide xong rồi render
    this.dayDetailTimer = setTimeout(() => {
      if (!this.dayDetailChart) return;
      const canvas = this.dayDetailChart.nativeElement;
      // Flush Chart.js registry cho canvas này phòng trường hợp còn sót
      const existing = Chart.getChart(canvas);
      if (existing) existing.destroy();

      const ctx = canvas.getContext('2d');
      this.dayDetailChartInstance = new Chart(ctx, {
        type: 'doughnut',
        data: {
          labels: ['Đang ở (CHECK_IN)', 'Đã xác nhận (CONFIRMED)', 'Đã trả phòng (CHECK_OUT)', 'Còn trống'],
          datasets: [{
            data: [data.checkInCount, data.confirmedCount, data.checkOutCount, data.availableRooms],
            backgroundColor: ['#10b981', '#3b82f6', '#94a3b8', '#e2e8f0'],
            borderWidth: 2,
            borderColor: '#ffffff',
            hoverOffset: 6
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          cutout: '65%',
          plugins: {
            legend: {
              position: 'bottom',
              labels: { font: { size: 11 }, padding: 12, boxWidth: 12 }
            }
          }
        }
      });
    }, 50);
  }

  prevCalendarMonth(): void {
    if (this.calendarMonth === 1) { this.calendarMonth = 12; this.calendarYear--; }
    else this.calendarMonth--;
    this.loadOccupancyCalendar();
  }

  nextCalendarMonth(): void {
    if (this.calendarMonth === 12) { this.calendarMonth = 1; this.calendarYear++; }
    else this.calendarMonth++;
    this.loadOccupancyCalendar();
  }

  get calendarMonthLabel(): string {
    return new Date(this.calendarYear, this.calendarMonth - 1, 1)
      .toLocaleDateString('vi-VN', { month: 'long', year: 'numeric' });
  }

  // Trả về lớp CSS màu theo % công suất
  occupancyColorClass(pct: number): string {
    if (pct === 0)   return 'bg-slate-100 text-slate-400';
    if (pct < 30)    return 'bg-emerald-100 text-emerald-700';
    if (pct < 60)    return 'bg-yellow-100 text-yellow-700';
    if (pct < 90)    return 'bg-orange-100 text-orange-700';
    return 'bg-rose-100 text-rose-700';
  }

  // Số ngày trống ở đầu tháng (thứ Hai = 0)
  get calendarLeadingBlanks(): number[] {
    const firstDay = new Date(this.calendarYear, this.calendarMonth - 1, 1).getDay();
    // Chuyển từ Sun=0 sang Mon=0
    return Array(firstDay === 0 ? 6 : firstDay - 1).fill(0);
  }

  get filterLabel(): string {
    const map: Record<string, string> = {
      DATE: '3 ngày gần nhất', WEEK: '7 ngày qua', MONTH: 'Tháng này', YEAR: '12 tháng'
    };
    return map[this.currentFilter] || '';
  }
}
