/* ============================================================================
   HỆ THỐNG QUẢN LÝ DỊCH VỤ VÀ TƯ VẤN SPA (Spa Consultation Portal)
   - Học phần: PRJ301 - Java Web Application (MVC2 Architecture)
   - Cấu hình kết nối CSDL (Xem DatabaseConnection.java):
       Server = localhost, Port = 1433, Database = SpaConsultationPortal
       User = sa, Password = 12345
   - Hướng dẫn chạy: Chạy toàn bộ file script này 1 lần trong SQL Server Management Studio (SSMS).
   ============================================================================ */

USE master;
GO

-- Xóa database cũ nếu đã tồn tại để tránh xung đột dữ liệu
IF DB_ID('SpaConsultationPortal') IS NOT NULL
BEGIN
    ALTER DATABASE SpaConsultationPortal SET SINGLE_USER
        WITH ROLLBACK IMMEDIATE;
    DROP DATABASE SpaConsultationPortal;
END
GO

CREATE DATABASE SpaConsultationPortal;
GO

USE SpaConsultationPortal;
GO

/* ============================================================================
   1. BẢNG NGƯỜI DÙNG (tblUsers)
   - Chứa thông tin tài khoản của 3 vai trò:
     + ADM: Quản trị viên (Admin)
     + USR: Khách hàng (Customer / User)
     + STF: Nhân viên tư vấn (Staff)
   
   [LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI]:
   - Khóa chính `userID`: Được tham chiếu bởi tblAppointments(userID, staffID) và tblReviews(userID).
   - Ràng buộc `CK_Users_Role`: Giới hạn roleID chỉ thuộc ('ADM', 'USR', 'STF').
     -> Nếu đổi mã vai trò, phải sửa: User.java (isAdmin, isStaff, isCustomer),
        RegisterController.java (DEFAULT_ROLE), dashboard.jsp, và các Controller phân quyền.
   - Cột `email`: Có ràng buộc UNIQUE, dùng làm tên đăng nhập tại LoginController và UserDAO.
   - Đối tượng Java tương ứng: [com.spa.dto.User]
   - Lớp DAO quản lý: [com.spa.dao.UserDAO]
   ============================================================================ */
CREATE TABLE tblUsers (
    userID      VARCHAR(50)   PRIMARY KEY NOT NULL, -- Mã người dùng (Khóa chính)
    fullName    NVARCHAR(500) NOT NULL,             -- Họ và tên
    email       VARCHAR(100)  NOT NULL UNIQUE,      -- Email (Tên đăng nhập duy nhất)
    phoneNumber VARCHAR(15)   NOT NULL,             -- Số điện thoại (9 - 15 chữ số)
    roleID      NVARCHAR(5)   NOT NULL,             -- Mã vai trò: 'ADM', 'USR', 'STF'
    password    VARCHAR(50)   NOT NULL,             -- Mật khẩu
    CONSTRAINT CK_Users_Role CHECK (roleID IN ('ADM', 'USR', 'STF'))
);
GO

/* ============================================================================
   2. BẢNG DỊCH VỤ SPA (tblServices)
   - Chứa danh mục các gói dịch vụ spa do Admin quản lý.
   
   [LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI]:
   - Khóa chính `serviceID`: Được tham chiếu bởi tblAppointments(serviceID) và tblReviews(serviceID).
   - Ràng buộc `CK_Services_Price`: Giá tiền phải >= 0.
   - Khi xóa một dịch vụ đang được tham chiếu trong tblAppointments hoặc tblReviews:
     SQL Server sẽ chặn lại với mã lỗi 547 (Foreign Key Violation). DeleteServiceController
     bắt mã lỗi này để thông báo cho người dùng.
   - Đối tượng Java tương ứng: [com.spa.dto.Service]
   - Lớp DAO quản lý: [com.spa.dao.ServiceDAO]
   ============================================================================ */
CREATE TABLE tblServices (
    serviceID   VARCHAR(50)    PRIMARY KEY NOT NULL, -- Mã dịch vụ (Khóa chính, không tự sinh)
    serviceName NVARCHAR(200)  NOT NULL,             -- Tên dịch vụ spa
    description NVARCHAR(1000) NOT NULL,             -- Mô tả chi tiết dịch vụ
    price       DECIMAL(18, 2) NOT NULL,             -- Giá tiền (DECIMAL map với java.math.BigDecimal)
    CONSTRAINT CK_Services_Price CHECK (price >= 0)
);
GO

/* ============================================================================
   3. BẢNG LỊCH HẸN VÀ TƯ VẤN (tblAppointments)
   - Lưu trữ lịch đặt dịch vụ của khách hàng và quá trình theo dõi tư vấn của nhân viên.
   
   [LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI]:
   - `userID`: Khách hàng đặt lịch (FK -> tblUsers).
   - `serviceID`: Dịch vụ được đặt (FK -> tblServices).
   - `staffID`: Nhân viên được phân công tư vấn (FK -> tblUsers). Cho phép NULL khi mới đặt.
   - `consultationNotes`: Ghi chú tư vấn của nhân viên (NVARCHAR(1000)). Cho phép NULL.
   - Ràng buộc `CK_App_Status`: Trạng thái chỉ nhận ('Pending', 'Completed', 'Cancelled').
     + 'Pending': Lịch mới đặt, cho phép khách hủy (CancelAppointment) hoặc nhân viên hoàn thành (CompleteAppointment).
     + 'Completed': Đã hoàn thành dịch vụ, kích hoạt điều kiện cho phép khách hàng đánh giá tại ReviewDAO / reviewService.jsp.
     + 'Cancelled': Lịch đã hủy.
   - Đối tượng Java tương ứng: [com.spa.dto.Appointment]
   - Lớp DAO quản lý: [com.spa.dao.AppointmentDAO]
   ============================================================================ */
CREATE TABLE tblAppointments (
    appointmentID     VARCHAR(50)    PRIMARY KEY NOT NULL, -- Mã lịch hẹn (ví dụ: 'app1', 'app2')
    userID            VARCHAR(50)    NOT NULL,             -- Khách hàng đặt lịch (FK -> tblUsers)
    serviceID         VARCHAR(50)    NOT NULL,             -- Dịch vụ được chọn (FK -> tblServices)
    appointmentDate   DATETIME       NOT NULL,             -- Thời gian hẹn (Map với java.sql.Timestamp)
    status            NVARCHAR(20)   NOT NULL,             -- Trạng thái: 'Pending', 'Completed', 'Cancelled'
    staffID           VARCHAR(50)    NULL,                 -- Nhân viên phụ trách (FK -> tblUsers, NULL nếu chưa giao)
    consultationNotes NVARCHAR(1000) NULL,                 -- Ghi chú tư vấn của nhân viên
    CONSTRAINT FK_App_User    FOREIGN KEY (userID)
        REFERENCES tblUsers(userID),
    CONSTRAINT FK_App_Service FOREIGN KEY (serviceID)
        REFERENCES tblServices(serviceID),
    CONSTRAINT FK_App_Staff   FOREIGN KEY (staffID)
        REFERENCES tblUsers(userID),
    CONSTRAINT CK_App_Status  CHECK
        (status IN ('Pending', 'Completed', 'Cancelled'))
);
GO

/* ============================================================================
   4. BẢNG ĐÁNH GIÁ DỊCH VỤ (tblReviews)
   - Lưu trữ nhận xét và điểm đánh giá (1-5 sao) của khách hàng cho dịch vụ đã sử dụng.
   
   [LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI]:
   - Ràng buộc `CK_Rev_Rating`: Điểm số rating phải từ 1 đến 5 (BETWEEN 1 AND 5).
   - Ràng buộc `UQ_Rev_UserService`: UNIQUE(userID, serviceID) - Mỗi khách hàng chỉ được
     đánh giá mỗi dịch vụ DUY NHẤT 1 LẦN.
   - Điều kiện nghiệp vụ (Xem ReviewDAO.getReviewableServices): Chỉ được đánh giá khi đã có lịch hẹn
     Completed cho dịch vụ đó.
   - Đối tượng Java tương ứng: [com.spa.dto.Review]
   - Lớp DAO quản lý: [com.spa.dao.ReviewDAO]
   ============================================================================ */
CREATE TABLE tblReviews (
    reviewID  VARCHAR(50)    PRIMARY KEY NOT NULL, -- Mã đánh giá (ví dụ: 'rev1', 'rev2')
    userID    VARCHAR(50)    NOT NULL,             -- Khách hàng đánh giá (FK -> tblUsers)
    serviceID VARCHAR(50)    NOT NULL,             -- Dịch vụ được đánh giá (FK -> tblServices)
    rating    INT            NOT NULL,             -- Điểm đánh giá (1 đến 5 sao)
    comments  NVARCHAR(1000) NULL,                 -- Bình luận cảm nhận của khách hàng
    CONSTRAINT FK_Rev_User    FOREIGN KEY (userID)
        REFERENCES tblUsers(userID),
    CONSTRAINT FK_Rev_Service FOREIGN KEY (serviceID)
        REFERENCES tblServices(serviceID),
    CONSTRAINT CK_Rev_Rating  CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT UQ_Rev_UserService UNIQUE (userID, serviceID)
);
GO

/* ============================================================================
   DỮ LIỆU MẪU (TEST DATA)
   - 6 tài khoản người dùng: 1 Admin (admin1), 3 Customer (user1, user2, user3), 2 Staff (staff1, staff2).
   - 6 dịch vụ spa (svc1 đến svc6).
   - 8 lịch hẹn mẫu với đủ 3 trạng thái (Pending, Completed, Cancelled).
   - 4 bài đánh giá mẫu.
   ============================================================================ */

-- Chèn dữ liệu bảng tblUsers
INSERT INTO tblUsers
    (userID, fullName, email, phoneNumber, roleID, password) VALUES
('admin1', N'Admin User',    'admin@example.com', '1234567890', 'ADM', 'adminpass'),
('user1',  N'John Doe',      'john@example.com',  '0987654321', 'USR', 'userpass'),
('user2',  N'Tran Thu Ha',   'ha@example.com',    '0912345678', 'USR', 'ha12345'),
('user3',  N'Le Minh Tuan',  'tuan@example.com',  '0933444555', 'USR', 'tuan123'),
('staff1', N'Staff Member',  'staff@example.com', '1122334455', 'STF', 'staffpass'),
('staff2', N'Vo Ngoc Anh',   'anh@example.com',   '0977888999', 'STF', 'anh12345');
GO

-- Chèn dữ liệu bảng tblServices
INSERT INTO tblServices
    (serviceID, serviceName, description, price) VALUES
('svc1', N'Full-Body Massage',  N'A relaxing full-body massage.',        50.00),
('svc2', N'Facial Treatment',   N'Rejuvenating facial treatment.',       40.00),
('svc3', N'Hot Stone Therapy',  N'Warm stones to release deep tension.', 75.00),
('svc4', N'Aromatherapy',       N'Essential oils for relaxation.',       35.00),
('svc5', N'Foot Reflexology',   N'Pressure point therapy for the feet.', 30.00),
('svc6', N'Body Scrub',         N'Full-body exfoliation treatment.',     45.00);
GO

-- Chèn dữ liệu bảng tblAppointments
INSERT INTO tblAppointments
    (appointmentID, userID, serviceID, appointmentDate,
     status, staffID, consultationNotes) VALUES
('app1', 'user1', 'svc1', '2026-08-18 09:00', N'Pending',   'staff1', NULL),
('app2', 'user1', 'svc2', '2026-08-12 14:00', N'Completed', 'staff1',
    N'Customer has sensitive skin, used a mild product.'),
('app3', 'user2', 'svc3', '2026-08-19 10:30', N'Pending',   'staff2', NULL),
('app4', 'user2', 'svc1', '2026-08-05 16:00', N'Completed', 'staff2',
    N'Requested lighter pressure on the shoulders.'),
('app5', 'user3', 'svc4', '2026-08-20 08:00', N'Pending',   NULL,     NULL),
('app6', 'user3', 'svc5', '2026-08-08 13:00', N'Completed', 'staff1', NULL),
('app7', 'user1', 'svc6', '2026-08-01 11:00', N'Cancelled', NULL,     NULL),
('app8', 'user2', 'svc2', '2026-08-21 15:30', N'Pending',   'staff1', NULL);
GO

-- Chèn dữ liệu bảng tblReviews
INSERT INTO tblReviews
    (reviewID, userID, serviceID, rating, comments) VALUES
('rev1', 'user1', 'svc2', 5, N'Very professional and relaxing.'),
('rev2', 'user2', 'svc1', 4, N'Good service, a bit crowded.'),
('rev3', 'user3', 'svc5', 5, N'My feet felt brand new.'),
('rev4', 'user2', 'svc3', 3, N'The room was too warm for me.');
GO

-- Kiểm tra lại toàn bộ dữ liệu mẫu vừa nạp
SELECT * FROM tblUsers;
SELECT * FROM tblServices;
SELECT * FROM tblAppointments;
SELECT * FROM tblReviews;
GO
