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
 * - Xử lý thao tác ĐÁNH DẤU HOÀN THÀNH LỊCH HẸN (Action: CompleteAppointment - Dành cho Staff).
 * - ĐÂY LÀ MẮT XÍCH QUAN TRỌNG NHẤT TRONG TOÀN BỘ CHUỖI NGHIỆP VỤ:
 *   + Khi nhân viên bấm "CompleteAppointment", lịch hẹn chuyển từ trạng thái 'Pending' sang 'Completed'.
 *   + Trạng thái 'Completed' là điều kiện tiên quyết để Khách hàng có thể nhìn thấy dịch vụ
 *     trong danh sách đánh giá tại [reviewService.jsp]. Nếu thiếu chức năng này, không có dịch vụ nào
 *     được phép đánh giá.
 * - KIỂM TRA RÀNG BUỘC NGHIỆP VỤ & BẢO MẬT:
 *   1. Chỉ nhân viên ĐƯỢC PHÂN CÔNG cho lịch hẹn đó mới có quyền bấm hoàn thành
 *      (`loginUser.getUserID().equals(app.getStaffID())`).
 *   2. Chỉ lịch hẹn đang ở trạng thái 'Pending' mới được phép chuyển sang 'Completed' (`app.isPending()`).
 * - Gọi `AppointmentDAO.updateStatus(appointmentID, "Completed")`.
 * - Chuyển tiếp (forward) về `ViewConsultationController` để làm mới danh sách tư vấn.
 *
 * [LUỒNG THỰC THI (EXECUTION FLOW)]
 * 1. Staff bấm nút Complete trên [consultation.jsp] -> Form POST gửi MainController -> CompleteAppointmentController.
 * 2. Kiểm tra quyền Staff trong Session (`loginUser.isStaff()`).
 * 3. Đọc tham số `appointmentID`.
 * 4. Gọi `AppointmentDAO.getByID(appointmentID)`:
 *    - Nếu app == null -> Set ERROR "That appointment does not exist".
 *    - Nếu app.getStaffID() != loginUser.getUserID() -> Set ERROR "You can only complete an appointment assigned to you".
 *    - Nếu !app.isPending() -> Set ERROR "Only a pending appointment can be completed".
 *    - Nếu thỏa mãn -> `dao.updateStatus(app.getAppointmentID(), "Completed")`.
 * 5. Chuyển tiếp về `ViewConsultationController`.
 *
 * [LUỒNG DỮ LIỆU (DATA FLOW)]
 * - Dữ liệu vào:
 *   + `session.getAttribute("LOGIN_USER")` -> UserID của Staff đang đăng nhập.
 *   + `request.getParameter("appointmentID")` -> Mã lịch hẹn cần hoàn thành.
 * - Dữ liệu xuống DB:
 *   + UPDATE tblAppointments SET status = 'Completed' WHERE appointmentID = ?
 * - Dữ liệu ra Request:
 *   + Thành công: `request.setAttribute("MESSAGE", "Appointment ... is now completed")` -> [ViewConsultationController].
 *   + Thất bại: `request.setAttribute("ERROR", error)` -> [ViewConsultationController].
 *
 * [LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
 * 1. Sửa COMPLETED = "Completed":
 *    -> BẮT BUỘC phải đồng bộ với:
 *       + Ràng buộc CK_App_Status CHECK (status IN ('Pending', 'Completed', 'Cancelled')) trong [SpaConsultationPortal.sql].
 *       + Câu truy vấn lọc dịch vụ đánh giá GET_REVIEWABLE (`a.status = 'Completed'`) trong [ReviewDAO.java].
 *       + Kiểm tra `${app.completed}` trong [Appointment.java] và [appointmentList.jsp].
 * 2. Sửa VIEW_CONSULTATION = "ViewConsultationController":
 *    -> Luôn quay về ViewConsultationController để làm mới giao diện.
 * ============================================================================
 */
public class CompleteAppointmentController extends HttpServlet {

    // [CÁC TRANG ĐÍCH]
    private static final String VIEW_CONSULTATION = "ViewConsultationController";
    private static final String LOGIN_PAGE = "login.jsp";

    // [TRẠNG THÁI HOÀN THÀNH LỊCH HẸN]
    // [TÁC ĐỘNG]: Giá trị này kích hoạt điều kiện cho phép Khách hàng đánh giá dịch vụ trong ReviewDAO
    private static final String COMPLETED = "Completed";

    protected void processRequest(HttpServletRequest request,
                                  HttpServletResponse response)
            throws ServletException, IOException {
        String url = VIEW_CONSULTATION;
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
                // [2. ĐỌC MÃ LỊCH HẸN TỪ REQUEST]:
                String appointmentID = request.getParameter("appointmentID");
                if (appointmentID == null || appointmentID.trim().isEmpty()) {
                    request.setAttribute("ERROR", "No appointment was selected");
                } else {
                    // [3. ĐỌC LỊCH HẸN TỪ CSDL ĐỂ KIỂM TRA RÀNG BUỘC]:
                    AppointmentDAO dao = new AppointmentDAO();
                    Appointment app = dao.getByID(appointmentID.trim());

                    if (app == null) {
                        request.setAttribute("ERROR",
                                "That appointment does not exist");
                    } else if (!loginUser.getUserID().equals(app.getStaffID())) {
                        // BẢO MẬT: Chống nhân viên này bấm hoàn thành lịch của nhân viên khác
                        request.setAttribute("ERROR", "You can only complete an"
                                + " appointment assigned to you");
                    } else if (!app.isPending()) {
                        // NGHIỆP VỤ: Chỉ lịch hẹn đang ở trạng thái 'Pending' mới được hoàn thành
                        request.setAttribute("ERROR",
                                "Only a pending appointment can be completed");
                    } else if (dao.updateStatus(app.getAppointmentID(), COMPLETED)) {
                        // [4. CẬP NHẬT TRẠNG THÁI SANG 'COMPLETED' THÀNH CÔNG]:
                        request.setAttribute("MESSAGE", "Appointment "
                                + app.getAppointmentID() + " is now completed");
                    } else {
                        request.setAttribute("ERROR",
                                "Could not complete the appointment");
                    }
                }
            }
        } catch (Exception e) {
            log("Error at CompleteAppointmentController: " + e.toString());
            request.setAttribute("ERROR",
                    "Could not complete the appointment");
            url = VIEW_CONSULTATION;
        } finally {
            // [5. CHUYỂN TIẾP VỀ VIEWCONSULTATIONCONTROLLER ĐỂ TẢI LẠI TRANG]:
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
