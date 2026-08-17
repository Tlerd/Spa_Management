package com.spa.dto;

/**
 * ============================================================================
 * [VAI TRÒ & CHỨC NĂNG]
 * - Lớp DTO (Data Transfer Object) đại diện cho thực thể Người Dùng (tblUsers).
 * - Đóng gói dữ liệu người dùng để truyền tải giữa tầng DAO, Controller, Session và View (JSP).
 * - Chú ý an toàn bảo mật: Không chứa trường mật khẩu (password) trong DTO này
 *   nhằm tránh rò rỉ dữ liệu nhạy cảm khi lưu trong HttpSession suốt phiên làm việc.
 *
 * [LUỒNG DỮ LIỆU (DATA FLOW)]
 * 1. Từ CSDL (tblUsers) -> UserDAO.checkLogin() khởi tạo đối tượng User.
 * 2. LoginController nhận User từ UserDAO -> Lưu vào HttpSession: session.setAttribute("LOGIN_USER", user).
 * 3. Các Controller khác lấy ra để phân quyền và lấy userID/staffID:
 *    User loginUser = (User) session.getAttribute("LOGIN_USER");
 * 4. Các trang JSP đọc thông tin hiển thị và kiểm tra vai trò qua EL:
 *    ${sessionScope.LOGIN_USER.fullName}, ${sessionScope.LOGIN_USER.admin},
 *    ${sessionScope.LOGIN_USER.customer}, ${sessionScope.LOGIN_USER.staff}.
 *
 * [LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
 * 1. Sửa tên trường (ví dụ: userID, fullName, email, phoneNumber, roleID):
 *    -> Phải sửa getter/setter tương ứng trong lớp này.
 *    -> Phải sửa câu lệnh gán trong UserDAO (checkLogin, insert).
 *    -> Phải sửa các Controller gọi getter: LoginController, RegisterController,
 *       ViewServicesController, LoadBookingController, BookAppointmentController,
 *       ViewAppointmentsController, CancelAppointmentController,
 *       ViewConsultationController, SaveNotesController, CompleteAppointmentController,
 *       LoadReviewController, ReviewServiceController.
 *    -> Phải sửa các biểu thức EL trên JSP:
 *       + [dashboard.jsp]: ${sessionScope.LOGIN_USER.fullName}, ${sessionScope.LOGIN_USER.roleID}, ${sessionScope.LOGIN_USER.email}
 *       + [consultation.jsp]: ${sessionScope.LOGIN_USER.fullName}
 * 2. Sửa các phương thức kiểm tra vai trò (isAdmin, isStaff, isCustomer):
 *    -> Ảnh hưởng trực tiếp tới logic phân quyền ở tất cả 13 Controller chức năng.
 *    -> Ảnh hưởng tới điều hướng EL trên [dashboard.jsp] và bộ lọc ở đầu các file JSP.
 * 3. Sửa mã vai trò ("ADM", "STF", "USR"):
 *    -> Phải đồng bộ với:
 *       + Ràng buộc CSDL: CK_Users_Role CHECK (roleID IN ('ADM', 'USR', 'STF')) trong [SpaConsultationPortal.sql].
 *       + Hằng số DEFAULT_ROLE = "USR" trong [RegisterController.java].
 *       + Phương thức isAdmin, isStaff, isCustomer trong lớp này.
 * ============================================================================
 */
public class User {

    // [TÁC ĐỘNG]: Khớp với cột userID VARCHAR(50) trong bảng tblUsers
    private String userID;

    // [TÁC ĐỘNG]: Khớp với cột fullName NVARCHAR(500) trong bảng tblUsers, hiển thị ở dashboard.jsp & consultation.jsp
    private String fullName;

    // [TÁC ĐỘNG]: Khớp với cột email VARCHAR(100) UNIQUE trong bảng tblUsers, dùng làm tài khoản đăng nhập
    private String email;

    // [TÁC ĐỘNG]: Khớp với cột phoneNumber VARCHAR(15) trong bảng tblUsers
    private String phoneNumber;

    // [TÁC ĐỘNG]: Khớp với cột roleID NVARCHAR(5) trong bảng tblUsers ('ADM', 'STF', 'USR')
    private String roleID;

    public User() {
    }

    public User(String userID, String fullName, String email,
                String phoneNumber, String roleID) {
        this.userID = userID;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.roleID = roleID;
    }

    public String getUserID() { return userID; }
    public void setUserID(String userID) { this.userID = userID; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getRoleID() { return roleID; }
    public void setRoleID(String roleID) { this.roleID = roleID; }

    /**
     * Kiểm tra người dùng có phải Quản trị viên (Admin - ADM) hay không.
     * [DỮ LIỆU ĐI TỚI ĐÂU]:
     * - Được gọi bởi các Controller Admin: ViewServicesController, LoadServiceController,
     *   CreateServiceController, UpdateServiceController, DeleteServiceController.
     * - Được gọi trong JSP bằng EL: ${sessionScope.LOGIN_USER.admin} ở [dashboard.jsp],
     *   [serviceList.jsp], [createService.jsp], [updateService.jsp].
     * [TÁC ĐỘNG KHI SỬA]: Nếu đổi "ADM" -> Phải sửa bảng tblUsers, SQL insert mẫu và JSP.
     */
    public boolean isAdmin() { return "ADM".equals(roleID); }

    /**
     * Kiểm tra người dùng có phải Nhân viên tư vấn (Staff - STF) hay không.
     * [DỮ LIỆU ĐI TỚI ĐÂU]:
     * - Được gọi bởi các Controller Staff: ViewConsultationController, SaveNotesController, CompleteAppointmentController.
     * - Được gọi trong JSP bằng EL: ${sessionScope.LOGIN_USER.staff} ở [dashboard.jsp], [consultation.jsp].
     * [TÁC ĐỘNG KHI SỬA]: Nếu đổi "STF" -> Phải sửa bảng tblUsers và các file liên quan.
     */
    public boolean isStaff() { return "STF".equals(roleID); }

    /**
     * Kiểm tra người dùng có phải Khách hàng (Customer - USR) hay không.
     * [DỮ LIỆU ĐI TỚI ĐÂU]:
     * - Được gọi bởi các Controller Customer: LoadBookingController, BookAppointmentController,
     *   ViewAppointmentsController, CancelAppointmentController, LoadReviewController, ReviewServiceController.
     * - Được gọi trong JSP bằng EL: ${sessionScope.LOGIN_USER.customer} ở [dashboard.jsp],
     *   [bookAppointment.jsp], [appointmentList.jsp], [reviewService.jsp].
     * [TÁC ĐỘNG KHI SỬA]: Nếu đổi "USR" -> Phải sửa bảng tblUsers và RegisterController.
     */
    public boolean isCustomer() { return "USR".equals(roleID); }
}
