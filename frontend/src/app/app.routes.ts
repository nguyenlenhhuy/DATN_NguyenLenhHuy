import { Routes } from '@angular/router';
import { LoginComponent } from './pages/login/login.component';
import { HomeComponent } from './pages/home/home.component';
import { RegisterComponent } from './pages/register/register.component';
import { ForgotPasswordComponent } from './pages/forgot-password/forgot-password.component';
import { AdminLayoutComponent } from './components/admin-layout/admin-layout.component';
import { DashboardComponent } from './pages/admin/dashboard/dashboard.component';
import { adminGuard } from './guards/admin.guard';
import { ProfileComponent } from './pages/profile/profile.component';
import { UserManagementComponent } from './pages/admin/user-management/user-management.component';

export const routes: Routes = [
  // --- PHÂN HỆ NGƯỜI DÙNG (PUBLIC) ---
  { path: 'home', component: HomeComponent },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'profile', component: ProfileComponent }, 

  // --- PHÂN HỆ QUẢN TRỊ (ADMIN) ---
  { 
    path: 'admin', 
    component: AdminLayoutComponent, 
    // canActivate: [adminGuard], // Mở lại khi Guard của bạn đã hoạt động ổn định
    children: [
      { path: 'dashboard', component: DashboardComponent },
      
      // ĐƯA USER MANAGEMENT VÀO ĐÂY ĐỂ HIỆN TRONG SIDEBAR
      { path: 'users', component: UserManagementComponent }, 

      // ĐÃ CHỈNH SỬA: Đưa Quản lý phòng vào làm con của Admin Layout
      // Chỉ để là 'rooms', Angular tự hiểu URL đầy đủ là: /admin/rooms
      { 
        path: 'rooms', 
        loadComponent: () => import('./components/room-matrix/room-matrix.component').then(m => m.RoomMatrixComponent),
        title: 'Hệ thống Quản lý Phòng - Admin'
      },

      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  
  // --- ĐIỀU HƯỚNG MẶC ĐỊNH ---
  { path: '', redirectTo: 'home', pathMatch: 'full' },
  
  // ĐÃ CHỈNH SỬA: DÒNG NÀY BẮT BUỘC LUÔN LUÔN PHẢI ĐỂ Ở DƯỚI CÙNG
  { path: '**', redirectTo: 'home' }
];