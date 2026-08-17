package com.spa.controller;

import com.spa.dao.UserDAO;
import com.spa.dto.User;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * ============================================================================
 * [VAI TRÒ & CHỨC NĂNG]
 * - Xử lý chức năng ĐĂNG KÝ TÀI KHOẢN MỚI (Action: Register).
 * - Nhận thông tin người dùng từ form [register.jsp] (userID, fullName, email, phoneNumber, password, confirm).
 * - Thực hiện kiểm tra tính hợp lệ dữ liệu chặt chẽ ở phía Server (Server-side validation):
 *   + Không để trống bất kỳ trường nào.
 *   + Mật khẩu và Xác nhận mật khẩu phải trùng khớp.
 *   + Độ dài mật khẩu >= 6 ký tự.
 *   + Số điện thoại đúng định dạng từ 9-15 chữ số.
 *   + Kiểm tra trùng mã tài khoản (`dao.isUserIDExisted`).
 *   + Kiểm tra trùng email (`dao.isEmailExisted`).
 * - GÁN CỨNG vai trò là "USR" (Customer) để đảm bảo an ninh (không cho phép người dùng tự chọn vai trò Admin).
 * - Khi thành công: Chuyển về [login.jsp] kèm thông báo thành công `MESSAGE`.
 * - Khi có lỗi: Giữ lại 4 trường thông tin đã gõ (OLD_USER_ID, OLD_FULL_NAME, OLD_EMAIL, OLD_PHONE),
 *   đặt thông báo lỗi `ERROR`, và quay lại [register.jsp].
 *
 * [LUỒNG THỰC THI (EXECUTION FLOW)]
 * 1. Form [register.jsp] POST -> MainController (action=Register) -> RegisterController.
 * 2. Đọc các tham số: `userID`, `fullName`, `email`, `phoneNumber`, `password`, `confirm`.
 * 3. Kiểm tra validation tuần tự:
 *    - isBlank -> Báo lỗi "Please fill in every field".
 *    - password != confirm -> Báo lỗi "The two passwords do not match".
 *    - password.length < 6 -> Báo lỗi "Password must be at least 6 characters".
 *    - phoneNumber regex -> Báo lỗi "Phone number must be 9 to 15 digits".
 *    - isUserIDExisted -> Báo lỗi "User ID ... is already taken".
 *    - isEmailExisted -> Báo lỗi "Email ... is already registered".
 * 4. Nếu không có lỗi (error == null):
 *    - Tạo đối tượng `User` với roleID = "USR".
 *    - Gọi `UserDAO.insert(user, password)`.
 *    - Nếu insert thành công: Set `MESSAGE` và chuyển sang `login.jsp`.
 * 5. Nếu có lỗi: Set `ERROR` và các thuộc tính `OLD_*` -> forward về `register.jsp`.
 *
 * [LUỒNG DỮ LIỆU (DATA FLOW)]
 * - Dữ liệu vào:
 *   + `request.getParameter("userID")`, `fullName`, `email`, `phoneNumber`, `password`, `confirm`.
 * - Dữ liệu xuống DB:
 *   + Bảng `tblUsers` (userID, fullName, email, phoneNumber, roleID='USR', password).
 * - Dữ liệu ra Request:
 *   + Thành công: `request.setAttribute("MESSAGE", "Registration successful, please log in")` -> [login.jsp].
 *   + Thất bại: `request.setAttribute("ERROR", error)`, `OLD_USER_ID`, `OLD_FULL_NAME`, `OLD_EMAIL`, `OLD_PHONE` -> [register.jsp].
 *
 * [LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
 * 1. Sửa DEFAULT_ROLE ("USR"):
 *    -> Phải đồng bộ với ràng buộc Check Constraint trong [SpaConsultationPortal.sql] và [User.java].
 * 2. Sửa các quy tắc validate (độ dài mật khẩu, regex điện thoại):
 *    -> Nên đồng bộ với thuộc tính HTML5 `minlength="6"`, `pattern="[0-9]{9,15}"` trên [register.jsp].
 * 3. Sửa tên Request Attribute `OLD_*`:
 *    -> Phải sửa các thẻ `<c:out value="${OLD_USER_ID}"/>`... trên [register.jsp].
 * ============================================================================
 */
public class RegisterController extends HttpServlet {

    // [CÁC TRANG ĐÍCH]
    private static final String REGISTER_PAGE = "register.jsp";
    private static final String LOGIN_PAGE = "login.jsp";

    // [VAI TRÒ MẶC ĐỊNH CHO TÀI KHOẢN TỰ ĐĂNG KÝ]
    // [TÁC ĐỘNG]: Khớp với roleID='USR' trong bảng tblUsers và User.isCustomer()
    private static final String DEFAULT_ROLE = "USR";

    protected void processRequest(HttpServletRequest request,
                                  HttpServletResponse response)
            throws ServletException, IOException {
        String url = REGISTER_PAGE;
        try {
            // [1. ĐỌC THAM SỐ GỬI LÊN TỪ FORM REGISTER.JSP]
            String userID = request.getParameter("userID");
            String fullName = request.getParameter("fullName");
            String email = request.getParameter("email");
            String phoneNumber = request.getParameter("phoneNumber");
            String password = request.getParameter("password");
            String confirm = request.getParameter("confirm");

            String error = null;

            // [2. KIỂM TRA HỢP LỆ DỮ LIỆU PHÍA SERVER (SERVER-SIDE VALIDATION)]
            if (isBlank(userID) || isBlank(fullName) || isBlank(email)
                    || isBlank(phoneNumber) || isBlank(password)
                    || isBlank(confirm)) {
                error = "Please fill in every field";
            } else if (!password.equals(confirm)) {
                error = "The two passwords do not match";
            } else if (password.length() < 6) {
                error = "Password must be at least 6 characters";
            } else if (!phoneNumber.trim().matches("[0-9]{9,15}")) {
                error = "Phone number must be 9 to 15 digits";
            } else {
                // [3. KIỂM TRA TRÙNG LẶP TRONG CSDL QUA USERDAO]
                UserDAO dao = new UserDAO();
                if (dao.isUserIDExisted(userID.trim())) {
                    error = "User ID " + userID.trim() + " is already taken";
                } else if (dao.isEmailExisted(email.trim())) {
                    error = "Email " + email.trim() + " is already registered";
                } else {
                    // [4. THỰC HIỆN TẠO TÀI KHOẢN MỚI]
                    User user = new User(userID.trim(), fullName.trim(),
                            email.trim(), phoneNumber.trim(), DEFAULT_ROLE);
                    if (dao.insert(user, password)) {
                        // Đăng ký thành công -> Chuyển sang login.jsp kèm thông báo
                        request.setAttribute("MESSAGE",
                                "Registration successful, please log in");
                        url = LOGIN_PAGE;
                    } else {
                        error = "Could not create the account, please try again";
                    }
                }
            }

            // [5. NẾU CÓ LỖI: LƯU THÔNG TIN ĐÃ NHẬP ĐỂ ĐỔ LẠI FORM (KHÔNG GIỮ MẬT KHẨU)]
            if (error != null) {
                request.setAttribute("ERROR", error);
                request.setAttribute("OLD_USER_ID", userID);
                request.setAttribute("OLD_FULL_NAME", fullName);
                request.setAttribute("OLD_EMAIL", email);
                request.setAttribute("OLD_PHONE", phoneNumber);
            }
        } catch (Exception e) {
            log("Error at RegisterController: " + e.toString());
            request.setAttribute("ERROR", "System error, please try again later");
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
