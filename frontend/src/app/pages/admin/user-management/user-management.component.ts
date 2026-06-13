import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { UserAdminService } from '../../../services/user-admin.service';
import { AuthService } from '../../../services/auth.service';
import { User } from '../../../models/user.model';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './user-management.component.html'
})
export class UserManagementComponent implements OnInit {
  private userAdminService = inject(UserAdminService);
  private authService = inject(AuthService);
  private router = inject(Router);

  // Danh sách dữ liệu
  users: User[] = [];
  filteredUsers: User[] = [];
  
  // Thông tin người dùng đang đăng nhập
  loggedInUser: any = null;

  // Trạng thái giao diện
  isLoading: boolean = false;
  showAddModal: boolean = false;
  
  // Các biến phục vụ tìm kiếm và lọc
  searchTerm: string = '';
  filterRole: string = 'ALL';
  filterStatus: string = 'ALL';

  // Đối tượng nhân viên mới
  newStaff = {
    username: '',
    email: '',
    fullName: '',
    password: '',
    phone: ''
  };

  // ngOnInit duy nhất: Khởi tạo dữ liệu khi component được load
  ngOnInit(): void {
    this.fetchUsers();
    this.loadLoggedInUser();
  }

  // Lấy thông tin admin đang đăng nhập từ LocalStorage
  loadLoggedInUser(): void {
    const userData = localStorage.getItem('user');
    if (userData) {
      this.loggedInUser = JSON.parse(userData);
    }
  }

  // Tải danh sách người dùng từ Server
  fetchUsers(): void {
    this.isLoading = true;
    this.userAdminService.getAllUsers().subscribe({
      next: (res: User[]) => {
        // Chỉ lấy những user chưa bị xóa mềm
        this.users = res.filter(u => !u.isDeleted);
        this.applyFilter();
        this.isLoading = false;
      },
      error: (err: any) => {
        console.error('Lỗi tải dữ liệu:', err);
        this.isLoading = false;
      }
    });
  }

  // Xử lý lọc dữ liệu tại chỗ (Frontend filter)
  applyFilter(): void {
    this.filteredUsers = this.users.filter(user => {
      const matchSearch = user.fullName.toLowerCase().includes(this.searchTerm.toLowerCase()) || 
                          user.email.toLowerCase().includes(this.searchTerm.toLowerCase());
      const matchRole = this.filterRole === 'ALL' || user.role.roleType === this.filterRole;
      const matchStatus = this.filterStatus === 'ALL' || user.status === this.filterStatus;
      return matchSearch && matchRole && matchStatus;
    });
  }

  // Thêm nhân viên mới
  onAddStaff(): void {
    this.newStaff.username = this.newStaff.email; 
    this.userAdminService.createStaff(this.newStaff).subscribe({
      next: (msg: string) => {
        alert(msg);
        this.showAddModal = false;
        this.fetchUsers(); 
        this.resetNewStaffForm();
      },
      error: (err: any) => {
        console.error(err);
        alert('Lỗi khi thêm nhân viên! Vui lòng kiểm tra lại dữ liệu.');
      }
    });
  }

  // Thay đổi quyền (Role) trực tiếp trên bảng
  onChangeRole(user: User, newRole: string): void {
    this.userAdminService.updateRole(user.id, newRole).subscribe({
      next: (msg: string) => {
        alert(msg);
        user.role.roleType = newRole as any;
        this.applyFilter(); // Cập nhật lại danh sách hiển thị
      },
      error: (err: any) => alert('Lỗi: ' + err.error)
    });
  }

  // Khóa hoặc mở khóa tài khoản
  onToggleStatus(user: User): void {
    const newStatus = user.status === 'ACTIVE' ? 'LOCKED' : 'ACTIVE';
    this.userAdminService.updateStatus(user.id, newStatus).subscribe({
      next: (msg: string) => {
        alert(msg);
        user.status = newStatus as any;
        this.applyFilter();
      },
      error: (err: any) => alert('Lỗi cập nhật trạng thái!')
    });
  }

  // Xóa tài khoản (Vô hiệu hóa)
  onDelete(id: number): void {
    if (confirm('Xác nhận ngừng hoạt động tài khoản này?')) {
      this.userAdminService.deactivateUser(id).subscribe({
        next: (msg: string) => {
          alert(msg);
          this.fetchUsers();
        },
        error: (err: any) => alert('Lỗi: ' + err.error)
      });
    }
  }

  logout(): void {
    if (!confirm('Bạn có chắc chắn muốn đăng xuất?')) return;
    this.authService.logout();
  }

  private resetNewStaffForm(): void {
    this.newStaff = { username: '', email: '', fullName: '', password: '', phone: '' };
  }
}