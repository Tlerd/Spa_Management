package com.spa.controller;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * ============================================================================
 * [VAI TRÒ & CHỨC NĂNG]
 * - MainController đóng vai trò là Front Controller (Bộ điều phối trung tâm) trong mô hình MVC2.
 * - Là "CỬA NGÕ DUY NHẤT" tiếp nhận mọi Request từ Client gửi tới ứng dụng:
 *   + Không tự thực thi câu lệnh SQL.
 *   + Không in HTML ra response.
 *   + Không chứa logic nghiệp vụ chi tiết.
 *   + Nhiệm vụ duy nhất: Đọc tham số `action`, đối chiếu với danh sách các action đã định nghĩa,
 *     sau đó chuyển tiếp (forward) Request tới Servlet chức năng (Sub-controller) tương ứng.
 *
 * [LUỒNG THỰC THI (EXECUTION FLOW)]
 * 1. Mọi Form submit (POST) hoặc Link (GET) trên các trang JSP đều trỏ về:
 *    `action="MainController"` (POST) hoặc `href="MainController?action=..."` (GET).
 * 2. Cấu hình UTF-8 được đặt ngay đầu: `request.setCharacterEncoding("UTF-8")`
 *    -> Tất cả 17 Sub-controller phía sau không cần phải lặp lại cấu hình này.
 * 3. Đọc tham số `String action = request.getParameter("action")`.
 * 4. So sánh `action` với các hằng số:
 *    - null -> Chuyển về login.jsp
 *    - "Login" -> LoginController
 *    - "Logout" -> LogoutController
 *    - "Register" -> RegisterController
 *    - "ViewServices" -> ViewServicesController
 *    - "CreateService" -> CreateServiceController
 *    - "LoadService" -> LoadServiceController
 *    - "UpdateService" -> UpdateServiceController
 *    - "DeleteService" -> DeleteServiceController
 *    - "LoadBooking" -> LoadBookingController
 *    - "BookAppointment" -> BookAppointmentController
 *    - "ViewAppointments" -> ViewAppointmentsController
 *    - "CancelAppointment" -> CancelAppointmentController
 *    - "LoadReview" -> LoadReviewController
 *    - "ReviewService" -> ReviewServiceController
 *    - "ViewConsultation" -> ViewConsultationController
 *    - "SaveNotes" -> SaveNotesController
 *    - "CompleteAppointment" -> CompleteAppointmentController
 *    - action không hợp lệ -> error.jsp kèm thông báo lỗi
 * 5. Khối `finally`: Gọi `request.getRequestDispatcher(url).forward(request, response)` duy nhất 1 lần.
 *
 * [LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
 * 1. Sửa giá trị của bất kỳ hằng số ACTION nào (ví dụ sửa LOGIN = "Login" thành "DoLogin"):
 *    -> Phải sửa thuộc tính `value` của nút submit hoặc giá trị tham số trên URL trong TẤT CẢ các file JSP:
 *       + login.jsp, register.jsp, dashboard.jsp, serviceList.jsp, createService.jsp,
 *         updateService.jsp, bookAppointment.jsp, appointmentList.jsp, consultation.jsp, reviewService.jsp.
 * 2. Sửa tên URL Sub-controller (ví dụ sửa LOGIN_CONTROLLER = "LoginController" thành "LoginServlet"):
 *    -> Phải sửa cấu hình <url-pattern> tương ứng trong tệp [web.xml].
 * 3. Nếu thêm một chức năng mới vào hệ thống:
 *    -> Bắt buộc phải khai báo thêm Hằng số Action và Hằng số Controller tại lớp này.
 *    -> Thêm nhánh `else if (NEW_ACTION.equals(action))` trong processRequest.
 *    -> Đăng ký Servlet mới trong [web.xml].
 * ============================================================================
 */
public class MainController extends HttpServlet {

    // [CÁC TRANG CƠ BẢN]
    // [TÁC ĐỘNG]: Đường dẫn file JSP chào mừng và hiển thị lỗi
    private static final String LOGIN_PAGE = "login.jsp";
    private static final String ERROR_PAGE = "error.jsp";

    // ==========================================
    // 1. NHÓM XÁC THỰC (AUTHENTICATION)
    // ==========================================
    // [TÁC ĐỘNG]: Sửa LOGIN -> Sửa nút submit trên [login.jsp]
    private static final String LOGIN = "Login";
    private static final String LOGIN_CONTROLLER = "LoginController"; // Khớp web.xml: /LoginController

    // [TÁC ĐỘNG]: Sửa LOGOUT -> Sửa nút submit trên [dashboard.jsp]
    private static final String LOGOUT = "Logout";
    private static final String LOGOUT_CONTROLLER = "LogoutController"; // Khớp web.xml: /LogoutController

    // [TÁC ĐỘNG]: Sửa REGISTER -> Sửa nút submit trên [register.jsp]
    private static final String REGISTER = "Register";
    private static final String REGISTER_CONTROLLER = "RegisterController"; // Khớp web.xml: /RegisterController

    // ==========================================
    // 2. NHÓM QUẢN LÝ DỊCH VỤ - DÀNH CHO ADMIN
    // ==========================================
    // [TÁC ĐỘNG]: Sửa VIEW_SERVICES -> Sửa [dashboard.jsp], [serviceList.jsp], [createService.jsp], [updateService.jsp]
    private static final String VIEW_SERVICES = "ViewServices";
    private static final String VIEW_SERVICES_CONTROLLER = "ViewServicesController"; // Khớp web.xml

    // [TÁC ĐỘNG]: Sửa CREATE_SERVICE -> Sửa nút submit trên [createService.jsp]
    private static final String CREATE_SERVICE = "CreateService";
    private static final String CREATE_SERVICE_CONTROLLER = "CreateServiceController"; // Khớp web.xml

    // [TÁC ĐỘNG]: Sửa LOAD_SERVICE -> Sửa link Edit trên [serviceList.jsp]
    private static final String LOAD_SERVICE = "LoadService";
    private static final String LOAD_SERVICE_CONTROLLER = "LoadServiceController"; // Khớp web.xml

    // [TÁC ĐỘNG]: Sửa UPDATE_SERVICE -> Sửa nút submit trên [updateService.jsp]
    private static final String UPDATE_SERVICE = "UpdateService";
    private static final String UPDATE_SERVICE_CONTROLLER = "UpdateServiceController"; // Khớp web.xml

    // [TÁC ĐỘNG]: Sửa DELETE_SERVICE -> Sửa nút submit trên [serviceList.jsp]
    private static final String DELETE_SERVICE = "DeleteService";
    private static final String DELETE_SERVICE_CONTROLLER = "DeleteServiceController"; // Khớp web.xml

    // ==========================================
    // 3. NHÓM ĐẶT LỊCH HẸN - DÀNH CHO KHÁCH HÀNG
    // ==========================================
    // [TÁC ĐỘNG]: Sửa LOAD_BOOKING -> Sửa link trên [dashboard.jsp]
    private static final String LOAD_BOOKING = "LoadBooking";
    private static final String LOAD_BOOKING_CONTROLLER = "LoadBookingController"; // Khớp web.xml

    // [TÁC ĐỘNG]: Sửa BOOK_APPOINTMENT -> Sửa nút submit trên [bookAppointment.jsp]
    private static final String BOOK_APPOINTMENT = "BookAppointment";
    private static final String BOOK_APPOINTMENT_CONTROLLER = "BookAppointmentController"; // Khớp web.xml

    // [TÁC ĐỘNG]: Sửa VIEW_APPOINTMENTS -> Sửa link trên [dashboard.jsp]
    private static final String VIEW_APPOINTMENTS = "ViewAppointments";
    private static final String VIEW_APPOINTMENTS_CONTROLLER = "ViewAppointmentsController"; // Khớp web.xml

    // [TÁC ĐỘNG]: Sửa CANCEL_APPOINTMENT -> Sửa nút submit trên [appointmentList.jsp]
    private static final String CANCEL_APPOINTMENT = "CancelAppointment";
    private static final String CANCEL_APPOINTMENT_CONTROLLER = "CancelAppointmentController"; // Khớp web.xml

    // ==========================================
    // 4. NHÓM ĐÁNH GIÁ DỊCH VỤ - DÀNH CHO KHÁCH HÀNG
    // ==========================================
    // [TÁC ĐỘNG]: Sửa LOAD_REVIEW -> Sửa link trên [dashboard.jsp] và [appointmentList.jsp]
    private static final String LOAD_REVIEW = "LoadReview";
    private static final String LOAD_REVIEW_CONTROLLER = "LoadReviewController"; // Khớp web.xml

    // [TÁC ĐỘNG]: Sửa REVIEW_SERVICE -> Sửa nút submit trên [reviewService.jsp]
    private static final String REVIEW_SERVICE = "ReviewService";
    private static final String REVIEW_SERVICE_CONTROLLER = "ReviewServiceController"; // Khớp web.xml

    // ==========================================
    // 5. NHÓM TƯ VẤN & THEO DÕI - DÀNH CHO NHÂN VIÊN (STAFF)
    // ==========================================
    // [TÁC ĐỘNG]: Sửa VIEW_CONSULTATION -> Sửa link trên [dashboard.jsp]
    private static final String VIEW_CONSULTATION = "ViewConsultation";
    private static final String VIEW_CONSULTATION_CONTROLLER = "ViewConsultationController"; // Khớp web.xml

    // [TÁC ĐỘNG]: Sửa SAVE_NOTES -> Sửa nút submit SaveNotes trên [consultation.jsp]
    private static final String SAVE_NOTES = "SaveNotes";
    private static final String SAVE_NOTES_CONTROLLER = "SaveNotesController"; // Khớp web.xml

    // [TÁC ĐỘNG]: Sửa COMPLETE_APPOINTMENT -> Sửa nút submit Complete trên [consultation.jsp]
    private static final String COMPLETE_APPOINTMENT = "CompleteAppointment";
    private static final String COMPLETE_APPOINTMENT_CONTROLLER = "CompleteAppointmentController"; // Khớp web.xml

    /**
     * Xử lý chính: Nhận mọi HTTP GET / POST, phân tích tham số `action` và chuyển tiếp Request.
     */
    protected void processRequest(HttpServletRequest request,
                                  HttpServletResponse response)
            throws ServletException, IOException {
        // [THIẾT LẬP BẢNG MÃ UTF-8 ĐẦU TIÊN TRƯỚC KHI ĐỌC PARAMETER]
        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");
        String url = LOGIN_PAGE;
        try {
            // [ĐỌC THAM SỐ ACTION]: Lấy từ query string (?action=...) hoặc form input (name="action")
            String action = request.getParameter("action");

            // [ĐIỀU HƯỚNG ACTION THEO MÔ HÌNH BỘ ĐIỀU PHỐI (DISPATCHER)]
            if (action == null) {
                // Người dùng truy cập trực tiếp MainController mà không có action -> về login.jsp
                url = LOGIN_PAGE;
            } else if (LOGIN.equals(action)) {
                url = LOGIN_CONTROLLER;
            } else if (LOGOUT.equals(action)) {
                url = LOGOUT_CONTROLLER;
            } else if (REGISTER.equals(action)) {
                url = REGISTER_CONTROLLER;
            } else if (VIEW_SERVICES.equals(action)) {
                url = VIEW_SERVICES_CONTROLLER;
            } else if (CREATE_SERVICE.equals(action)) {
                url = CREATE_SERVICE_CONTROLLER;
            } else if (LOAD_SERVICE.equals(action)) {
                url = LOAD_SERVICE_CONTROLLER;
            } else if (UPDATE_SERVICE.equals(action)) {
                url = UPDATE_SERVICE_CONTROLLER;
            } else if (DELETE_SERVICE.equals(action)) {
                url = DELETE_SERVICE_CONTROLLER;
            } else if (LOAD_BOOKING.equals(action)) {
                url = LOAD_BOOKING_CONTROLLER;
            } else if (BOOK_APPOINTMENT.equals(action)) {
                url = BOOK_APPOINTMENT_CONTROLLER;
            } else if (VIEW_APPOINTMENTS.equals(action)) {
                url = VIEW_APPOINTMENTS_CONTROLLER;
            } else if (CANCEL_APPOINTMENT.equals(action)) {
                url = CANCEL_APPOINTMENT_CONTROLLER;
            } else if (LOAD_REVIEW.equals(action)) {
                url = LOAD_REVIEW_CONTROLLER;
            } else if (REVIEW_SERVICE.equals(action)) {
                url = REVIEW_SERVICE_CONTROLLER;
            } else if (VIEW_CONSULTATION.equals(action)) {
                url = VIEW_CONSULTATION_CONTROLLER;
            } else if (SAVE_NOTES.equals(action)) {
                url = SAVE_NOTES_CONTROLLER;
            } else if (COMPLETE_APPOINTMENT.equals(action)) {
                url = COMPLETE_APPOINTMENT_CONTROLLER;
            } else {
                // Action lạ không được hỗ trợ -> Đưa tới error.jsp
                request.setAttribute("ERROR",
                        "Action not supported: " + action);
                url = ERROR_PAGE;
            }
        } catch (Exception e) {
            log("Error at MainController: " + e.toString());
            request.setAttribute("ERROR",
                    "System error, please try again later");
            url = ERROR_PAGE;
        } finally {
            // [CHUYỂN TIẾP DUY NHẤT 1 LẦN]: Giữ nguyên Request & Response sang Servlet đích
            request.getRequestDispatcher(url).forward(request, response);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    public String getServletInfo() {
        return "Front controller for Spa Consultation Portal";
    }
}
