package com.spa.controller;

import com.spa.dao.ServiceDAO;
import com.spa.dto.Service;
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
 * - Xử lý thao tác NẠP DANH SÁCH DỊCH VỤ LÊN FORM ĐẶT LỊCH (Action: LoadBooking - Dành cho Customer).
 * - Được kích hoạt khi Khách hàng bấm "Book an appointment" trên [dashboard.jsp] hoặc khi
 *   form đặt lịch gặp lỗi cần nạp lại dữ liệu.
 * - Kiểm tra quyền Customer từ Session (`loginUser.isCustomer()`).
 * - Gọi `ServiceDAO.search("")` để lấy toàn bộ danh sách các dịch vụ hiện có trong CSDL.
 * - Gán danh sách vào Request Attribute `LIST_SERVICE` và forward sang [bookAppointment.jsp].
 *
 * [LUỒNG THỰC THI (EXECUTION FLOW)]
 * 1. Khách hàng click link trên [dashboard.jsp] -> MainController (action=LoadBooking) -> LoadBookingController.
 * 2. Kiểm tra quyền Customer trong Session.
 * 3. Gọi `ServiceDAO.search("")` -> nhận `List<Service>`.
 * 4. `request.setAttribute("LIST_SERVICE", list)`.
 * 5. Forward sang [bookAppointment.jsp] để render thẻ `<select name="serviceID">`.
 *
 * [LUỒNG DỮ LIỆU (DATA FLOW)]
 * - Dữ liệu vào:
 *   + `session.getAttribute("LOGIN_USER")` -> Kiểm tra quyền.
 * - Dữ liệu ra Request:
 *   + `request.setAttribute("LIST_SERVICE", list)` -> Đổ vào các thẻ `<option value="${service.serviceID}">` trên [bookAppointment.jsp].
 *
 * [LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
 * 1. Sửa tên Request Attribute `"LIST_SERVICE"`:
 *    -> BẮT BUỘC phải sửa thẻ `<c:forEach items="${LIST_SERVICE}">` trong [bookAppointment.jsp].
 * 2. Phân biệt với LoadReviewController:
 *    -> LoadBookingController lấy TẤT CẢ dịch vụ trong CSDL để khách chọn đặt lịch mới.
 *    -> Trong khi LoadReviewController chỉ lấy những dịch vụ mà khách ĐÃ HOÀN THÀNH và CHƯA đánh giá.
 * ============================================================================
 */
public class LoadBookingController extends HttpServlet {

    // [CÁC TRANG ĐÍCH]
    private static final String BOOKING_PAGE = "bookAppointment.jsp";
    private static final String LOGIN_PAGE = "login.jsp";
    private static final String ERROR_PAGE = "error.jsp";

    protected void processRequest(HttpServletRequest request,
                                  HttpServletResponse response)
            throws ServletException, IOException {
        String url = ERROR_PAGE;
        try {
            // [1. KIỂM TRA PHÂN QUYỀN CUSTOMER TỪ SESSION]:
            HttpSession session = request.getSession(false);
            User loginUser = session == null
                    ? null : (User) session.getAttribute("LOGIN_USER");

            if (loginUser == null || !loginUser.isCustomer()) {
                request.setAttribute("ERROR", "You must log in as a customer");
                url = LOGIN_PAGE;
            } else {
                // [2. NẠP TOÀN BỘ DANH SÁCH DỊCH VỤ TỪ CSDL]:
                ServiceDAO dao = new ServiceDAO();
                List<Service> list = dao.search("");

                // [3. ĐẶT DANH SÁCH VÀO REQUEST ATTRIBUTE CHO DROPDOWN TRÊN JSP]:
                request.setAttribute("LIST_SERVICE", list);
                url = BOOKING_PAGE;
            }
        } catch (Exception e) {
            log("Error at LoadBookingController: " + e.toString());
            request.setAttribute("ERROR", "Could not load the service list");
            url = ERROR_PAGE;
        } finally {
            // [4. CHUYỂN TIẾP TỚI BOOKAPPOINTMENT.JSP]:
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
