package com.spa.controller;

import com.spa.dao.AppointmentDAO;
import com.spa.dto.Appointment;
import com.spa.dto.User;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * ============================================================================
 * [VAI TRÒ & CHỨC NĂNG]
 * - Controller ĐỌC DỮ LIỆU trung tâm của nhóm tư vấn và theo dõi dịch vụ (Action: ViewConsultation - Dành cho Staff).
 * - Lấy `staffID` trực tiếp từ Session (`loginUser.getUserID()`).
 * - Gọi `AppointmentDAO.getByStaff(staffID)` để lấy các lịch hẹn được phân công cho CHÍNH nhân viên đó
 *   (bảo mật: không cho phép xem trộm lịch của nhân viên khác).
 * - Đặt danh sách vào Request Attribute `LIST_CONSULTATION` và forward sang [consultation.jsp].
 * - Là ĐÍCH ĐẾN SAU KHI THAY ĐỔI DỮ LIỆU của:
 *   + SaveNotesController (lưu ghi chú tư vấn xong -> quay về ViewConsultationController để thấy ghi chú mới).
 *   + CompleteAppointmentController (đánh dấu hoàn thành xong -> quay về ViewConsultationController để cập nhật trạng thái).
 *
 * [LUỒNG THỰC THI (EXECUTION FLOW)]
 * 1. Nhân viên bấm "View consultations" trên [dashboard.jsp] hoặc được forward từ SaveNotes/Complete Controller
 *    -> MainController (action=ViewConsultation) -> ViewConsultationController.
 * 2. Kiểm tra quyền Staff trong Session (`loginUser.isStaff()`).
 * 3. Gọi `AppointmentDAO.getByStaff(loginUser.getUserID())` -> nhận `List<Appointment>`.
 * 4. `request.setAttribute("LIST_CONSULTATION", list)`.
 * 5. Forward sang [consultation.jsp].
 *
 * [LUỒNG DỮ LIỆU (DATA FLOW)]
 * - Dữ liệu vào:
 *   + `session.getAttribute("LOGIN_USER")` -> Lấy `userID` của Staff đang đăng nhập.
 * - Dữ liệu ra Request:
 *   + `request.setAttribute("LIST_CONSULTATION", list)` -> Đổ vào bảng tư vấn và theo dõi trên [consultation.jsp].
 *
 * [LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
 * 1. Sửa tên Request Attribute `"LIST_CONSULTATION"`:
 *    -> BẮT BUỘC phải sửa thẻ `<c:forEach items="${LIST_CONSULTATION}">` trong [consultation.jsp].
 * 2. Sửa CONSULTATION_PAGE = "consultation.jsp":
 *    -> Đổi file giao diện trang tư vấn của nhân viên.
 * ============================================================================
 */
public class ViewConsultationController extends HttpServlet {

    // [CÁC TRANG ĐÍCH]
    private static final String CONSULTATION_PAGE = "consultation.jsp";
    private static final String LOGIN_PAGE = "login.jsp";
    private static final String ERROR_PAGE = "error.jsp";

    protected void processRequest(HttpServletRequest request,
                                  HttpServletResponse response)
            throws ServletException, IOException {
        String url = ERROR_PAGE;
        try {
            // [1. KIỂM TRA PHÂN QUYỀN STAFF TỪ SESSION]:
            HttpSession session = request.getSession(false);
            User loginUser = session == null
                    ? null : (User) session.getAttribute("LOGIN_USER");

            if (loginUser == null || !loginUser.isStaff()) {
                request.setAttribute("ERROR",
                        "You must log in as a staff member");
                url = LOGIN_PAGE;
            } else {
                // [2. TRUY VẤN DANH SÁCH LỊCH HẸN PHÂN CÔNG CHO NHÂN VIÊN ĐANG ĐĂNG NHẬP]:
                // BẢO MẬT: staffID lấy từ Session, không lấy từ form input ẩn
                AppointmentDAO dao = new AppointmentDAO();
                List<Appointment> list = dao.getByStaff(loginUser.getUserID());

                // [3. ĐẶT DANH SÁCH VÀO REQUEST ATTRIBUTE ĐỂ HIỂN THỊ TRÊN JSP]:
                request.setAttribute("LIST_CONSULTATION", list);
                url = CONSULTATION_PAGE;
            }
        } catch (Exception e) {
            log("Error at ViewConsultationController: " + e.toString());
            request.setAttribute("ERROR", "Could not load your consultations");
            url = ERROR_PAGE;
        } finally {
            // [4. CHUYỂN TIẾP SANG CONSULTATION.JSP]:
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
