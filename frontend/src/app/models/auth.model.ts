export interface LoginRequest {
  username: string;
  password:  string;
}

export interface RegisterRequest {
  email: string;
  fullName: string;
  phone: string;
  password: string;
}

export interface VerifyOtpRequest {
  email: string;
  otpCode: string;
}

export interface UpdateEmailRequest {
  phone: string;
  newEmail: string;
}

export interface AuthResponse {
  token: string;
  type: string;
  username: string;
  role: string; // 'ADMIN' | 'STAFF' | 'CUSTOMER'
}