package com.spa.controller;

import com.spa.dao.UserDAO;
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
 * - Xử lý chức năng ĐĂNG NHẬP (Action: Login) của tất cả người dùng (Admin, Staff, Customer).
 * - Nhận email và password từ form [login.jsp], gọi UserDAO.checkLogin để xác thực.
 * - Khi thành công: Lưu toàn bộ đối tượng `User` vào HttpSession với key `LOGIN_USER`
 *   và chuyển hướng tới [dashboard.jsp].
 * - Khi thất bại: Giữ lại email cũ (request attribute `OLD_EMAIL`), đặt thông báo lỗi `ERROR`,
 *   và forward về lại [login.jsp].
 *
 * [LUỒNG THỰC THI (EXECUTION FLOW)]
 * 1. Client submit form [login.jsp] -> MainController (action=Login) -> LoginController.
 * 2. LoginController đọc tham số: `email`, `password`.
 * 3. Kiểm tra rỗng/null:
 *    - Nếu thiếu: Đặt request attribute `ERROR` = "Please enter both email and password" -> về login.jsp.
 * 4. Gọi `UserDAO.checkLogin(email.trim(), password)`:
 *    - Nếu trả về `null` (sai email/mật khẩu): Đặt `ERROR` và `OLD_EMAIL` -> về login.jsp.
 *    - Nếu trả về đối tượng `User`:
 *      + `HttpSession session = request.getSession();`
 *      + `session.setAttribute("LOGIN_USER", user);`
 *      + Chuyển tiếp tới [dashboard.jsp].
 *
 * [LUỒNG DỮ LIỆU (DATA FLOW)]
 * - Dữ liệu vào:
 *   + `request.getParameter("email")`
 *   + `request.getParameter("password")`
 * - Dữ liệu ra Session:
 *   + `session.setAttribute("LOGIN_USER", user)` -> Đối tượng User mang userID, fullName, email, roleID.
 * - Dữ liệu ra Request (khi lỗi):
 *   + `request.setAttribute("ERROR", ...)` -> Hiển thị trên [login.jsp]
 *   + `request.setAttribute("OLD_EMAIL", email)` -> Đổ lại vào ô <input name="email"> trên [login.jsp]
 *
 * [LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
 * 1. Sửa tên Session Key `"LOGIN_USER"`:
 *    -> BẮT BUỘC phải sửa ở TẤT CẢ các Controller khác (khi lấy User ra kiểm tra quyền)
 *       và ở tất cả các file JSP (khi kiểm tra sessionScope.LOGIN_USER).
 * 2. Sửa tên Request Attribute `"ERROR"` hoặc `"OLD_EMAIL"`:
 *    -> Phải sửa thẻ `<c:out value="${ERROR}"/>` và `<c:out value="${OLD_EMAIL}"/>` trong [login.jsp].
 * 3. Sửa tên input form:
 *    -> Nếu trên [login.jsp] sửa `name="email"` hoặc `name="password"` thì phải sửa
 *       `request.getParameter("...")` tương ứng trong lớp này.
 * ============================================================================
 */
public class LoginController extends HttpServlet {

    // [CÁC TRANG ĐÍCH]
    // [TÁC ĐỘNG]: Sửa LOGIN_PAGE -> Đổi file JSP đăng nhập; Sửa DASHBOARD_PAGE -> Đổi trang chủ sau đăng nhập
    private static final String LOGIN_PAGE = "login.jsp";
    private static final String DASHBOARD_PAGE = "dashboard.jsp";

    protected void processRequest(HttpServletRequest request,
                                  HttpServletResponse response)
            throws ServletException, IOException {
        String url = LOGIN_PAGE;
        try {
            // [1. ĐỌC DỮ LIỆU TỪ REQUEST]: Gửi từ form login.jsp
            String email = request.getParameter("email");
            String password = request.getParameter("password");

            // [2. KIỂM TRA TÍNH ĐẦY ĐỦ CỦA DỮ LIỆU]:
            if (email == null || email.trim().isEmpty()
                    || password == null || password.isEmpty()) {
                request.setAttribute("ERROR",
                        "Please enter both email and password");
            } else {
                // [3. GỌI DAO XÁC THỰC CSDL]:
                UserDAO dao = new UserDAO();
                User user = dao.checkLogin(email.trim(), password);

                if (user == null) {
                    // Đăng nhập thất bại: Giữ lại email đã gõ, KHÔNG BAO GIỜ giữ lại mật khẩu vì lý do bảo mật
                    request.setAttribute("ERROR", "Incorrect email or password");
                    request.setAttribute("OLD_EMAIL", email);
                } else {
                    // [4. ĐĂNG NHẬP THÀNH CÔNG]:
                    // Lưu nguyên đối tượng User vào Session với key "LOGIN_USER"
                    // Đối tượng này sẽ được dùng xuyên suốt phiên làm việc bởi mọi Controller và JSP
                    HttpSession session = request.getSession();
                    session.setAttribute("LOGIN_USER", user);
                    url = DASHBOARD_PAGE;
                }
            }
        } catch (Exception e) {
            log("Error at LoginController: " + e.toString());
            request.setAttribute("ERROR", "System error, please try again later");
        } finally {
            // [5. CHUYỂN TIẾP SANG TRANG ĐÍCH]: (dashboard.jsp nếu đúng, login.jsp nếu sai/lỗi)
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
