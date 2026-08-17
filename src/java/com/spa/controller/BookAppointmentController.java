package com.spa.controller;

import com.spa.dao.AppointmentDAO;
import com.spa.dao.ServiceDAO;
import com.spa.dto.Appointment;
import com.spa.dto.User;
import java.io.IOException;
import java.sql.Timestamp;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * ============================================================================
 * [VAI TRÒ & CHỨC NĂNG]
 * - Xử lý chức năng ĐẶT LỊCH HẸN DỊCH VỤ SPA (Action: BookAppointment - Dành cho Customer).
 * - Nhận `serviceID` và `appointmentDate` từ form [bookAppointment.jsp].
 * - Lấy `userID` trực tiếp từ Session (`loginUser.getUserID()`) để đảm bảo an ninh (không cho phép đặt lịch hộ người khác).
 * - GÁN CỨNG trạng thái ban đầu luôn là "Pending" (`PENDING = "Pending"`).
 * - GÁN CỨNG `staffID = null` và `consultationNotes = null` (chưa phân công nhân viên khi mới đặt).
 * - Tự động sinh mã lịch hẹn qua `dao.getNextID()`.
 * - XỬ LÝ ĐỊNH DẠNG NGÀY GIỜ (datetime-local -> Timestamp):
 *   + Ô nhập `<input type="datetime-local">` gửi về chuỗi dạng "2026-08-20T08:00".
 *   + Hàm `parseDateTime()` thay thế ký tự 'T' thành khoảng trắng ' ' và nối thêm ":00"
 *     để tạo định dạng chuẩn "yyyy-MM-dd HH:mm:ss" cho `Timestamp.valueOf()`.
 * - KIỂM TRA RÀNG BUỘC NGHIỆP VỤ:
 *   + Ngày hẹn bắt buộc phải ở THỜI GIAN TƯƠNG LAI (`when.after(new Timestamp(System.currentTimeMillis()))`).
 *   + Dịch vụ được chọn phải còn tồn tại trong CSDL (`serviceDAO.getByID(...) != null`).
 * - Khi thành công: Chuyển tiếp tới `ViewAppointmentsController` để xem danh sách lịch hẹn.
 * - Khi có lỗi: Đặt thông báo lỗi `ERROR` và quay lại `LoadBookingController`.
 *
 * [LUỒNG THỰC THI (EXECUTION FLOW)]
 * 1. Form [bookAppointment.jsp] submit POST -> MainController (action=BookAppointment) -> BookAppointmentController.
 * 2. Kiểm tra quyền Customer trong Session.
 * 3. Đọc tham số `serviceID`, `appointmentDate`.
 * 4. Parse chuỗi ngày giờ qua hàm `parseDateTime` -> kiểm tra `when.after(now)`.
 * 5. Kiểm tra `serviceDAO.getByID(serviceID)` có tồn tại không.
 * 6. Tạo đối tượng `Appointment(dao.getNextID(), loginUser.getUserID(), serviceID, when, "Pending", null, null)`.
 * 7. Gọi `AppointmentDAO.insert(app)`:
 *    - Thành công -> Set `MESSAGE` -> Chuyển sang `ViewAppointmentsController`.
 *    - Thất bại / Có lỗi -> Set `ERROR` -> Chuyển về `LoadBookingController`.
 *
 * [LUỒNG DỮ LIỆU (DATA FLOW)]
 * - Dữ liệu vào:
 *   + `session.getAttribute("LOGIN_USER")` -> Lấy `userID` của khách hàng.
 *   + `request.getParameter("serviceID")` -> Mã dịch vụ khách chọn.
 *   + `request.getParameter("appointmentDate")` -> Chuỗi ngày giờ từ input datetime-local.
 * - Dữ liệu xuống DB:
 *   + INSERT INTO tblAppointments (appointmentID, userID, serviceID, appointmentDate, status='Pending', staffID=NULL, consultationNotes=NULL).
 * - Dữ liệu ra Request:
 *   + Thành công: `request.setAttribute("MESSAGE", "Appointment ... has been booked")` -> [ViewAppointmentsController].
 *   + Thất bại: `request.setAttribute("ERROR", error)` -> [LoadBookingController].
 *
 * [LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
 * 1. Sửa PENDING = "Pending":
 *    -> Phải đồng bộ với CK_App_Status CHECK (status IN ('Pending', 'Completed', 'Cancelled')) trong [SpaConsultationPortal.sql].
 * 2. Sửa cách định dạng ngày giờ:
 *    -> Phải đảm bảo tương thích giữa ô `<input type="datetime-local">` trên [bookAppointment.jsp] và hàm parseDateTime().
 * ============================================================================
 */
public class BookAppointmentController extends HttpServlet {

    // [CÁC TRANG ĐÍCH]
    private static final String LOAD_BOOKING = "LoadBookingController";
    private static final String VIEW_APPOINTMENTS = "ViewAppointmentsController";
    private static final String LOGIN_PAGE = "login.jsp";

    // [TRẠNG THÁI KHỞI TẠO MẶC ĐỊNH CHO LỊCH HẸN MỚI]
    // [TÁC ĐỘNG]: Khớp với ràng buộc status IN ('Pending', 'Completed', 'Cancelled') trong CSDL
    private static final String PENDING = "Pending";

    protected void processRequest(HttpServletRequest request,
                                  HttpServletResponse response)
            throws ServletException, IOException {
        String url = LOAD_BOOKING;
        try {
            // [1. KIỂM TRA PHÂN QUYỀN CUSTOMER VÀ LẤY THÔNG TIN ĐĂNG NHẬP TỪ SESSION]:
            HttpSession session = request.getSession(false);
            User loginUser = session == null
                    ? null : (User) session.getAttribute("LOGIN_USER");

            if (loginUser == null || !loginUser.isCustomer()) {
                request.setAttribute("ERROR", "You must log in as a customer");
                url = LOGIN_PAGE;
            } else {
                // [2. ĐỌC THAM SỐ TỪ FORM ĐẶT LỊCH]:
                String serviceID = request.getParameter("serviceID");
                String appointmentDate = request.getParameter("appointmentDate");

                String error = null;
                Timestamp when = null;

                // [3. KIỂM TRA HỢP LỆ VÀ ÉP KIỂU NGÀY GIỜ HẸN]:
                if (serviceID == null || serviceID.trim().isEmpty()) {
                    error = "Please select a service";
                } else if (appointmentDate == null
                        || appointmentDate.trim().isEmpty()) {
                    error = "Please choose an appointment date";
                } else {
                    when = parseDateTime(appointmentDate.trim());
                    if (when == null) {
                        error = "The appointment date is not a valid date";
                    } else if (!when.after(new Timestamp(
                            System.currentTimeMillis()))) {
                        // RÀNG BUỘC NGHIỆP VỤ: Ngày hẹn phải ở trong tương lai
                        error = "The appointment date must be in the future";
                    }
                }

                // [4. KIỂM TRA DỊCH VỤ ĐƯỢC CHỌN CÒN TỒN TẠI TRONG CSDL]:
                if (error == null) {
                    ServiceDAO serviceDAO = new ServiceDAO();
                    if (serviceDAO.getByID(serviceID.trim()) == null) {
                        error = "The selected service no longer exists";
                    }
                }

                // [5. TẠO ĐỐI TƯỢNG VÀ THÊM VÀO BẢNG TBLAPPOINTMENTS]:
                if (error == null) {
                    AppointmentDAO dao = new AppointmentDAO();
                    Appointment app = new Appointment(
                            dao.getNextID(),              // Tự sinh mã 'appN'
                            loginUser.getUserID(),        // Lấy userID từ Session
                            serviceID.trim(),             // Mã dịch vụ
                            when,                         // Thời gian Timestamp
                            PENDING,                      // Trạng thái 'Pending'
                            null,                         // staffID chưa gán
                            null);                        // consultationNotes chưa có
                    if (dao.insert(app)) {
                        // Đặt lịch thành công -> Chuyển sang ViewAppointmentsController để hiển thị danh sách
                        request.setAttribute("MESSAGE", "Appointment "
                                + app.getAppointmentID() + " has been booked");
                        url = VIEW_APPOINTMENTS;
                    } else {
                        error = "Could not book the appointment, please try again";
                    }
                }

                // [6. NẾU CÓ LỖI: QUAY VỀ LOADBOOKING ĐỂ NẠP LẠI DROPDOWN DỊCH VỤ]:
                if (error != null) {
                    request.setAttribute("ERROR", error);
                    url = LOAD_BOOKING;
                }
            }
        } catch (Exception e) {
            log("Error at BookAppointmentController: " + e.toString());
            request.setAttribute("ERROR", "System error, please try again later");
            url = LOAD_BOOKING;
        } finally {
            request.getRequestDispatcher(url).forward(request, response);
        }
    }

    /**
     * Chuyển đổi định dạng chuỗi từ ô input datetime-local thành đối tượng Timestamp.
     * Ví dụ: "2026-08-20T08:00" -> "2026-08-20 08:00:00" -> java.sql.Timestamp
     * [DỮ LIỆU ĐI TỚI ĐÂU]:
     * - Dùng để gán vào thuộc tính appointmentDate của Appointment và lưu vào cột appointmentDate trong tblAppointments.
     */
    private Timestamp parseDateTime(String raw) {
        try {
            String value = raw.replace('T', ' ');
            if (value.length() == 16) {      // Thiếu phần giây :ss
                value = value + ":00";
            }
            return Timestamp.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}
