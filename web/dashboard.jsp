<%--
============================================================================
[VAI TRÒ & CHỨC NĂNG]
- BẢNG ĐIỀU KHIỂN TRUNG TÂM (dashboard.jsp) - Mọi vai trò sau khi đăng nhập thành công đều tới đây.
- Phân quyền giao diện động (Role-based Navigation Menu):
  + Admin (ADM): Thấy menu "Manage services" và "Create a new service".
  + Staff (STF): Thấy menu "View consultations".
  + Customer (USR): Thấy menu "Book an appointment", "My appointments", "Review a service".
- Bảo vệ trang: Dùng `<c:redirect url="login.jsp"/>` nếu phiên chưa đăng nhập (`empty sessionScope.LOGIN_USER`).

[LUỒNG THỰC THI (EXECUTION FLOW)]
1. Kiểm tra sessionScope.LOGIN_USER: Nếu null -> Chuyển hướng ngay về [login.jsp].
2. Dựa vào getter boolean của lớp User:
   - `${sessionScope.LOGIN_USER.admin}` -> Gọi `isAdmin()`: Mở các link của Admin.
   - `${sessionScope.LOGIN_USER.staff}` -> Gọi `isStaff()`: Mở link của Staff.
   - `${sessionScope.LOGIN_USER.customer}` -> Gọi `isCustomer()`: Mở các link của Customer.
3. Khi bấm các đường link trên menu -> Gửi GET request về MainController với tham số action tương ứng.
4. Khi bấm nút "Logout" -> Submit form POST về MainController (action=Logout) -> LogoutController.

[LUỒNG DỮ LIỆU (DATA FLOW)]
- Dữ liệu nhận từ Session:
  + `${sessionScope.LOGIN_USER.fullName}`: Tên người dùng.
  + `${sessionScope.LOGIN_USER.roleID}`: Mã vai trò (ADM, STF, USR).
  + `${sessionScope.LOGIN_USER.email}`: Email đăng nhập.
- Dữ liệu nhận từ Request:
  + `${MESSAGE}`: Thông báo thành công nếu có.
  + `${ERROR}`: Thông báo lỗi nếu có.

[LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
1. Sửa các link menu:
   - `action=ViewServices` -> Phải khớp VIEW_SERVICES trong [MainController.java].
   - `action=ViewConsultation` -> Phải khớp VIEW_CONSULTATION trong [MainController.java].
   - `action=LoadBooking` -> Phải khớp LOAD_BOOKING trong [MainController.java].
   - `action=ViewAppointments` -> Phải khớp VIEW_APPOINTMENTS trong [MainController.java].
   - `action=LoadReview` -> Phải khớp LOAD_REVIEW trong [MainController.java].
2. Link `createService.jsp`: Là link duy nhất trỏ thẳng vào JSP vì form tạo mới hoàn toàn trống,
   không cần tải dữ liệu từ CSDL.
============================================================================
--%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Dashboard</title>
        <link rel="stylesheet" href="css/styles.css"/>
    </head>
    <body>
        <%-- BẢO VỆ TRANG: Nếu chưa đăng nhập thì dừng render và chuyển hướng về login.jsp --%>
        <c:if test="${empty sessionScope.LOGIN_USER}">
            <c:redirect url="login.jsp"/>
        </c:if>

        <div class="container">
            <h2>Welcome, <c:out value="${sessionScope.LOGIN_USER.fullName}"/></h2>
            <p>Role: <c:out value="${sessionScope.LOGIN_USER.roleID}"/>
               | Email: <c:out value="${sessionScope.LOGIN_USER.email}"/></p>

            <%-- Thông báo phản hồi từ hệ thống --%>
            <c:if test="${not empty MESSAGE}">
                <p class="message"><c:out value="${MESSAGE}"/></p>
            </c:if>
            <c:if test="${not empty ERROR}">
                <p class="error"><c:out value="${ERROR}"/></p>
            </c:if>

            <%-- MENU ĐIỀU HƯỚNG THEO TỪNG VAI TRÒ DỰA VÀO CÁC PHƯƠNG THỨC TRONG LỚP USER --%>
            <nav>
                <ul>
                    <c:choose>
                        <%-- 1. MENU DÀNH CHO ADMIN --%>
                        <c:when test="${sessionScope.LOGIN_USER.admin}">
                            <li><a href="MainController?action=ViewServices">Manage services</a></li>
                            <li><a href="createService.jsp">Create a new service</a></li>
                        </c:when>

                        <%-- 2. MENU DÀNH CHO NHÂN VIÊN TƯ VẤN (STAFF) --%>
                        <c:when test="${sessionScope.LOGIN_USER.staff}">
                            <li><a href="MainController?action=ViewConsultation">View consultations</a></li>
                        </c:when>

                        <%-- 3. MENU DÀNH CHO KHÁCH HÀNG (CUSTOMER) --%>
                        <c:when test="${sessionScope.LOGIN_USER.customer}">
                            <li><a href="MainController?action=LoadBooking">Book an appointment</a></li>
                            <li><a href="MainController?action=ViewAppointments">My appointments</a></li>
                            <li><a href="MainController?action=LoadReview">Review a service</a></li>
                        </c:when>
                    </c:choose>
                </ul>
            </nav>

            <%-- FORM ĐĂNG XUẤT --%>
            <form action="MainController" method="POST">
                <input type="submit" value="Logout" name="action"/>
            </form>
        </div>
    </body>
</html>
