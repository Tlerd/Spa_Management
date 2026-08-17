<%--
============================================================================
[VAI TRÒ & CHỨC NĂNG]
- Giao diện ĐĂNG NHẬP (login.jsp) - Cho phép tất cả người dùng (Khách, Admin, Staff, Customer) truy cập.
- Chứa form nhập `email` và `password`.

[LUỒNG THỰC THI (EXECUTION FLOW)]
1. Khi submit form:
   - Gửi POST request về `action="MainController"` kèm `<input type="submit" value="Login" name="action"/>`.
   - Tham số gửi đi: `action=Login`, `email=...`, `password=...`.
2. MainController nhận action="Login" -> Chuyển tiếp tới [LoginController].
3. LoginController xử lý:
   - Thành công -> Chuyển hướng tới [dashboard.jsp].
   - Thất bại -> Quay về lại [login.jsp] kèm thông báo lỗi `ERROR` và giữ lại `OLD_EMAIL`.

[LUỒNG DỮ LIỆU (DATA FLOW)]
- Dữ liệu gửi đi (Request parameters):
  + `email`: Chuỗi email tài khoản.
  + `password`: Chuỗi mật khẩu.
  + `action`: "Login".
- Dữ liệu nhận từ Controller (Request attributes):
  + `${OLD_EMAIL}`: Đổ lại vào ô nhập email nếu đăng nhập thất bại.
  + `${ERROR}`: Thông báo lỗi màu đỏ (nếu sai mật khẩu hoặc để trống).
  + `${MESSAGE}`: Thông báo thành công màu xanh (khi vừa đăng ký xong từ register.jsp hoặc vừa đăng xuất từ LogoutController).

[LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
1. Sửa `action="MainController"`:
   -> Phải khớp với <url-pattern>/MainController</url-pattern> trong [web.xml] và [MainController.java].
2. Sửa `name="action"` và `value="Login"`:
   -> BẮT BUỘC phải khớp với hằng số LOGIN = "Login" trong [MainController.java].
3. Sửa `name="email"` hoặc `name="password"`:
   -> Phải sửa `request.getParameter("email")` và `request.getParameter("password")` trong [LoginController.java].
4. Sửa `${OLD_EMAIL}`, `${ERROR}`, `${MESSAGE}`:
   -> Phải đồng bộ với `request.setAttribute(...)` trong [LoginController.java], [LogoutController.java], [RegisterController.java].
============================================================================
--%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Login</title>
        <link rel="stylesheet" href="css/styles.css"/>
    </head>
    <body>
        <div class="container">
            <h2>Spa Consultation Portal</h2>

            <%--
                Form gửi dữ liệu về MainController theo mô hình MVC2.
                Nút submit mang name="action" và value="Login" tự động đóng gói cặp tham số action=Login.
            --%>
            <form action="MainController" method="POST">
                <p>
                    <label for="email">Email:</label>
                    <input type="email" id="email" name="email"
                           value="<c:out value='${OLD_EMAIL}'/>" required=""/>
                </p>
                <p>
                    <%-- Mật khẩu không bao giờ đổ lại giá trị cũ để bảo đảm an toàn --%>
                    <label for="password">Password:</label>
                    <input type="password" id="password" name="password"
                           required=""/>
                </p>
                <p>
                    <input type="submit" value="Login" name="action"/>
                </p>
            </form>

            <%-- Hiển thị thông báo lỗi (đỏ) hoặc thông báo thành công (xanh) --%>
            <c:if test="${not empty ERROR}">
                <p class="error"><c:out value="${ERROR}"/></p>
            </c:if>
            <c:if test="${not empty MESSAGE}">
                <p class="message"><c:out value="${MESSAGE}"/></p>
            </c:if>

            <p>Do not have an account?
               <a href="register.jsp">Register here</a></p>
        </div>
    </body>
</html>
