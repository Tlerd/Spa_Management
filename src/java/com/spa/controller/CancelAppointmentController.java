package com.spa.controller;

import com.spa.dao.AppointmentDAO;
import com.spa.dto.Appointment;
import com.spa.dto.User;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * ============================================================================
 * [VAI TRÒ & CHỨC NĂNG]
 * - Xử lý thao tác HỦY LỊCH HẸN (Action: CancelAppointment - Dành cho Customer).
 * - Nhận `appointmentID` từ form submit nút Cancel trên [appointmentList.jsp].
 * - KIỂM TRA RÀNG BUỘC NGHIỆP VỤ & BẢO MẬT PHÍA SERVER:
 *   1. Khách hàng chỉ được hủy lịch hẹn của CHÍNH MÌNH:
 *      So sánh `loginUser.getUserID().equals(app.getUserID())`.
 *   2. Chỉ được phép hủy khi lịch hẹn đang ở trạng thái "Pending" (`app.isPending()`).
 *      Lịch đã 'Completed' hoặc đã 'Cancelled' không được phép hủy lại.
 * - Gọi `AppointmentDAO.updateStatus(appointmentID, "Cancelled")` để cập nhật CSDL.
 * - Chuyển tiếp (forward) về `ViewAppointmentsController` để nạp lại danh sách lịch hẹn mới nhất.
 *
 * [LUỒNG THỰC THI (EXECUTION FLOW)]
 * 1. Khách bấm Cancel trên [appointmentList.jsp] -> Form POST gửi MainController (action=CancelAppointment) -> CancelAppointmentController.
 * 2. Kiểm tra quyền Customer trong Session.
 * 3. Đọc tham số `appointmentID = request.getParameter("appointmentID")`.
 * 4. Gọi `AppointmentDAO.getByID(appointmentID)`:
 *    - Nếu app == null -> Set ERROR "That appointment does not exist".
 *    - Nếu app.getUserID() != loginUser.getUserID() -> Set ERROR "You can only cancel your own appointment".
 *    - Nếu !app.isPending() -> Set ERROR "Only a pending appointment can be cancelled".
 *    - Nếu thỏa mãn tất cả -> `dao.updateStatus(app.getAppointmentID(), "Cancelled")`.
 * 5. Chuyển tiếp về `ViewAppointmentsController`.
 *
 * [LUỒNG DỮ LIỆU (DATA FLOW)]
 * - Dữ liệu vào:
 *   + `session.getAttribute("LOGIN_USER")` -> UserID của khách hàng đang đăng nhập.
 *   + `request.getParameter("appointmentID")` -> Mã lịch hẹn muốn hủy (từ thẻ <input type="hidden">).
 * - Dữ liệu xuống DB:
 *   + UPDATE tblAppointments SET status = 'Cancelled' WHERE appointmentID = ?
 * - Dữ liệu ra Request:
 *   + Thành công: `request.setAttribute("MESSAGE", "Appointment ... has been cancelled")` -> [ViewAppointmentsController] -> [appointmentList.jsp].
 *   + Thất bại: `request.setAttribute("ERROR", error)` -> [ViewAppointmentsController] -> [appointmentList.jsp].
 *
 * [LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
 * 1. Sửa CANCELLED = "Cancelled":
 *    -> Phải đồng bộ với CK_App_Status CHECK constraint trong [SpaConsultationPortal.sql].
 * 2. Sửa VIEW_APPOINTMENTS = "ViewAppointmentsController":
 *    -> Luôn quay về ViewAppointmentsController sau khi hủy để làm mới dữ liệu trên giao diện bảng.
 * ============================================================================
 */
public class CancelAppointmentController extends HttpServlet {

    // [CÁC TRANG ĐÍCH]
    private static final String VIEW_APPOINTMENTS = "ViewAppointmentsController";
    private static final String LOGIN_PAGE = "login.jsp";

    // [TRẠNG THÁI HỦY LỊCH HẸN]
    // [TÁC ĐỘNG]: Khớp với ràng buộc status='Cancelled' trong bảng tblAppointments
    private static final String CANCELLED = "Cancelled";

    protected void processRequest(HttpServletRequest request,
                                  HttpServletResponse response)
            throws ServletException, IOException {
        String url = VIEW_APPOINTMENTS;
        try {
            // [1. KIỂM TRA PHÂN QUYỀN CUSTOMER TỪ SESSION]:
            HttpSession session = request.getSession(false);
            User loginUser = session == null
                    ? null : (User) session.getAttribute("LOGIN_USER");

            if (loginUser == null || !loginUser.isCustomer()) {
                request.setAttribute("ERROR", "You must log in as a customer");
                url = LOGIN_PAGE;
            } else {
                // [2. ĐỌC MÃ LỊCH HẸN TỪ REQUEST]:
                String appointmentID = request.getParameter("appointmentID");
                if (appointmentID == null || appointmentID.trim().isEmpty()) {
                    request.setAttribute("ERROR", "No appointment was selected");
                } else {
                    // [3. ĐỌC THÔNG TIN LỊCH HẸN TỪ CSDL ĐỂ KIỂM TRA RÀNG BUỘC BẢO MẬT]:
                    AppointmentDAO dao = new AppointmentDAO();
                    Appointment app = dao.getByID(appointmentID.trim());

                    if (app == null) {
                        request.setAttribute("ERROR",
                                "That appointment does not exist");
                    } else if (!loginUser.getUserID().equals(app.getUserID())) {
                        // BẢO MẬT: Chống giả mạo request hủy lịch hẹn của khách hàng khác
                        request.setAttribute("ERROR",
                                "You can only cancel your own appointment");
                    } else if (!app.isPending()) {
                        // NGHIỆP VỤ: Chỉ cho phép hủy khi lịch hẹn đang ở trạng thái 'Pending'
                        request.setAttribute("ERROR",
                                "Only a pending appointment can be cancelled");
                    } else if (dao.updateStatus(app.getAppointmentID(), CANCELLED)) {
                        // [4. CẬP NHẬT TRẠNG THÁI SANG 'CANCELLED' THÀNH CÔNG]:
                        request.setAttribute("MESSAGE", "Appointment "
                                + app.getAppointmentID() + " has been cancelled");
                    } else {
                        request.setAttribute("ERROR",
                                "Could not cancel the appointment");
                    }
                }
            }
        } catch (Exception e) {
            log("Error at CancelAppointmentController: " + e.toString());
            request.setAttribute("ERROR", "Could not cancel the appointment");
            url = VIEW_APPOINTMENTS;
        } finally {
            // [5. CHUYỂN TIẾP VỀ VIEWAPPOINTMENTSCONTROLLER ĐỂ HIỂN THỊ LẠI DANH SÁCH]:
            request.getRequestDispatcher(url).forward(request, response);
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
