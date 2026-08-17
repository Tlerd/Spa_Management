package com.spa.controller;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * ============================================================================
 * [VAI TRÒ & CHỨC NĂNG]
 * - Xử lý chức năng ĐĂNG XUẤT (Action: Logout).
 * - Hủy bỏ phiên làm việc hiện tại (HttpSession.invalidate()), xóa sạch thông tin LOGIN_USER,
 *   và chuyển hướng người dùng về lại trang [login.jsp] kèm thông báo "You have been logged out".
 *
 * [LUỒNG THỰC THI (EXECUTION FLOW)]
 * 1. Người dùng bấm nút Logout trên [dashboard.jsp] -> Form gửi tới MainController (action=Logout).
 * 2. MainController forward sang LogoutController.
 * 3. LogoutController lấy Session hiện tại qua `request.getSession(false)`.
 *    -> Dùng `false` để không tự động tạo mới session nếu phiên đã hết hạn.
 * 4. Nếu session != null -> Gọi `session.invalidate()` để xóa session khỏi bộ nhớ Web Server.
 * 5. Đặt thông báo `MESSAGE` = "You have been logged out" vào Request.
 * 6. Forward về [login.jsp].
 *
 * [LUỒNG DỮ LIỆU (DATA FLOW)]
 * - Dữ liệu bị hủy: Toàn bộ attribute trong HttpSession (bao gồm `LOGIN_USER`).
 * - Dữ liệu ra Request:
 *   + `request.setAttribute("MESSAGE", "You have been logged out")` -> Hiển thị thông báo màu xanh trên [login.jsp].
 *
 * [LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
 * 1. Sửa tên Request Attribute `"MESSAGE"`:
 *    -> Phải sửa thẻ `<c:out value="${MESSAGE}"/>` trên [login.jsp].
 * 2. Sửa LOGIN_PAGE = "login.jsp":
 *    -> Đổi trang đích sau khi đăng xuất thành công.
 * ============================================================================
 */
public class LogoutController extends HttpServlet {

    // [TRANG ĐÍCH SAU KHI ĐĂNG XUẤT]
    private static final String LOGIN_PAGE = "login.jsp";

    protected void processRequest(HttpServletRequest request,
                                  HttpServletResponse response)
            throws ServletException, IOException {
        String url = LOGIN_PAGE;
        try {
            // Lấy session hiện tại (nếu có), KHÔNG tự tạo session mới
            HttpSession session = request.getSession(false);
            if (session != null) {
                // Xóa toàn bộ dữ liệu trong phiên làm việc (LOGIN_USER bị hủy hoàn toàn)
                session.invalidate();
            }
            // Đặt thông báo thành công hiển thị ở login.jsp
            request.setAttribute("MESSAGE", "You have been logged out");
        } catch (Exception e) {
            log("Error at LogoutController: " + e.toString());
            request.setAttribute("ERROR", "System error, please try again later");
        } finally {
            // Chuyển tiếp về trang đăng nhập
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
