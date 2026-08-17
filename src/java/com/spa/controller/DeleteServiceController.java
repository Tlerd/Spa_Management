package com.spa.controller;

import com.spa.dao.ServiceDAO;
import com.spa.dto.User;
import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * ============================================================================
 * [VAI TRÒ & CHỨC NĂNG]
 * - Xử lý thao tác XÓA DỊCH VỤ SPA (Action: DeleteService - Dành cho Admin).
 * - Nhận `serviceID` từ form submit nút Delete trên từng dòng của [serviceList.jsp].
 * - Kiểm tra quyền Admin từ Session.
 * - Gọi `ServiceDAO.delete(serviceID)`:
 *   + Nếu xóa thành công: Đặt thông báo thành công `MESSAGE` và quay về `ViewServicesController`.
 *   + Nếu dịch vụ không tồn tại: Đặt `ERROR` và quay về `ViewServicesController`.
 * - BẮT NGOẠI LỆ RÀNG BUỘC KHÓA NGOẠI (Foreign Key Violation - SQL Error Code 547):
 *   Nếu dịch vụ đang được tham chiếu bởi lịch hẹn (tblAppointments) hoặc đánh giá (tblReviews),
 *   SQL Server sẽ ném lỗi 547. Controller bắt lỗi này và thông báo rõ ràng cho Admin:
 *   "Service ... cannot be deleted because it is used by an appointment or a review"
 *   thay vì để văng lỗi 500 ra màn hình.
 *
 * [LUỒNG THỰC THI (EXECUTION FLOW)]
 * 1. Admin bấm nút "DeleteService" trên [serviceList.jsp] -> Form POST gửi tới MainController (action=DeleteService) -> DeleteServiceController.
 * 2. Kiểm tra quyền Admin trong Session (`loginUser.isAdmin()`).
 * 3. Đọc tham số `serviceID = request.getParameter("serviceID")`.
 * 4. Gọi `ServiceDAO.delete(serviceID)`:
 *    - Bắt `SQLException`: Nếu `sqle.getErrorCode() == 547` -> Set ERROR giải thích ràng buộc khóa ngoại.
 * 5. Chuyển tiếp (forward) về `ViewServicesController` để tải lại danh sách dịch vụ mới nhất.
 *
 * [LUỒNG DỮ LIỆU (DATA FLOW)]
 * - Dữ liệu vào:
 *   + `request.getParameter("serviceID")` -> Mã dịch vụ cần xóa (từ thẻ <input type="hidden" name="serviceID">).
 * - Dữ liệu xuống DB:
 *   + DELETE FROM tblServices WHERE serviceID = ?
 * - Dữ liệu ra Request:
 *   + Thành công: `request.setAttribute("MESSAGE", "Service ... deleted")` -> [ViewServicesController] -> [serviceList.jsp].
 *   + Thất bại / Bị khóa ngoại: `request.setAttribute("ERROR", ...)` -> [ViewServicesController] -> [serviceList.jsp].
 *
 * [LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
 * 1. Mã lỗi CSDL `FK_VIOLATION = 547`:
 *    -> Đây là mã lỗi chuẩn của Microsoft SQL Server cho vi phạm Foreign Key.
 * 2. Sửa VIEW_SERVICES = "ViewServicesController":
 *    -> Sau khi xóa, bắt buộc quay về ViewServicesController để đồng bộ lại dữ liệu trong CSDL với giao diện bảng.
 * ============================================================================
 */
public class DeleteServiceController extends HttpServlet {

    // [CÁC TRANG ĐÍCH]
    private static final String VIEW_SERVICES = "ViewServicesController";
    private static final String LOGIN_PAGE = "login.jsp";

    // [MÃ LỖI VI PHẠM KHÓA NGOẠI CỦA SQL SERVER]
    // [TÁC ĐỘNG]: Ràng buộc khóa ngoại với tblAppointments(serviceID) và tblReviews(serviceID)
    private static final int FK_VIOLATION = 547;

    protected void processRequest(HttpServletRequest request,
                                  HttpServletResponse response)
            throws ServletException, IOException {
        String url = VIEW_SERVICES;
        try {
            // [1. KIỂM TRA PHÂN QUYỀN ADMIN TỪ SESSION]:
            HttpSession session = request.getSession(false);
            User loginUser = session == null
                    ? null : (User) session.getAttribute("LOGIN_USER");

            if (loginUser == null || !loginUser.isAdmin()) {
                request.setAttribute("ERROR",
                        "You must log in as an administrator");
                url = LOGIN_PAGE;
            } else {
                // [2. ĐỌC MÃ DỊCH VỤ CẦN XÓA TỪ REQUEST]:
                String serviceID = request.getParameter("serviceID");
                if (serviceID == null || serviceID.trim().isEmpty()) {
                    request.setAttribute("ERROR", "No service was selected");
                } else {
                    ServiceDAO dao = new ServiceDAO();
                    try {
                        // [3. THỰC HIỆN XÓA TRONG CSDL]:
                        if (dao.delete(serviceID.trim())) {
                            request.setAttribute("MESSAGE", "Service "
                                    + serviceID.trim() + " deleted");
                        } else {
                            request.setAttribute("ERROR", "Service "
                                    + serviceID.trim() + " no longer exists");
                        }
                    } catch (SQLException sqle) {
                        // [4. BẮT LỖI RÀNG BUỘC KHÓA NGOẠI NẾU DỊCH VỤ ĐÃ CÓ LỊCH HẸN HOẶC ĐÁNH GIÁ]:
                        if (sqle.getErrorCode() == FK_VIOLATION) {
                            request.setAttribute("ERROR", "Service "
                                    + serviceID.trim() + " cannot be deleted"
                                    + " because it is used by an appointment"
                                    + " or a review");
                        } else {
                            throw sqle;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log("Error at DeleteServiceController: " + e.toString());
            request.setAttribute("ERROR", "Could not delete the service");
            url = VIEW_SERVICES;
        } finally {
            // [5. CHUYỂN TIẾP VỀ VIEWSERVICESCONTROLLER ĐỂ TẢI LẠI BẢNG DỊCH VỤ]:
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
