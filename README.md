
```
Hotel_Booking_Project
├─ backend
│  ├─ .idea
│  │  ├─ compiler.xml
│  │  ├─ dataSources
│  │  ├─ dataSources.local.xml
│  │  ├─ dataSources.xml
│  │  ├─ encodings.xml
│  │  ├─ jarRepositories.xml
│  │  ├─ misc.xml
│  │  ├─ vcs.xml
│  │  └─ workspace.xml
│  ├─ bin
│  │  ├─ default
│  │  ├─ generated-sources
│  │  │  └─ annotations
│  │  ├─ generated-test-sources
│  │  │  └─ annotations
│  │  ├─ main
│  │  │  ├─ application.properties
│  │  │  ├─ org
│  │  │  │  └─ example
│  │  │  │     └─ backend
│  │  │  │        ├─ BackendApplication.class
│  │  │  │        ├─ config
│  │  │  │        │  ├─ ApplicationConfig.class
│  │  │  │        │  ├─ JwtFilter.class
│  │  │  │        │  ├─ PayOSConfig.class
│  │  │  │        │  └─ SecurityConfig.class
│  │  │  │        ├─ controller
│  │  │  │        │  ├─ AdminDashboardController.class
│  │  │  │        │  ├─ AdminUserController.class
│  │  │  │        │  ├─ AuthController.class
│  │  │  │        │  ├─ BookingController.class
│  │  │  │        │  ├─ ReviewController.class
│  │  │  │        │  ├─ RoomController.class
│  │  │  │        │  ├─ RoomManagementController.class
│  │  │  │        │  ├─ RoomTypeController.class
│  │  │  │        │  └─ UserController.class
│  │  │  │        ├─ dto
│  │  │  │        │  ├─ request
│  │  │  │        │  │  ├─ AdminCreateUserRequest$AdminCreateUserRequestBuilder.class
│  │  │  │        │  │  ├─ AdminCreateUserRequest.class
│  │  │  │        │  │  ├─ BookingRequest.class
│  │  │  │        │  │  ├─ ChangePasswordRequest.class
│  │  │  │        │  │  ├─ ForgotPasswordRequest.class
│  │  │  │        │  │  ├─ LoginRequest.class
│  │  │  │        │  │  ├─ RegisterRequest.class
│  │  │  │        │  │  ├─ ResetPasswordRequest.class
│  │  │  │        │  │  ├─ ReviewRequestDTO.class
│  │  │  │        │  │  ├─ RoomImageRequest.class
│  │  │  │        │  │  ├─ RoomRequest.class
│  │  │  │        │  │  ├─ RoomSearchRequest.class
│  │  │  │        │  │  ├─ RoomTypeImageRequest.class
│  │  │  │        │  │  ├─ RoomTypeRequest.class
│  │  │  │        │  │  ├─ UpdateEmailRequest.class
│  │  │  │        │  │  └─ VerifyOtpRequest.class
│  │  │  │        │  └─ response
│  │  │  │        │     ├─ AuthResponse.class
│  │  │  │        │     ├─ BookingHistoryResponseDTO$BookingHistoryResponseDTOBuilder.class
│  │  │  │        │     ├─ BookingHistoryResponseDTO.class
│  │  │  │        │     ├─ BookingStatusProjection.class
│  │  │  │        │     ├─ CustomerRoomResponseDTO.class
│  │  │  │        │     ├─ RevenueTrendProjection.class
│  │  │  │        │     ├─ ReviewResponseDTO$ReviewResponseDTOBuilder.class
│  │  │  │        │     ├─ ReviewResponseDTO.class
│  │  │  │        │     ├─ RoomResponseDTO$RoomResponseDTOBuilder.class
│  │  │  │        │     ├─ RoomResponseDTO.class
│  │  │  │        │     ├─ RoomTypeDetailResponse$RoomTypeDetailResponseBuilder.class
│  │  │  │        │     ├─ RoomTypeDetailResponse.class
│  │  │  │        │     ├─ UserProfileResponse$UserProfileResponseBuilder.class
│  │  │  │        │     ├─ UserProfileResponse.class
│  │  │  │        │     ├─ UserResponse$UserResponseBuilder.class
│  │  │  │        │     └─ UserResponse.class
│  │  │  │        ├─ entity
│  │  │  │        │  ├─ AuditLog.class
│  │  │  │        │  ├─ Booking.class
│  │  │  │        │  ├─ BookingDetail.class
│  │  │  │        │  ├─ DailyRevenueStat$DailyRevenueStatBuilder.class
│  │  │  │        │  ├─ DailyRevenueStat.class
│  │  │  │        │  ├─ enums
│  │  │  │        │  │  ├─ BookingStatus.class
│  │  │  │        │  │  ├─ InvoiceStatus.class
│  │  │  │        │  │  ├─ MediaType.class
│  │  │  │        │  │  ├─ OtpType.class
│  │  │  │        │  │  ├─ PaymentMethod.class
│  │  │  │        │  │  ├─ PaymentStatus.class
│  │  │  │        │  │  ├─ RoleType.class
│  │  │  │        │  │  ├─ RoomStatus.class
│  │  │  │        │  │  └─ UserStatus.class
│  │  │  │        │  ├─ Hotel.class
│  │  │  │        │  ├─ Invoice.class
│  │  │  │        │  ├─ OtpStorage.class
│  │  │  │        │  ├─ Review.class
│  │  │  │        │  ├─ ReviewMedia.class
│  │  │  │        │  ├─ Role.class
│  │  │  │        │  ├─ Room.class
│  │  │  │        │  ├─ RoomImage.class
│  │  │  │        │  ├─ RoomPrice.class
│  │  │  │        │  ├─ RoomType.class
│  │  │  │        │  ├─ RoomTypeImage.class
│  │  │  │        │  ├─ User$UserBuilder.class
│  │  │  │        │  └─ User.class
│  │  │  │        ├─ exception
│  │  │  │        │  ├─ AppException.class
│  │  │  │        │  ├─ GlobalExceptionHandler.class
│  │  │  │        │  ├─ ResourceNotFoundException.class
│  │  │  │        │  └─ ReviewAlreadyExistsException.class
│  │  │  │        ├─ repository
│  │  │  │        │  ├─ AuditLogRepository.class
│  │  │  │        │  ├─ BookingDetailRepository.class
│  │  │  │        │  ├─ BookingRepository.class
│  │  │  │        │  ├─ DailyRevenueStatRepository.class
│  │  │  │        │  ├─ DashboardRepository.class
│  │  │  │        │  ├─ HotelRepository.class
│  │  │  │        │  ├─ InvoiceRepository.class
│  │  │  │        │  ├─ OtpStorageRepository.class
│  │  │  │        │  ├─ ReviewMediaRepository.class
│  │  │  │        │  ├─ ReviewRepository.class
│  │  │  │        │  ├─ RoleRepository.class
│  │  │  │        │  ├─ RoomImageRepository.class
│  │  │  │        │  ├─ RoomPriceRepository.class
│  │  │  │        │  ├─ RoomRepository.class
│  │  │  │        │  ├─ RoomTypeImageRepository.class
│  │  │  │        │  ├─ RoomTypeRepository.class
│  │  │  │        │  └─ UserRepository.class
│  │  │  │        ├─ security
│  │  │  │        │  ├─ JwtAuthenticationFilter.class
│  │  │  │        │  ├─ JwtTokenProvider.class
│  │  │  │        │  └─ JwtUtils.class
│  │  │  │        └─ service
│  │  │  │           ├─ AuthService.class
│  │  │  │           ├─ BookingService.class
│  │  │  │           ├─ DashboardService.class
│  │  │  │           ├─ PaymentService.class
│  │  │  │           ├─ RevenueScheduler.class
│  │  │  │           ├─ ReviewService.class
│  │  │  │           ├─ RoomManagementService.class
│  │  │  │           ├─ RoomService.class
│  │  │  │           ├─ RoomTypeService.class
│  │  │  │           └─ UserService.class
│  │  │  └─ templates
│  │  └─ test
│  │     └─ org
│  │        └─ example
│  │           └─ backend
│  │              └─ BackendApplicationTests.class
│  ├─ build
│  │  ├─ classes
│  │  │  └─ java
│  │  │     └─ main
│  │  │        └─ org
│  │  │           └─ example
│  │  │              └─ backend
│  │  │                 └─ BackendApplication.class
│  │  ├─ generated
│  │  │  └─ sources
│  │  │     ├─ annotationProcessor
│  │  │     │  └─ java
│  │  │     │     └─ main
│  │  │     └─ headers
│  │  │        └─ java
│  │  │           └─ main
│  │  ├─ reports
│  │  │  └─ problems
│  │  │     └─ problems-report.html
│  │  ├─ resolvedMainClassName
│  │  ├─ resources
│  │  │  └─ main
│  │  │     ├─ application.properties
│  │  │     ├─ static
│  │  │     └─ templates
│  │  └─ tmp
│  │     └─ compileJava
│  │        └─ previous-compilation-data.bin
│  ├─ HELP.md
│  ├─ pom.xml
│  ├─ src
│  │  ├─ main
│  │  │  ├─ java
│  │  │  │  └─ org
│  │  │  │     └─ example
│  │  │  │        └─ backend
│  │  │  │           ├─ BackendApplication.java
│  │  │  │           ├─ config
│  │  │  │           │  ├─ ApplicationConfig.java
│  │  │  │           │  ├─ JwtFilter.java
│  │  │  │           │  ├─ PayOSConfig.java
│  │  │  │           │  └─ SecurityConfig.java
│  │  │  │           ├─ controller
│  │  │  │           │  ├─ AdminDashboardController.java
│  │  │  │           │  ├─ AdminUserController.java
│  │  │  │           │  ├─ AuthController.java
│  │  │  │           │  ├─ BookingController.java
│  │  │  │           │  ├─ ReviewController.java
│  │  │  │           │  ├─ RoomController.java
│  │  │  │           │  ├─ RoomManagementController.java
│  │  │  │           │  ├─ RoomTypeController.java
│  │  │  │           │  └─ UserController.java
│  │  │  │           ├─ dto
│  │  │  │           │  ├─ request
│  │  │  │           │  │  ├─ AdminCreateUserRequest.java
│  │  │  │           │  │  ├─ BookingRequest.java
│  │  │  │           │  │  ├─ ChangePasswordRequest.java
│  │  │  │           │  │  ├─ ForgotPasswordRequest.java
│  │  │  │           │  │  ├─ LoginRequest.java
│  │  │  │           │  │  ├─ RegisterRequest.java
│  │  │  │           │  │  ├─ ResetPasswordRequest.java
│  │  │  │           │  │  ├─ ReviewRequestDTO.java
│  │  │  │           │  │  ├─ RoomImageRequest.java
│  │  │  │           │  │  ├─ RoomRequest.java
│  │  │  │           │  │  ├─ RoomSearchRequest.java
│  │  │  │           │  │  ├─ RoomTypeImageRequest.java
│  │  │  │           │  │  ├─ RoomTypeRequest.java
│  │  │  │           │  │  ├─ UpdateEmailRequest.java
│  │  │  │           │  │  └─ VerifyOtpRequest.java
│  │  │  │           │  └─ response
│  │  │  │           │     ├─ AuthResponse.java
│  │  │  │           │     ├─ BookingHistoryResponseDTO.java
│  │  │  │           │     ├─ BookingStatusProjection.java
│  │  │  │           │     ├─ CustomerRoomResponseDTO.java
│  │  │  │           │     ├─ RevenueTrendProjection.java
│  │  │  │           │     ├─ ReviewResponseDTO.java
│  │  │  │           │     ├─ RoomResponseDTO.java
│  │  │  │           │     ├─ RoomTypeDetailResponse.java
│  │  │  │           │     ├─ UserProfileResponse.java
│  │  │  │           │     └─ UserResponse.java
│  │  │  │           ├─ entity
│  │  │  │           │  ├─ AuditLog.java
│  │  │  │           │  ├─ Booking.java
│  │  │  │           │  ├─ BookingDetail.java
│  │  │  │           │  ├─ DailyRevenueStat.java
│  │  │  │           │  ├─ enums
│  │  │  │           │  │  ├─ BookingStatus.java
│  │  │  │           │  │  ├─ InvoiceStatus.java
│  │  │  │           │  │  ├─ MediaType.java
│  │  │  │           │  │  ├─ OtpType.java
│  │  │  │           │  │  ├─ PaymentMethod.java
│  │  │  │           │  │  ├─ PaymentStatus.java
│  │  │  │           │  │  ├─ RoleType.java
│  │  │  │           │  │  ├─ RoomStatus.java
│  │  │  │           │  │  └─ UserStatus.java
│  │  │  │           │  ├─ Hotel.java
│  │  │  │           │  ├─ Invoice.java
│  │  │  │           │  ├─ OtpStorage.java
│  │  │  │           │  ├─ Review.java
│  │  │  │           │  ├─ ReviewMedia.java
│  │  │  │           │  ├─ Role.java
│  │  │  │           │  ├─ Room.java
│  │  │  │           │  ├─ RoomImage.java
│  │  │  │           │  ├─ RoomPrice.java
│  │  │  │           │  ├─ RoomType.java
│  │  │  │           │  ├─ RoomTypeImage.java
│  │  │  │           │  └─ User.java
│  │  │  │           ├─ exception
│  │  │  │           │  ├─ AppException.java
│  │  │  │           │  ├─ GlobalExceptionHandler.java
│  │  │  │           │  ├─ ResourceNotFoundException.java
│  │  │  │           │  └─ ReviewAlreadyExistsException.java
│  │  │  │           ├─ repository
│  │  │  │           │  ├─ AuditLogRepository.java
│  │  │  │           │  ├─ BookingDetailRepository.java
│  │  │  │           │  ├─ BookingRepository.java
│  │  │  │           │  ├─ DailyRevenueStatRepository.java
│  │  │  │           │  ├─ DashboardRepository.java
│  │  │  │           │  ├─ HotelRepository.java
│  │  │  │           │  ├─ InvoiceRepository.java
│  │  │  │           │  ├─ OtpStorageRepository.java
│  │  │  │           │  ├─ ReviewMediaRepository.java
│  │  │  │           │  ├─ ReviewRepository.java
│  │  │  │           │  ├─ RoleRepository.java
│  │  │  │           │  ├─ RoomImageRepository.java
│  │  │  │           │  ├─ RoomPriceRepository.java
│  │  │  │           │  ├─ RoomRepository.java
│  │  │  │           │  ├─ RoomTypeImageRepository.java
│  │  │  │           │  ├─ RoomTypeRepository.java
│  │  │  │           │  └─ UserRepository.java
│  │  │  │           ├─ security
│  │  │  │           │  ├─ JwtAuthenticationFilter.java
│  │  │  │           │  ├─ JwtTokenProvider.java
│  │  │  │           │  └─ JwtUtils.java
│  │  │  │           └─ service
│  │  │  │              ├─ AuthService.java
│  │  │  │              ├─ BookingService.java
│  │  │  │              ├─ DashboardService.java
│  │  │  │              ├─ PaymentService.java
│  │  │  │              ├─ RevenueScheduler.java
│  │  │  │              ├─ ReviewService.java
│  │  │  │              ├─ RoomManagementService.java
│  │  │  │              ├─ RoomService.java
│  │  │  │              ├─ RoomTypeService.java
│  │  │  │              └─ UserService.java
│  │  │  └─ resources
│  │  │     ├─ application.properties
│  │  │     ├─ static
│  │  │     └─ templates
│  │  └─ test
│  │     └─ java
│  │        └─ org
│  │           └─ example
│  │              └─ backend
│  │                 └─ BackendApplicationTests.java
│  └─ target
│     ├─ classes
│     │  ├─ application.properties
│     │  └─ org
│     │     └─ example
│     │        └─ backend
│     │           ├─ BackendApplication.class
│     │           ├─ config
│     │           │  ├─ ApplicationConfig.class
│     │           │  ├─ JwtFilter.class
│     │           │  ├─ PayOSConfig.class
│     │           │  └─ SecurityConfig.class
│     │           ├─ controller
│     │           │  ├─ AdminDashboardController.class
│     │           │  ├─ AdminUserController.class
│     │           │  ├─ AuthController.class
│     │           │  ├─ BookingController.class
│     │           │  ├─ ReviewController.class
│     │           │  ├─ RoomController.class
│     │           │  ├─ RoomManagementController.class
│     │           │  ├─ RoomTypeController.class
│     │           │  └─ UserController.class
│     │           ├─ dto
│     │           │  ├─ request
│     │           │  │  ├─ AdminCreateUserRequest$AdminCreateUserRequestBuilder.class
│     │           │  │  ├─ AdminCreateUserRequest.class
│     │           │  │  ├─ BookingRequest.class
│     │           │  │  ├─ ChangePasswordRequest.class
│     │           │  │  ├─ ForgotPasswordRequest.class
│     │           │  │  ├─ LoginRequest.class
│     │           │  │  ├─ RegisterRequest.class
│     │           │  │  ├─ ResetPasswordRequest.class
│     │           │  │  ├─ ReviewRequestDTO.class
│     │           │  │  ├─ RoomImageRequest.class
│     │           │  │  ├─ RoomRequest.class
│     │           │  │  ├─ RoomSearchRequest.class
│     │           │  │  ├─ RoomTypeImageRequest.class
│     │           │  │  ├─ RoomTypeRequest.class
│     │           │  │  ├─ UpdateEmailRequest.class
│     │           │  │  └─ VerifyOtpRequest.class
│     │           │  └─ response
│     │           │     ├─ AuthResponse.class
│     │           │     ├─ BookingHistoryResponseDTO$BookingHistoryResponseDTOBuilder.class
│     │           │     ├─ BookingHistoryResponseDTO.class
│     │           │     ├─ BookingStatusProjection.class
│     │           │     ├─ CustomerRoomResponseDTO.class
│     │           │     ├─ RevenueTrendProjection.class
│     │           │     ├─ ReviewResponseDTO$ReviewResponseDTOBuilder.class
│     │           │     ├─ ReviewResponseDTO.class
│     │           │     ├─ RoomResponseDTO$RoomResponseDTOBuilder.class
│     │           │     ├─ RoomResponseDTO.class
│     │           │     ├─ RoomTypeDetailResponse$RoomTypeDetailResponseBuilder.class
│     │           │     ├─ RoomTypeDetailResponse.class
│     │           │     ├─ UserProfileResponse$UserProfileResponseBuilder.class
│     │           │     ├─ UserProfileResponse.class
│     │           │     ├─ UserResponse$UserResponseBuilder.class
│     │           │     └─ UserResponse.class
│     │           ├─ entity
│     │           │  ├─ AuditLog.class
│     │           │  ├─ Booking.class
│     │           │  ├─ BookingDetail.class
│     │           │  ├─ DailyRevenueStat$DailyRevenueStatBuilder.class
│     │           │  ├─ DailyRevenueStat.class
│     │           │  ├─ enums
│     │           │  │  ├─ BookingStatus.class
│     │           │  │  ├─ InvoiceStatus.class
│     │           │  │  ├─ MediaType.class
│     │           │  │  ├─ OtpType.class
│     │           │  │  ├─ PaymentMethod.class
│     │           │  │  ├─ PaymentStatus.class
│     │           │  │  ├─ RoleType.class
│     │           │  │  ├─ RoomStatus.class
│     │           │  │  └─ UserStatus.class
│     │           │  ├─ Hotel.class
│     │           │  ├─ Invoice.class
│     │           │  ├─ OtpStorage.class
│     │           │  ├─ Review.class
│     │           │  ├─ ReviewMedia.class
│     │           │  ├─ Role.class
│     │           │  ├─ Room.class
│     │           │  ├─ RoomImage.class
│     │           │  ├─ RoomPrice.class
│     │           │  ├─ RoomType.class
│     │           │  ├─ RoomTypeImage.class
│     │           │  ├─ User$UserBuilder.class
│     │           │  └─ User.class
│     │           ├─ exception
│     │           │  ├─ AppException.class
│     │           │  ├─ GlobalExceptionHandler.class
│     │           │  ├─ ResourceNotFoundException.class
│     │           │  └─ ReviewAlreadyExistsException.class
│     │           ├─ repository
│     │           │  ├─ AuditLogRepository.class
│     │           │  ├─ BookingDetailRepository.class
│     │           │  ├─ BookingRepository.class
│     │           │  ├─ DailyRevenueStatRepository.class
│     │           │  ├─ DashboardRepository.class
│     │           │  ├─ HotelRepository.class
│     │           │  ├─ InvoiceRepository.class
│     │           │  ├─ OtpStorageRepository.class
│     │           │  ├─ ReviewMediaRepository.class
│     │           │  ├─ ReviewRepository.class
│     │           │  ├─ RoleRepository.class
│     │           │  ├─ RoomImageRepository.class
│     │           │  ├─ RoomPriceRepository.class
│     │           │  ├─ RoomRepository.class
│     │           │  ├─ RoomTypeImageRepository.class
│     │           │  ├─ RoomTypeRepository.class
│     │           │  └─ UserRepository.class
│     │           ├─ security
│     │           │  ├─ JwtAuthenticationFilter.class
│     │           │  ├─ JwtTokenProvider.class
│     │           │  └─ JwtUtils.class
│     │           └─ service
│     │              ├─ AuthService.class
│     │              ├─ BookingService.class
│     │              ├─ DashboardService.class
│     │              ├─ PaymentService.class
│     │              ├─ RevenueScheduler.class
│     │              ├─ ReviewService.class
│     │              ├─ RoomManagementService.class
│     │              ├─ RoomService.class
│     │              ├─ RoomTypeService.class
│     │              └─ UserService.class
│     ├─ generated-sources
│     │  └─ annotations
│     └─ maven-status
│        └─ maven-compiler-plugin
│           └─ compile
│              └─ default-compile
│                 ├─ createdFiles.lst
│                 └─ inputFiles.lst
└─ frontend
   ├─ .angular
   │  └─ cache
   │     └─ 18.2.21
   │        └─ frontend
   │           ├─ .tsbuildinfo
   │           └─ vite
   │              └─ deps
   │                 ├─ @angular_common.js
   │                 ├─ @angular_common.js.map
   │                 ├─ @angular_common_http.js
   │                 ├─ @angular_common_http.js.map
   │                 ├─ @angular_core.js
   │                 ├─ @angular_core.js.map
   │                 ├─ @angular_forms.js
   │                 ├─ @angular_forms.js.map
   │                 ├─ @angular_platform-browser.js
   │                 ├─ @angular_platform-browser.js.map
   │                 ├─ @angular_router.js
   │                 ├─ @angular_router.js.map
   │                 ├─ chunk-6Q4RANH6.js
   │                 ├─ chunk-6Q4RANH6.js.map
   │                 ├─ chunk-A2MYQCX4.js
   │                 ├─ chunk-A2MYQCX4.js.map
   │                 ├─ chunk-CXCX2JKZ.js
   │                 ├─ chunk-CXCX2JKZ.js.map
   │                 ├─ chunk-F3ZSUPDB.js
   │                 ├─ chunk-F3ZSUPDB.js.map
   │                 ├─ chunk-FFZIAYYX.js
   │                 ├─ chunk-FFZIAYYX.js.map
   │                 ├─ chunk-TL4DGONE.js
   │                 ├─ chunk-TL4DGONE.js.map
   │                 ├─ chunk-W2BAPFRP.js
   │                 ├─ chunk-W2BAPFRP.js.map
   │                 ├─ package.json
   │                 ├─ rxjs.js
   │                 ├─ rxjs.js.map
   │                 ├─ rxjs_operators.js
   │                 ├─ rxjs_operators.js.map
   │                 └─ _metadata.json
   ├─ .editorconfig
   ├─ angular.json
   ├─ package-lock.json
   ├─ package.json
   ├─ public
   │  ├─ assets
   │  │  └─ images
   │  │     └─ hero-banner.jpg
   │  └─ favicon.ico
   ├─ README.md
   ├─ src
   │  ├─ app
   │  │  ├─ app.component.html
   │  │  ├─ app.component.scss
   │  │  ├─ app.component.spec.ts
   │  │  ├─ app.component.ts
   │  │  ├─ app.config.ts
   │  │  ├─ app.routes.ts
   │  │  ├─ components
   │  │  │  ├─ admin-layout
   │  │  │  │  ├─ admin-layout.component.html
   │  │  │  │  └─ admin-layout.component.ts
   │  │  │  ├─ footer
   │  │  │  │  └─ footer.component.ts
   │  │  │  ├─ header
   │  │  │  │  └─ header.component.ts
   │  │  │  └─ room-matrix
   │  │  │     ├─ room-matrix.component.html
   │  │  │     ├─ room-matrix.component.scss
   │  │  │     ├─ room-matrix.component.spec.ts
   │  │  │     └─ room-matrix.component.ts
   │  │  ├─ guards
   │  │  │  └─ admin.guard.ts
   │  │  ├─ interceptors
   │  │  │  └─ auth.interceptor.ts
   │  │  ├─ models
   │  │  │  ├─ admin-management.model.ts
   │  │  │  ├─ auth.model.ts
   │  │  │  ├─ room-management.model.ts
   │  │  │  ├─ room.model.ts
   │  │  │  └─ user.model.ts
   │  │  ├─ pages
   │  │  │  ├─ admin
   │  │  │  │  ├─ dashboard
   │  │  │  │  │  ├─ dashboard.component.html
   │  │  │  │  │  ├─ dashboard.component.scss
   │  │  │  │  │  ├─ dashboard.component.spec.ts
   │  │  │  │  │  └─ dashboard.component.ts
   │  │  │  │  ├─ promotion-management
   │  │  │  │  │  ├─ promotion-management.component.html
   │  │  │  │  │  ├─ promotion-management.component.scss
   │  │  │  │  │  ├─ promotion-management.component.spec.ts
   │  │  │  │  │  └─ promotion-management.component.ts
   │  │  │  │  └─ user-management
   │  │  │  │     ├─ user-management.component.html
   │  │  │  │     ├─ user-management.component.scss
   │  │  │  │     ├─ user-management.component.spec.ts
   │  │  │  │     └─ user-management.component.ts
   │  │  │  ├─ forgot-password
   │  │  │  │  ├─ forgot-password.component.html
   │  │  │  │  └─ forgot-password.component.ts
   │  │  │  ├─ home
   │  │  │  │  ├─ home.component.html
   │  │  │  │  └─ home.component.ts
   │  │  │  ├─ login
   │  │  │  │  ├─ login.component.html
   │  │  │  │  └─ login.component.ts
   │  │  │  ├─ profile
   │  │  │  │  ├─ profile.component.html
   │  │  │  │  └─ profile.component.ts
   │  │  │  └─ register
   │  │  │     ├─ register.component.html
   │  │  │     └─ register.component.ts
   │  │  └─ services
   │  │     ├─ auth.service.ts
   │  │     ├─ room-management.service.ts
   │  │     ├─ room.service.ts
   │  │     ├─ user-admin.service.ts
   │  │     └─ user.service.ts
   │  ├─ index.html
   │  ├─ main.ts
   │  └─ styles.scss
   ├─ tailwind.config.js
   ├─ tsconfig.app.json
   ├─ tsconfig.json
   └─ tsconfig.spec.json

```