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
    // canActivate: [adminGuard], // Bạn có thể mở lại khi Guard đã ổn
    children: [
      { path: 'dashboard', component: DashboardComponent },
      // ĐƯA USER MANAGEMENT VÀO ĐÂY ĐỂ HIỆN TRONG SIDEBAR
      { path: 'users', component: UserManagementComponent }, 
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  
  // --- ĐIỀU HƯỚNG MẶC ĐỊNH ---
  { path: '', redirectTo: 'home', pathMatch: 'full' },
  // DÒNG NÀY LUÔN LUÔN PHẢI ĐỂ Ở CUỐI CÙNG
  { path: '**', redirectTo: 'home' }
];