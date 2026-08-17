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
 * - Controller ĐỌC DỮ LIỆU trung tâm của nhóm đặt lịch (Action: ViewAppointments - Dành cho Customer).
 * - Lấy `userID` của khách hàng trực tiếp từ Session (`loginUser.getUserID()`).
 * - Gọi `AppointmentDAO.getByUser(userID)` để lấy toàn bộ lịch hẹn của khách hàng đó
 *   (kèm theo tên dịch vụ và tên nhân viên phụ trách nhờ phép JOIN trong DAO).
 * - Đặt danh sách vào Request Attribute `LIST_APPOINTMENT` và forward sang [appointmentList.jsp].
 * - Là ĐÍCH ĐẾN SAU KHI THAY ĐỔI DỮ LIỆU của:
 *   + BookAppointmentController (đặt lịch xong -> chuyển sang ViewAppointmentsController để thấy lịch mới).
 *   + CancelAppointmentController (hủy lịch xong -> chuyển sang ViewAppointmentsController để thấy trạng thái Cancelled).
 *
 * [LUỒNG THỰC THI (EXECUTION FLOW)]
 * 1. Khách hàng bấm "My appointments" trên [dashboard.jsp] hoặc được forward từ Book/Cancel Controller
 *    -> MainController (action=ViewAppointments) -> ViewAppointmentsController.
 * 2. Kiểm tra quyền Customer trong Session.
 * 3. Gọi `AppointmentDAO.getByUser(loginUser.getUserID())` -> nhận `List<Appointment>`.
 * 4. `request.setAttribute("LIST_APPOINTMENT", list)`.
 * 5. Forward sang [appointmentList.jsp].
 *
 * [LUỒNG DỮ LIỆU (DATA FLOW)]
 * - Dữ liệu vào:
 *   + `session.getAttribute("LOGIN_USER")` -> Lấy `userID` của khách hàng đang đăng nhập.
 * - Dữ liệu ra Request:
 *   + `request.setAttribute("LIST_APPOINTMENT", list)` -> Đổ vào bảng lịch hẹn trên [appointmentList.jsp].
 *
 * [LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
 * 1. Sửa tên Request Attribute `"LIST_APPOINTMENT"`:
 *    -> BẮT BUỘC phải sửa thẻ `<c:forEach items="${LIST_APPOINTMENT}">` trong [appointmentList.jsp].
 * 2. Sửa APPOINTMENT_LIST_PAGE = "appointmentList.jsp":
 *    -> Đổi file giao diện danh sách lịch hẹn của khách hàng.
 * ============================================================================
 */
public class ViewAppointmentsController extends HttpServlet {

    // [CÁC TRANG ĐÍCH]
    private static final String APPOINTMENT_LIST_PAGE = "appointmentList.jsp";
    private static final String LOGIN_PAGE = "login.jsp";
    private static final String ERROR_PAGE = "error.jsp";

    protected void processRequest(HttpServletRequest request,
                                  HttpServletResponse response)
            throws ServletException, IOException {
        String url = ERROR_PAGE;
        try {
            // [1. KIỂM TRA PHÂN QUYỀN CUSTOMER VÀ LẤY THÔNG TIN ĐĂNG NHẬP TỪ SESSION]:
            HttpSession session = request.getSession(false);
            User loginUser = session == null
                    ? null : (User) session.getAttribute("LOGIN_USER");

            if (loginUser == null || !loginUser.isCustomer()) {
                request.setAttribute("ERROR", "You must log in as a customer");
                url = LOGIN_PAGE;
            } else {
                // [2. TRUY VẤN TOÀN BỘ LỊCH HẸN CỦA KHÁCH HÀNG NÀY QUA APPOINTMENTDAO]:
                AppointmentDAO dao = new AppointmentDAO();
                List<Appointment> list = dao.getByUser(loginUser.getUserID());

                // [3. ĐẶT DANH SÁCH VÀO REQUEST ATTRIBUTE ĐỂ HIỂN THỊ TRÊN JSP]:
                request.setAttribute("LIST_APPOINTMENT", list);
                url = APPOINTMENT_LIST_PAGE;
            }
        } catch (Exception e) {
            log("Error at ViewAppointmentsController: " + e.toString());
            request.setAttribute("ERROR", "Could not load your appointments");
            url = ERROR_PAGE;
        } finally {
            // [4. CHUYỂN TIẾP SANG APPOINTMENTLIST.JSP]:
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
