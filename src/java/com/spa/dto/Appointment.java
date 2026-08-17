package com.spa.dto;

import java.sql.Timestamp;

/**
 * ============================================================================
 * [VAI TRÒ & CHỨC NĂNG]
 * - Lớp DTO (Data Transfer Object) đại diện cho Lịch Hẹn / Buổi Tư Vấn (tblAppointments).
 * - Chứa cả 7 trường gốc của bảng CSDL tblAppointments VÀ 3 trường mở rộng
 *   (userName, serviceName, staffName) được JOIN từ các bảng tblUsers và tblServices
 *   để tối ưu hóa hiển thị trên giao diện người dùng mà không cần truy vấn lặp (N+1 query problem).
 *
 * [LUỒNG DỮ LIỆU (DATA FLOW)]
 * 1. Từ CSDL (tblAppointments JOIN tblServices, tblUsers):
 *    - AppointmentDAO.getByUser(userID) -> List<Appointment> -> ViewAppointmentsController -> [appointmentList.jsp] (${LIST_APPOINTMENT})
 *    - AppointmentDAO.getByStaff(staffID) -> List<Appointment> -> ViewConsultationController -> [consultation.jsp] (${LIST_CONSULTATION})
 *    - AppointmentDAO.getByID(appID) -> Appointment -> Dùng kiểm tra quyền & trạng thái tại:
 *      CancelAppointmentController, SaveNotesController, CompleteAppointmentController.
 * 2. Từ Người dùng đặt lịch:
 *    - [bookAppointment.jsp] -> BookAppointmentController -> new Appointment(...) -> AppointmentDAO.insert() -> CSDL tblAppointments
 *
 * [LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
 * 1. Sửa các thuộc tính gốc (appointmentID, userID, serviceID, appointmentDate, status, staffID, consultationNotes):
 *    -> Phải sửa tương ứng các cột trong bảng tblAppointments ở [SpaConsultationPortal.sql].
 *    -> Phải sửa câu lệnh SELECT_COLUMNS, INSERT, UPDATE_STATUS, UPDATE_NOTES trong [AppointmentDAO.java].
 *    -> Phải sửa các phương thức khởi tạo và getter/setter trong lớp này.
 *    -> Phải sửa các Controller liên quan tới đặt lịch và tư vấn.
 * 2. Sửa các trường mở rộng (userName, serviceName, staffName):
 *    -> Phải sửa câu lệnh JOIN và việc đọc rs.getString trong [AppointmentDAO.java] (queryList).
 *    -> Phải sửa hiển thị trên JSP:
 *       + [appointmentList.jsp]: ${app.serviceName}, ${app.staffName}
 *       + [consultation.jsp]: ${item.userName}, ${item.serviceName}
 * 3. Sửa phương thức isPending(), isCompleted():
 *    -> Ảnh hưởng tới logic kiểm tra trong CancelAppointmentController, CompleteAppointmentController.
 *    -> Ảnh hưởng tới việc ẩn/hiện nút Hủy / Hoàn thành trên [appointmentList.jsp] và [consultation.jsp].
 * ============================================================================
 */
public class Appointment {

    // [CÁC CỘT TRONG BẢNG tblAppointments]
    // [TÁC ĐỘNG]: Khóa chính trong bảng tblAppointments VARCHAR(50)
    private String appointmentID;

    // [TÁC ĐỘNG]: Khóa ngoại tham chiếu tới tblUsers(userID), là người đặt lịch
    private String userID;

    // [TÁC ĐỘNG]: Khóa ngoại tham chiếu tới tblServices(serviceID)
    private String serviceID;

    // [TÁC ĐỘNG]: Thời gian hẹn DATETIME trong CSDL, biểu diễn bằng java.sql.Timestamp
    private Timestamp appointmentDate;

    // [TÁC ĐỘNG]: Trạng thái lịch hẹn ('Pending', 'Completed', 'Cancelled')
    private String status;

    // [TÁC ĐỘNG]: Khóa ngoại tham chiếu tới tblUsers(userID), là nhân viên phụ trách tư vấn (có thể NULL)
    private String staffID;

    // [TÁC ĐỘNG]: Ghi chú tư vấn NVARCHAR(1000) do nhân viên ghi (có thể NULL)
    private String consultationNotes;

    // [CÁC TRƯỜNG MỞ RỘNG - LẤY TỪ PHÉP JOIN BẢNG ĐỂ HIỂN THỊ LÊN JSP]
    // [TÁC ĐỘNG]: Lấy từ tblUsers.fullName của khách hàng (u.fullName AS userName)
    private String userName;

    // [TÁC ĐỘNG]: Lấy từ tblServices.serviceName (s.serviceName)
    private String serviceName;

    // [TÁC ĐỘNG]: Lấy từ tblUsers.fullName của nhân viên (st.fullName AS staffName)
    private String staffName;

    public Appointment() {
    }

    public Appointment(String appointmentID, String userID, String serviceID,
                       Timestamp appointmentDate, String status,
                       String staffID, String consultationNotes) {
        this.appointmentID = appointmentID;
        this.userID = userID;
        this.serviceID = serviceID;
        this.appointmentDate = appointmentDate;
        this.status = status;
        this.staffID = staffID;
        this.consultationNotes = consultationNotes;
    }

    public String getAppointmentID() { return appointmentID; }
    public void setAppointmentID(String appointmentID) {
        this.appointmentID = appointmentID;
    }

    public String getUserID() { return userID; }
    public void setUserID(String userID) { this.userID = userID; }

    public String getServiceID() { return serviceID; }
    public void setServiceID(String serviceID) {
        this.serviceID = serviceID;
    }

    public Timestamp getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(Timestamp appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStaffID() { return staffID; }
    public void setStaffID(String staffID) { this.staffID = staffID; }

    public String getConsultationNotes() { return consultationNotes; }
    public void setConsultationNotes(String consultationNotes) {
        this.consultationNotes = consultationNotes;
    }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    /**
     * Kiểm tra trạng thái có đang là 'Pending' (Đang chờ) hay không.
     * [DỮ LIỆU ĐI TỚI ĐÂU]:
     * - Dùng trong CancelAppointmentController: Chỉ cho phép hủy khi isPending() == true.
     * - Dùng trong CompleteAppointmentController: Chỉ cho phép hoàn thành khi isPending() == true.
     * - Dùng trong EL tại [appointmentList.jsp] (${app.pending}) và [consultation.jsp] (${item.pending})
     *   để quyết định hiển thị nút Cancel/Complete.
     * [TÁC ĐỘNG KHI SỬA]: Nếu đổi chuỗi "Pending" -> Phải sửa CSDL Check constraint, DAO và Controller.
     */
    public boolean isPending() { return "Pending".equals(status); }

    /**
     * Kiểm tra trạng thái có là 'Completed' (Đã hoàn thành) hay không.
     * [DỮ LIỆU ĐI TỚI ĐÂU]:
     * - Dùng trong EL tại [appointmentList.jsp] (${app.completed}) để hiển thị link "Write a review".
     * [TÁC ĐỘNG KHI SỬA]: Phải đồng bộ với giá trị 'Completed' trong ReviewDAO (câu truy van GET_REVIEWABLE)
     *   và CompleteAppointmentController.
     */
    public boolean isCompleted() { return "Completed".equals(status); }
}
