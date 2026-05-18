export interface User {
  id: number;
  username: string;
  email: string;
  fullName: string;
  phone: string;
  role: {
    id: number;
    roleName: string;
    roleType: string;
  };
  status: 'PENDING' | 'ACTIVE' | 'LOCKED';
  isDeleted: boolean;
  createdAt: string;
}