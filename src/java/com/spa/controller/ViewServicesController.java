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
 * - Controller ĐỌC DỮ LIỆU trung tâm của nhóm quản lý dịch vụ (Action: ViewServices - Dành cho Admin).
 * - Kiểm tra quyền Admin từ Session.
 * - Đọc từ khóa tìm kiếm `search` từ request (nếu không có thì mặc định là rỗng "").
 * - Gọi `ServiceDAO.search(search.trim())` để lấy danh sách dịch vụ.
 * - Đặt danh sách vào Request Attribute `LIST_SERVICE`, từ khóa vào `SEARCH`, và forward sang [serviceList.jsp].
 * - Là ĐÍCH ĐẾN SAU KHI GHI DỮ LIỆU của 3 Controller:
 *   + CreateServiceController (tạo mới xong -> quay về ViewServicesController).
 *   + UpdateServiceController (cập nhật xong -> quay về ViewServicesController).
 *   + DeleteServiceController (xóa xong -> quay về ViewServicesController).
 *   -> Nhờ vậy danh sách dịch vụ trên [serviceList.jsp] luôn được nạp mới nhất từ CSDL.
 *
 * [LUỒNG THỰC THI (EXECUTION FLOW)]
 * 1. Client click "Manage services" trên [dashboard.jsp], hoặc submit form tìm kiếm trên [serviceList.jsp],
 *    hoặc được redirect từ Create/Update/Delete Controller -> MainController (action=ViewServices) -> ViewServicesController.
 * 2. Lấy `LOGIN_USER` từ HttpSession:
 *    - Nếu null hoặc !loginUser.isAdmin() -> Set ERROR "You must log in as an administrator" -> Chuyển về login.jsp.
 * 3. Đọc tham số `search` (từ khóa tìm kiếm).
 * 4. Gọi `ServiceDAO.search(search)` -> nhận `List<Service>`.
 * 5. Set attributes:
 *    - `request.setAttribute("LIST_SERVICE", list)`
 *    - `request.setAttribute("SEARCH", search)`
 * 6. Forward sang [serviceList.jsp].
 *
 * [LUỒNG DỮ LIỆU (DATA FLOW)]
 * - Dữ liệu vào:
 *   + `session.getAttribute("LOGIN_USER")` -> Đối tượng User để kiểm tra quyền.
 *   + `request.getParameter("search")` -> Từ khóa tìm kiếm tên dịch vụ.
 * - Dữ liệu ra Request:
 *   + `LIST_SERVICE`: `List<Service>` -> Đổ vào bảng `<table ...>` trên [serviceList.jsp].
 *   + `SEARCH`: Chuỗi tìm kiếm -> Giữ lại trong ô `<input name="search">` trên [serviceList.jsp].
 *
 * [LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
 * 1. Sửa tên Request Attribute `"LIST_SERVICE"`:
 *    -> BẮT BUỘC phải sửa thẻ `<c:forEach items="${LIST_SERVICE}">` trong [serviceList.jsp].
 * 2. Sửa tên Request Attribute `"SEARCH"`:
 *    -> Phải sửa thẻ `<input name="search" value="${SEARCH}">` trong [serviceList.jsp].
 * 3. Sửa tên Controller đích SERVICE_LIST_PAGE = "serviceList.jsp":
 *    -> Đổi file giao diện danh sách dịch vụ của Admin.
 * ============================================================================
 */
public class ViewServicesController extends HttpServlet {

    // [CÁC TRANG ĐÍCH]
    private static final String SERVICE_LIST_PAGE = "serviceList.jsp";
    private static final String LOGIN_PAGE = "login.jsp";
    private static final String ERROR_PAGE = "error.jsp";

    protected void processRequest(HttpServletRequest request,
                                  HttpServletResponse response)
            throws ServletException, IOException {
        String url = ERROR_PAGE;
        try {
            // [1. KIỂM TRA PHÂN QUYỀN ADMIN TỪ SESSION]:
            HttpSession session = request.getSession(false);
            User loginUser = session == null
                    ? null : (User) session.getAttribute("LOGIN_USER");

            if (loginUser == null || !loginUser.isAdmin()) {
                // Không có phiên hoặc không phải Admin -> Đẩy về login.jsp
                request.setAttribute("ERROR",
                        "You must log in as an administrator");
                url = LOGIN_PAGE;
            } else {
                // [2. ĐỌC TỪ KHÓA TÌM KIẾM TỪ FORM / QUERY STRING]:
                String search = request.getParameter("search");
                if (search == null) {
                    search = ""; // Nếu không truyền search -> Lấy toàn bộ dịch vụ
                }

                // [3. TRUY VẤN CSDL QUA SERVICEDAO]:
                ServiceDAO dao = new ServiceDAO();
                List<Service> list = dao.search(search.trim());

                // [4. ĐẶT DỮ LIỆU VÀO REQUEST ATTRIBUTE ĐỂ TRUYỀN SANG JSP]:
                request.setAttribute("LIST_SERVICE", list);
                request.setAttribute("SEARCH", search);
                url = SERVICE_LIST_PAGE;
            }
        } catch (Exception e) {
            log("Error at ViewServicesController: " + e.toString());
            request.setAttribute("ERROR", "Could not load the service list");
            url = ERROR_PAGE;
        } finally {
            // [5. CHUYỂN TIẾP SANG SERVICELIST.JSP]:
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
