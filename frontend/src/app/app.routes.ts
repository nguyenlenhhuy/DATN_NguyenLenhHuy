import { Routes } from '@angular/router';
import { LoginComponent } from './pages/login/login.component';
import { HomeComponent } from './pages/home/home.component';
import { RegisterComponent } from './pages/register/register.component';
import { ForgotPasswordComponent } from './pages/forgot-password/forgot-password.component';
import { AdminLayoutComponent } from './components/admin-layout/admin-layout.component';
import { DashboardComponent } from './pages/admin/dashboard/dashboard.component';
import { adminGuard } from './guards/admin.guard';
import { ProfileComponent } from './pages/profile/profile.component';

export const routes: Routes = [
  // --- PHÂN HỆ NGƯỜI DÙNG (PUBLIC) ---
  { path: 'home', component: HomeComponent },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'profile', component: ProfileComponent }, 

  // --- PHÂN HỆ QUẢN TRỊ (ADMIN) ---
  // Được bảo vệ chặt chẽ bởi Guard dựa trên Role từ Token
  { 
    path: 'admin', 
    component: AdminLayoutComponent, 
    canActivate: [adminGuard], 
    children: [
      { path: 'dashboard', component: DashboardComponent },
      // Huy có thể thêm các route quản lý phòng, khách sạn tại đây
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  
  // --- ĐIỀU HƯỚNG MẶC ĐỊNH ---
  { path: '', redirectTo: 'home', pathMatch: 'full' },
  { path: '**', redirectTo: 'home' } // Redirect các đường dẫn lạ về Trang chủ
];