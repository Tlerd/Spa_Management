<%--
============================================================================
[VAI TRÒ & CHỨC NĂNG]
- Giao diện ĐĂNG KÝ TÀI KHOẢN (register.jsp) - Dành cho khách truy cập muốn tạo tài khoản mới.
- Form thu thập: `userID`, `fullName`, `email`, `phoneNumber`, `password`, `confirm`.
- Lưu ý an ninh: Tuyệt đối KHÔNG có trường chọn vai trò (roleID) trên form này.
  Mọi tài khoản đăng ký đều được RegisterController gán mặc định roleID = "USR".

[LUỒNG THỰC THI (EXECUTION FLOW)]
1. Submit form -> POST request gửi về `action="MainController"` kèm `<input type="submit" value="Register" name="action"/>`.
2. MainController nhận action="Register" -> Chuyển tiếp tới [RegisterController].
3. RegisterController xử lý:
   - Nếu hợp lệ & tạo tài khoản thành công -> Chuyển sang [login.jsp] kèm thông báo `MESSAGE`.
   - Nếu có lỗi (trùng ID, trùng email, mật khẩu không khớp, để trống) -> Quay về lại [register.jsp],
     đặt thông báo lỗi `ERROR` và giữ lại 4 trường thông tin đã gõ (OLD_USER_ID, OLD_FULL_NAME, OLD_EMAIL, OLD_PHONE).

[LUỒNG DỮ LIỆU (DATA FLOW)]
- Dữ liệu gửi đi (Request parameters):
  + `userID`, `fullName`, `email`, `phoneNumber`, `password`, `confirm`, `action=Register`.
- Dữ liệu nhận từ Controller (Request attributes):
  + `${OLD_USER_ID}`, `${OLD_FULL_NAME}`, `${OLD_EMAIL}`, `${OLD_PHONE}`: Đổ lại form khi có lỗi.
  + `${ERROR}`: Thông báo lỗi chi tiết từ Server.

[LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
1. Sửa `value="Register" name="action"`:
   -> Phải khớp với hằng số REGISTER = "Register" trong [MainController.java].
2. Sửa các tên trường `name="..."` (userID, fullName, email, phoneNumber, password, confirm):
   -> Phải đồng bộ với các lệnh `request.getParameter("...")` trong [RegisterController.java].
3. Sửa các thuộc tính HTML5 (`pattern="[0-9]{9,15}"`, `minlength="6"`):
   -> Phải đồng bộ với logic kiểm tra Regex và độ dài trong [RegisterController.java].
============================================================================
--%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Register</title>
        <link rel="stylesheet" href="css/styles.css"/>
    </head>
    <body>
        <div class="container">
            <h2>Create a new account</h2>

            <form action="MainController" method="POST">
                <p>
                    <label for="userID">User ID:</label>
                    <input type="text" id="userID" name="userID"
                           value="<c:out value='${OLD_USER_ID}'/>"
                           maxlength="50" required=""/>
                </p>
                <p>
                    <label for="fullName">Full name:</label>
                    <input type="text" id="fullName" name="fullName"
                           value="<c:out value='${OLD_FULL_NAME}'/>"
                           maxlength="500" required=""/>
                </p>
                <p>
                    <label for="email">Email:</label>
                    <input type="email" id="email" name="email"
                           value="<c:out value='${OLD_EMAIL}'/>"
                           maxlength="100" required=""/>
                </p>
                <p>
                    <label for="phoneNumber">Phone number:</label>
                    <input type="text" id="phoneNumber" name="phoneNumber"
                           value="<c:out value='${OLD_PHONE}'/>"
                           pattern="[0-9]{9,15}" required=""/>
                </p>
                <p>
                    <label for="password">Password:</label>
                    <input type="password" id="password" name="password"
                           minlength="6" required=""/>
                </p>
                <p>
                    <label for="confirm">Confirm password:</label>
                    <input type="password" id="confirm" name="confirm"
                           minlength="6" required=""/>
                </p>
                <p>
                    <input type="submit" value="Register" name="action"/>
                </p>
            </form>

            <%-- Hiển thị thông báo lỗi từ Server nếu việc đăng ký thất bại --%>
            <c:if test="${not empty ERROR}">
                <p class="error"><c:out value="${ERROR}"/></p>
            </c:if>

            <p><a href="login.jsp">Back to login</a></p>
        </div>
    </body>
</html>
