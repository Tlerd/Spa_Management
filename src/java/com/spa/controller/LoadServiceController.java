package com.spa.controller;

import com.spa.dao.ServiceDAO;
import com.spa.dto.Service;
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
 * - Xử lý thao tác NẠP DỮ LIỆU DỊCH VỤ LÊN FORM CHỈNH SỬA (Action: LoadService - Dành cho Admin).
 * - Được kích hoạt khi Admin click vào đường link "Edit" trên [serviceList.jsp].
 * - Đọc `serviceID` từ tham số URL, gọi `ServiceDAO.getByID(serviceID)`.
 * - Khi tìm thấy: Đặt đối tượng `Service` vào Request Attribute `SERVICE` và forward sang [updateService.jsp].
 * - Khi không tìm thấy (ví dụ dịch vụ vừa bị Admin khác xóa): Đặt thông báo lỗi và quay về ViewServicesController
 *   để tải lại danh sách dịch vụ mới nhất.
 *
 * [LUỒNG THỰC THI (EXECUTION FLOW)]
 * 1. Admin bấm link "Edit" trên [serviceList.jsp] -> MainController (action=LoadService&serviceID=...) -> LoadServiceController.
 * 2. Kiểm tra quyền Admin trong Session (`loginUser.isAdmin()`).
 * 3. Đọc tham số `serviceID = request.getParameter("serviceID")`.
 * 4. Gọi `ServiceDAO.getByID(serviceID.trim())`:
 *    - Nếu `service == null`: Set `ERROR` = "Service ... no longer exists" -> Quay về `ViewServicesController`.
 *    - Nếu `service != null`: Set `request.setAttribute("SERVICE", service)` -> Chuyển tiếp tới [updateService.jsp].
 *
 * [LUỒNG DỮ LIỆU (DATA FLOW)]
 * - Dữ liệu vào:
 *   + `request.getParameter("serviceID")` -> Chuỗi mã dịch vụ cần sửa.
 * - Dữ liệu ra Request:
 *   + `request.setAttribute("SERVICE", service)` -> Chứa toàn bộ thông tin dịch vụ cũ để đổ vào các ô input trên [updateService.jsp].
 *
 * [LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
 * 1. Sửa tên Request Attribute `"SERVICE"`:
 *    -> BẮT BUỘC phải sửa các biểu thức EL trên [updateService.jsp]:
 *       `${SERVICE.serviceID}`, `${SERVICE.serviceName}`, `${SERVICE.description}`, `${SERVICE.price}`.
 * 2. Sửa UPDATE_PAGE = "updateService.jsp":
 *    -> Đổi file giao diện form cập nhật dịch vụ.
 * ============================================================================
 */
public class LoadServiceController extends HttpServlet {

    // [CÁC TRANG ĐÍCH]
    private static final String UPDATE_PAGE = "updateService.jsp";
    private static final String VIEW_SERVICES = "ViewServicesController";
    private static final String LOGIN_PAGE = "login.jsp";

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
                // [2. ĐỌC MÃ DỊCH VỤ CẦN SỬA TỪ REQUEST]:
                String serviceID = request.getParameter("serviceID");
                if (serviceID == null || serviceID.trim().isEmpty()) {
                    request.setAttribute("ERROR", "No service was selected");
                } else {
                    // [3. TRUY VẤN CHI TIẾT DỊCH VỤ TỪ CSDL]:
                    ServiceDAO dao = new ServiceDAO();
                    Service service = dao.getByID(serviceID.trim());
                    if (service == null) {
                        // Dịch vụ không tồn tại (đã bị xóa trước đó) -> Về danh sách kèm thông báo lỗi
                        request.setAttribute("ERROR",
                                "Service " + serviceID.trim()
                                + " no longer exists");
                    } else {
                        // [4. ĐẶT ĐỐI TƯỢNG SERVICE VÀO REQUEST ĐỂ HIỂN THỊ TRÊN FORM SỬA]:
                        request.setAttribute("SERVICE", service);
                        url = UPDATE_PAGE;
                    }
                }
            }
        } catch (Exception e) {
            log("Error at LoadServiceController: " + e.toString());
            request.setAttribute("ERROR", "Could not load the service");
            url = VIEW_SERVICES;
        } finally {
            // [5. CHUYỂN TIẾP TỚI UPDATESERVICE.JSP HOẶC VIEWSERVICESCONTROLLER]:
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
