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
 * - Xử lý thao tác LƯU GHI CHÚ TƯ VẤN (Action: SaveNotes - Dành cho Staff).
 * - Nhận `appointmentID` và `notes` (nội dung tư vấn) từ ô `<textarea name="notes">` trên [consultation.jsp].
 * - KIỂM TRA RÀNG BUỘC NGHIỆP VỤ & BẢO MẬT:
 *   1. Nhân viên chỉ được ghi chú cho lịch hẹn được PHÂN CÔNG ĐÚNG CHO MÌNH:
 *      So sánh `loginUser.getUserID().equals(app.getStaffID())`.
 *   2. Độ dài ghi chú <= 1000 ký tự (khớp với cột consultationNotes NVARCHAR(1000) trong CSDL).
 * - Gọi `AppointmentDAO.updateNotes(appointmentID, notes)` để lưu vào CSDL.
 * - Chuyển tiếp (forward) về `ViewConsultationController` để làm mới bảng hiển thị.
 *
 * [LUỒNG THỰC THI (EXECUTION FLOW)]
 * 1. Staff nhập ghi chú và bấm SaveNotes trên [consultation.jsp] -> Form POST gửi MainController -> SaveNotesController.
 * 2. Kiểm tra quyền Staff trong Session (`loginUser.isStaff()`).
 * 3. Đọc tham số `appointmentID` và `notes`.
 * 4. Kiểm tra độ dài ghi chú (notes.length() <= 1000).
 * 5. Gọi `AppointmentDAO.getByID(appointmentID)`:
 *    - Nếu app == null -> Set ERROR "That appointment does not exist".
 *    - Nếu app.getStaffID() != loginUser.getUserID() -> Set ERROR "You can only write notes for an appointment assigned to you".
 *    - Nếu hợp lệ -> Gọi `dao.updateNotes(app.getAppointmentID(), notes)`.
 * 6. Chuyển tiếp về `ViewConsultationController`.
 *
 * [LUỒNG DỮ LIỆU (DATA FLOW)]
 * - Dữ liệu vào:
 *   + `session.getAttribute("LOGIN_USER")` -> UserID của Staff.
 *   + `request.getParameter("appointmentID")` -> Mã lịch hẹn.
 *   + `request.getParameter("notes")` -> Nội dung ghi chú tư vấn.
 * - Dữ liệu xuống DB:
 *   + UPDATE tblAppointments SET consultationNotes = ? WHERE appointmentID = ?
 * - Dữ liệu ra Request:
 *   + Thành công: `request.setAttribute("MESSAGE", "Notes for appointment ... have been saved")` -> [ViewConsultationController].
 *   + Thất bại: `request.setAttribute("ERROR", error)` -> [ViewConsultationController].
 *
 * [LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
 * 1. Sửa giới hạn ký tự ghi chú (1000 ký tự):
 *    -> Phải đồng bộ với cột consultationNotes NVARCHAR(1000) trong [SpaConsultationPortal.sql]
 *       và thuộc tính maxlength="1000" trên [consultation.jsp].
 * 2. Sửa VIEW_CONSULTATION = "ViewConsultationController":
 *    -> Sau khi lưu ghi chú, luôn quay về ViewConsultationController để hiển thị lại dữ liệu vừa lưu.
 * ============================================================================
 */
public class SaveNotesController extends HttpServlet {

    // [CÁC TRANG ĐÍCH]
    private static final String VIEW_CONSULTATION = "ViewConsultationController";
    private static final String LOGIN_PAGE = "login.jsp";

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
                // [2. ĐỌC THAM SỐ GHI CHÚ VÀ MÃ LỊCH HẸN TỪ FORM]:
                String appointmentID = request.getParameter("appointmentID");
                String notes = request.getParameter("notes");

                // [3. KIỂM TRA HỢP LỆ VÀ ĐỘ DÀI GHI CHÚ]:
                if (appointmentID == null || appointmentID.trim().isEmpty()) {
                    request.setAttribute("ERROR", "No appointment was selected");
                } else if (notes != null && notes.trim().length() > 1000) {
                    request.setAttribute("ERROR",
                            "Consultation notes must not exceed 1000 characters");
                } else {
                    // [4. KIỂM TRA LỊCH HẸN CÓ ĐƯỢC PHÂN CÔNG ĐÚNG CHO NHÂN VIÊN ĐANG ĐĂNG NHẬP KHÔNG]:
                    AppointmentDAO dao = new AppointmentDAO();
                    Appointment app = dao.getByID(appointmentID.trim());

                    if (app == null) {
                        request.setAttribute("ERROR",
                                "That appointment does not exist");
                    } else if (!loginUser.getUserID().equals(app.getStaffID())) {
                        // BẢO MẬT: Chống nhân viên này ghi đè ghi chú vào lịch của nhân viên khác
                        request.setAttribute("ERROR", "You can only write notes"
                                + " for an appointment assigned to you");
                    } else if (dao.updateNotes(app.getAppointmentID(), notes)) {
                        // [5. LƯU GHI CHÚ THÀNH CÔNG VÀO CSDL]:
                        request.setAttribute("MESSAGE", "Notes for appointment "
                                + app.getAppointmentID() + " have been saved");
                    } else {
                        request.setAttribute("ERROR", "Could not save the notes");
                    }
                }
            }
        } catch (Exception e) {
            log("Error at SaveNotesController: " + e.toString());
            request.setAttribute("ERROR", "Could not save the notes");
            url = VIEW_CONSULTATION;
        } finally {
            // [6. CHUYỂN TIẾP VỀ VIEWCONSULTATIONCONTROLLER ĐỂ TẢI LẠI TRANG]:
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
