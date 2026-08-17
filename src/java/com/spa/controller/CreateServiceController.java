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
 * - Xử lý thao tác TẠO MỚI DỊCH VỤ SPA (Action: CreateService - Dành cho Admin).
 * - Nhận thông tin từ form [createService.jsp]: `serviceID`, `serviceName`, `description`, `price`.
 * - Thực hiện kiểm tra tính hợp lệ dữ liệu chặt chẽ ở Server:
 *   + Không để trống bất kỳ trường nào.
 *   + Độ dài: serviceID <= 50 ký tự, serviceName <= 200 ký tự, description <= 1000 ký tự.
 *   + Giá `price`: Phải là số hợp lệ và >= 0 (dùng BigDecimal để tránh sai số).
 *   + Kiểm tra trùng mã khóa chính qua `ServiceDAO.isServiceIDExisted()`.
 * - Khi thành công: Đặt thông báo thành công `MESSAGE` và chuyển tiếp về `ViewServicesController`
 *   để nạp lại danh sách mới nhất hiển thị trên [serviceList.jsp].
 * - Khi có lỗi: Giữ lại dữ liệu đã gõ (OLD_SERVICE_ID, OLD_SERVICE_NAME, OLD_DESCRIPTION, OLD_PRICE),
 *   đặt thông báo lỗi `ERROR`, và quay lại [createService.jsp].
 *
 * [LUỒNG THỰC THI (EXECUTION FLOW)]
 * 1. Admin submit form [createService.jsp] -> MainController (action=CreateService) -> CreateServiceController.
 * 2. Kiểm tra quyền Admin trong Session (`loginUser.isAdmin()`).
 * 3. Đọc các tham số: `serviceID`, `serviceName`, `description`, `price`.
 * 4. Validate dữ liệu:
 *    - isBlank -> "Please fill in every field"
 *    - length checks -> Báo lỗi độ dài
 *    - parse price -> BigDecimal >= 0
 *    - isServiceIDExisted -> "Service ID ... already exists"
 * 5. Nếu hợp lệ (error == null):
 *    - Tạo đối tượng `Service(serviceID, serviceName, description, priceValue)`.
 *    - Gọi `ServiceDAO.insert(service)`.
 *    - Nếu insert thành công -> `request.setAttribute("MESSAGE", ...)` -> `url = ViewServicesController`.
 * 6. Nếu có lỗi (error != null) -> Set `ERROR` và các thuộc tính `OLD_*` -> `url = createService.jsp`.
 *
 * [LUỒNG DỮ LIỆU (DATA FLOW)]
 * - Dữ liệu vào:
 *   + `request.getParameter("serviceID")`, `serviceName`, `description`, `price`.
 * - Dữ liệu xuống DB:
 *   + Bảng `tblServices` (serviceID, serviceName, description, price).
 * - Dữ liệu ra Request:
 *   + Thành công: `request.setAttribute("MESSAGE", "Service ... created")` -> [ViewServicesController] -> [serviceList.jsp].
 *   + Thất bại: `request.setAttribute("ERROR", error)` & `OLD_*` -> [createService.jsp].
 *
 * [LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
 * 1. Sửa URL đích sau khi tạo thành công (VIEW_SERVICES = "ViewServicesController"):
 *    -> QUY TẮC BẮT BUỘC: Không bao giờ forward trực tiếp tới serviceList.jsp vì trang đó sẽ nhận danh sách rỗng (null).
 *       Phải luôn quay về ViewServicesController để tải lại dữ liệu CSDL.
 * 2. Sửa các thuộc tính OLD_* :
 *    -> Phải sửa các thẻ `<c:out value="${OLD_SERVICE_ID}"/>`... trong [createService.jsp].
 * ============================================================================
 */
public class CreateServiceController extends HttpServlet {

    // [CÁC TRANG ĐÍCH]
    private static final String CREATE_PAGE = "createService.jsp";
    private static final String VIEW_SERVICES = "ViewServicesController";
    private static final String LOGIN_PAGE = "login.jsp";

    protected void processRequest(HttpServletRequest request,
                                  HttpServletResponse response)
            throws ServletException, IOException {
        String url = CREATE_PAGE;
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
                // [2. ĐỌC THAM SỐ GỬI LÊN TỪ FORM CREATESERVICE.JSP]:
                String serviceID = request.getParameter("serviceID");
                String serviceName = request.getParameter("serviceName");
                String description = request.getParameter("description");
                String price = request.getParameter("price");

                String error = null;
                BigDecimal priceValue = null;

                // [3. KIỂM TRA HỢP LỆ DỮ LIỆU]:
                if (isBlank(serviceID) || isBlank(serviceName)
                        || isBlank(description) || isBlank(price)) {
                    error = "Please fill in every field";
                } else if (serviceID.trim().length() > 50) {
                    error = "Service ID must not exceed 50 characters";
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

                // [4. KIỂM TRA TRÙNG MÃ KHÓA CHÍNH SERVICEID]:
                ServiceDAO dao = new ServiceDAO();
                if (error == null && dao.isServiceIDExisted(serviceID.trim())) {
                    error = "Service ID " + serviceID.trim() + " already exists";
                }

                // [5. THỰC HIỆN THÊM MỚI VÀO CSDL]:
                if (error == null) {
                    Service service = new Service(serviceID.trim(),
                            serviceName.trim(), description.trim(), priceValue);
                    if (dao.insert(service)) {
                        // Thêm thành công -> Chuyển về Servlet đọc danh sách để nạp lại dữ liệu
                        request.setAttribute("MESSAGE",
                                "Service " + service.getServiceID() + " created");
                        url = VIEW_SERVICES;
                    } else {
                        error = "Could not create the service, please try again";
                    }
                }

                // [6. NẾU CÓ LỖI: ĐỔ LẠI DỮ LIỆU ĐÃ GÕ LÊN FORM]:
                if (error != null) {
                    request.setAttribute("ERROR", error);
                    request.setAttribute("OLD_SERVICE_ID", serviceID);
                    request.setAttribute("OLD_SERVICE_NAME", serviceName);
                    request.setAttribute("OLD_DESCRIPTION", description);
                    request.setAttribute("OLD_PRICE", price);
                    url = CREATE_PAGE;
                }
            }
        } catch (Exception e) {
            log("Error at CreateServiceController: " + e.toString());
            request.setAttribute("ERROR", "System error, please try again later");
            url = CREATE_PAGE;
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
