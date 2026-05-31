import { Routes } from '@angular/router';
import { LoginComponent } from './pages/login/login.component';
import { HomeComponent } from './pages/home/home.component';
import { RegisterComponent } from './pages/register/register.component';
import { ForgotPasswordComponent } from './pages/forgot-password/forgot-password.component';
import { AdminLayoutComponent } from './components/admin-layout/admin-layout.component';
import { ProfileComponent } from './pages/profile/profile.component';
import { UserManagementComponent } from './pages/admin/user-management/user-management.component';
import { RoomsComponent } from './pages/rooms/rooms.component';

// IMPORT CÁC COMPONENT QUẢN TRỊ ĐỒNG BỘ CHUẨN KIẾN TRÚC
import { OverviewComponent } from './components/overview/overview.component';
import { BookingManagementComponent } from './components/booking-management/booking-management.component';
import { PromotionManagementComponent } from './components/promotion-management/promotion-management.component';

// Guard kiểm tra đặc quyền ADMIN cao cấp
import { adminGuard } from './guards/admin.guard'; 
import { FavoritesComponent } from './pages/favorites/favorites.component';
import { RoomDetailComponent } from './pages/room-detail/room-detail.component';

export const routes: Routes = [
  // ===========================================================================
  // 1. PHÂN HỆ NGƯỜI DÙNG & CÔNG KHAI (PUBLIC / CUSTOMER ROUTES)
  // ===========================================================================
  { path: 'home', component: HomeComponent },
  { path: 'rooms', component: RoomsComponent },
  { path: 'rooms/:id', component: RoomDetailComponent }, // ĐƯA DÒNG NÀY LÊN ĐÂY (Cạnh route 'rooms' cho dễ quản lý)
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'profile', component: ProfileComponent }, 
  { path: 'favorites', component: FavoritesComponent }, // ĐƯA DÒNG NÀY LÊN PHÂN HỆ PUBLIC luôn

  // ===========================================================================
  // 2. PHÂN HỆ QUẢN TRỊ & VẬN HÀNH KHÁCH SẠN (ADMIN & STAFF PANEL)
  // ===========================================================================
  { 
    path: 'admin', 
    component: AdminLayoutComponent, 
    children: [
      { 
        path: 'overview', 
        component: OverviewComponent,
        title: 'Tổng Quan Báo Cáo Doanh Thu'
      },
      { 
        path: 'users', 
        component: UserManagementComponent,
        canActivate: [adminGuard]
      }, 
      { 
        path: 'rooms', 
        loadComponent: () => import('./components/room-matrix/room-matrix.component').then(m => m.RoomMatrixComponent),
        canActivate: [adminGuard],
        title: 'Sơ đồ Ma trận Phòng - Hệ thống Admin'
      },
      { 
        path: 'booking-management', 
        component: BookingManagementComponent,
        title: 'Quản lý Đặt phòng & Thu ngân Quầy'
      },
      { 
        path: 'promotions', 
        component: PromotionManagementComponent,
        canActivate: [adminGuard],
        title: 'Quản lý Chiến dịch Khuyến mãi - Admin'
      },
      { path: '', redirectTo: 'overview', pathMatch: 'full' }
    ]
  },
  
  // ===========================================================================
  // 3. ĐIỀU HƯỚNG MẶC ĐỊNH & BẢO VỆ ĐƯỜNG DẪN SAI (FALLBACK ROUTES)
  // ===========================================================================
  { path: '', redirectTo: 'home', pathMatch: 'full' },
  { path: '**', redirectTo: 'home' } // BẮT BUỘC PHẢI LÀ DÒNG CUỐI CÙNG CỦA MẢNG!
];