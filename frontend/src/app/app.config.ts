import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { authInterceptor } from './interceptors/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    // Kích hoạt ma trận định tuyến chuẩn hóa
    provideRouter(routes),
    
    // Cung cấp một HttpClient duy nhất có gắn Interceptor xử lý JWT Token
    provideHttpClient(
      withInterceptors([authInterceptor])
    )
  ]
};