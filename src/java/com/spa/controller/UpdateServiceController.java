package com.spa.controller;

import com.spa.dao.ServiceDAO;
import com.spa.dto.Service;
import com.spa.dto.User;
import java.io.IOException;
import java.math.BigDecimal;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * ============================================================================
 * [VAI TRÒ & CHỨC NĂNG]
 * - Xử lý thao tác CẬP NHẬT THÔNG TIN DỊCH VỤ SPA (Action: UpdateService - Dành cho Admin).
 * - Nhận thông tin từ form [updateService.jsp]: `serviceID` (readonly), `serviceName`, `description`, `price`.
 * - Thực hiện kiểm tra tính hợp lệ dữ liệu chặt chẽ ở Server:
 *   + Không để trống trường nào.
 *   + serviceName <= 200 ký tự, description <= 1000 ký tự.
 *   + Giá `price` phải là số hợp lệ và >= 0.
 * - Gọi `ServiceDAO.update(service)` để ghi đè dữ liệu vào CSDL.
 * - Khi thành công: Đặt thông báo `MESSAGE` và chuyển tiếp về `ViewServicesController` để tải lại danh sách.
 * - Khi có lỗi: Tạo lại đối tượng `Service` mang dữ liệu vừa gõ gán vào Request Attribute `SERVICE`,
 *   đặt thông báo lỗi `ERROR`, và quay lại [updateService.jsp] (giúp người dùng không bị mất thông tin vừa sửa).
 *
 * [LUỒNG THỰC THI (EXECUTION FLOW)]
 * 1. Admin submit form [updateService.jsp] -> MainController (action=UpdateService) -> UpdateServiceController.
 * 2. Kiểm tra quyền Admin trong Session (`loginUser.isAdmin()`).
 * 3. Đọc các tham số: `serviceID`, `serviceName`, `description`, `price`.
 * 4. Validate dữ liệu:
 *    - serviceID thiếu -> "Service ID is missing"
 *    - isBlank -> "Please fill in every field"
 *    - length checks -> Báo lỗi độ dài
 *    - parse price -> BigDecimal >= 0
 * 5. Nếu hợp lệ (error == null):
 *    - Gọi `ServiceDAO.update(service)`:
 *      + Trả về `true`: Ghi nhận thành công, set `MESSAGE` -> Chuyển về `ViewServicesController`.
 *      + Trả về `false`: Dịch vụ đã bị xóa bởi người khác -> Set `ERROR` -> Chuyển về `ViewServicesController`.
 * 6. Nếu có lỗi validation:
 *    - Tạo đối tượng `typed` chứa thông tin vừa nhập -> `request.setAttribute("SERVICE", typed)`.
 *    - `request.setAttribute("ERROR", error)` -> Chuyển về `updateService.jsp`.
 *
 * [LUỒNG DỮ LIỆU (DATA FLOW)]
 * - Dữ liệu vào:
 *   + `request.getParameter("serviceID")` (từ ô readonly trên form), `serviceName`, `description`, `price`.
 * - Dữ liệu xuống DB:
 *   + UPDATE `tblServices` SET serviceName=?, description=?, price=? WHERE serviceID=?
 * - Dữ liệu ra Request:
 *   + Thành công: `request.setAttribute("MESSAGE", ...)` -> [ViewServicesController] -> [serviceList.jsp].
 *   + Thất bại: `request.setAttribute("SERVICE", typed)` & `request.setAttribute("ERROR", error)` -> [updateService.jsp].
 *
 * [LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
 * 1. Thuộc tính `readonly` trên form [updateService.jsp]:
 *    -> BẮT BUỘC dùng `readonly` chứ KHÔNG DÙNG `disabled`. Vì thẻ `<input disabled>` sẽ không được
 *       trình duyệt gửi lên request, dẫn tới `serviceID` bị null và không tìm thấy bản ghi để sửa.
 * 2. Sửa tên Request Attribute `"SERVICE"`:
 *    -> Phải sửa các thẻ `<c:out value="${SERVICE.xxx}"/>` trong [updateService.jsp].
 * ============================================================================
 */
public class UpdateServiceController extends HttpServlet {

    // [CÁC TRANG ĐÍCH]
    private static final String UPDATE_PAGE = "updateService.jsp";
    private static final String VIEW_SERVICES = "ViewServicesController";
    private static final String LOGIN_PAGE = "login.jsp";

    protected void processRequest(HttpServletRequest request,
                                  HttpServletResponse response)
            throws ServletException, IOException {
        String url = UPDATE_PAGE;
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
                // [2. ĐỌC CÁC THAM SỐ TỪ FORM UPDATESERVICE.JSP]:
                String serviceID = request.getParameter("serviceID");
                String serviceName = request.getParameter("serviceName");
                String description = request.getParameter("description");
                String price = request.getParameter("price");

                String error = null;
                BigDecimal priceValue = null;

                // [3. KIỂM TRA HỢP LỆ DỮ LIỆU]:
                if (isBlank(serviceID)) {
                    error = "Service ID is missing";
                } else if (isBlank(serviceName) || isBlank(description)
                        || isBlank(price)) {
                    error = "Please fill in every field";
                } else if (serviceName.trim().length() > 200) {
                    error = "Service name must not exceed 200 characters";
                } else if (description.trim().length() > 1000) {
                    error = "Description must not exceed 1000 characters";
                } else {
                    try {
                        priceValue = new BigDecimal(price.trim());
                        if (priceValue.compareTo(BigDecimal.ZERO) < 0) {
                            error = "Price must not be negative";
                        }
                    } catch (NumberFormatException nfe) {
                        error = "Price must be a number";
                    }
                }

                // [4. THỰC HIỆN CẬP NHẬT VÀO CSDL QUA SERVICEDAO]:
                if (error == null) {
                    ServiceDAO dao = new ServiceDAO();
                    Service service = new Service(serviceID.trim(),
                            serviceName.trim(), description.trim(), priceValue);
                    if (dao.update(service)) {
                        // Cập nhật thành công -> Về lại ViewServicesController để nạp danh sách mới
                        request.setAttribute("MESSAGE",
                                "Service " + service.getServiceID() + " updated");
                    } else {
                        // Không tìm thấy bản ghi (đã bị xóa trong lúc form đang mở)
                        request.setAttribute("ERROR", "Service "
                                + serviceID.trim() + " no longer exists");
                    }
                    url = VIEW_SERVICES;
                } else {
                    // [5. NẾU CÓ LỖI VALIDATE: TẠO LẠI ĐỐI TƯỢNG SERVICE ĐỂ GIỮ LẠI DỮ LIỆU ĐÃ GÕ TRÊN FORM]:
                    Service typed = new Service();
                    typed.setServiceID(serviceID == null ? "" : serviceID);
                    typed.setServiceName(serviceName == null ? "" : serviceName);
                    typed.setDescription(description == null ? "" : description);
                    typed.setPrice(priceValue == null
                            ? BigDecimal.ZERO : priceValue);
                    request.setAttribute("SERVICE", typed);
                    request.setAttribute("ERROR", error);
                    url = UPDATE_PAGE;
                }
            }
        } catch (Exception e) {
            log("Error at UpdateServiceController: " + e.toString());
            request.setAttribute("ERROR", "System error, please try again later");
            url = VIEW_SERVICES;
        } finally {
            request.getRequestDispatcher(url).forward(request, response);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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
