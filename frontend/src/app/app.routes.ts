import { Routes } from '@angular/router';
import { LoginComponent } from './pages/login/login.component';
import { HomeComponent } from './pages/home/home.component';
import { RegisterComponent } from './pages/register/register.component';
import { ForgotPasswordComponent } from './pages/forgot-password/forgot-password.component';
import { AdminLayoutComponent } from './components/admin-layout/admin-layout.component';
import { DashboardComponent } from './pages/admin/dashboard/dashboard.component';
import { ProfileComponent } from './pages/profile/profile.component';
import { UserManagementComponent } from './pages/admin/user-management/user-management.component';

// IMPORT 2 COMPONENT MỚI ĐÃ ĐƯỢC TÁCH RA RIÊNG BIỆT
import { BookingManagementComponent } from './components/booking-management/booking-management.component';
import { PromotionManagementComponent } from './components/promotion-management/promotion-management.component';

// Guard kiểm tra đặc quyền ADMIN cao cấp
import { adminGuard } from './guards/admin.guard'; 

export const routes: Routes = [
  // ===========================================================================
  // 1. PHÂN HỆ NGƯỜI DÙNG & CÔNG KHAI (PUBLIC / CUSTOMER ROUTES)
  // ===========================================================================
  { path: 'home', component: HomeComponent },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'profile', component: ProfileComponent }, 

  // ===========================================================================
  // 2. PHÂN HỆ QUẢN TRỊ & VẬN HÀNH KHÁCH SẠN (ADMIN & STAFF PANEL)
  // ===========================================================================
  { 
    path: 'admin', 
    component: AdminLayoutComponent, 
    // KHÔNG khóa adminGuard ở đây nữa để tài khoản STAFF (Lễ tân) cũng vào được Layout chung
    children: [
      // Màn hình tổng quan Dashboard (Cho phép cả hai hoặc khóa tùy bạn, ở đây tạm thời cho cả hai xem báo cáo)
      { path: 'dashboard', component: DashboardComponent },
      
      // 🔐 CHỈ ADMIN: Quản lý tài khoản User hệ thống
      { 
        path: 'users', 
        component: UserManagementComponent,
        canActivate: [adminGuard] // Chặn chặt nhân viên lễ tân vào chỉnh sửa tài khoản
      }, 

      // 🔐 CHỈ ADMIN: Sơ đồ ma trận quản lý hạ tầng phòng
      { 
        path: 'rooms', 
        loadComponent: () => import('./components/room-matrix/room-matrix.component').then(m => m.RoomMatrixComponent),
        canActivate: [adminGuard], // Chặn nhân viên lễ tân thay đổi cấu hình phòng vật lý
        title: 'Sơ đồ Ma trận Phòng - Hệ thống Admin'
      },

      // 🛎️ CẢ ADMIN & STAFF: Phân hệ Quản lý đặt phòng & Nghiệp vụ Quầy
      // Đường dẫn URL truy cập: /admin/bookings
      { 
        path: 'bookings', 
        component: BookingManagementComponent,
        title: 'Quản lý Đặt phòng & Thu ngân Quầy'
      },

      // 🔐 CHỈ ADMIN: Phân hệ Quản lý và cấu hình mã Khuyến mãi (Voucher)
      // Đường dẫn URL truy cập: /admin/promotions
      { 
        path: 'promotions', 
        component: PromotionManagementComponent,
        canActivate: [adminGuard], // Khóa chặt, STAFF gõ URL này sẽ bị đá ra ngoài ngay lập tức!
        title: 'Quản lý Chiến dịch Khuyến mãi - Admin'
      },

      // Nếu chỉ gõ /admin -> Tự động hướng vào màn hình Dashboard
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  
  // ===========================================================================
  // 3. ĐIỀU HƯỚNG MẶC ĐỊNH & BẢO VỆ ĐƯỜNG DẪN SAI (FALLBACK ROUTES)
  // ===========================================================================
  { path: '', redirectTo: 'home', pathMatch: 'full' },
  { path: '**', redirectTo: 'home' }
];