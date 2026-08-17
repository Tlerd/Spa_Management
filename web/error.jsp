<%--
============================================================================
[VAI TRÒ & CHỨC NĂNG]
- Trang HIỂN THỊ LỖI HỆ THỐNG (error.jsp) - Mọi vai trò đều có thể vào khi xảy ra lỗi ngoài ý muốn.
- Tiếp nhận thông báo lỗi từ Request Attribute `ERROR` và hiển thị ra màn hình một cách an toàn.
- LƯU Ý AN NINH (Security): Chỉ hiển thị thông điệp thân thiện do lập trình viên đặt vào `ERROR`,
  TUYỆT ĐỐI KHÔNG in trực tiếp stack trace hoặc thông tin CSDL (tên bảng, SQL query) ra màn hình
  để tránh lộ thông tin nội bộ cho kẻ tấn công.
- Cung cấp sẵn 2 lối thoát (Navigation): Link quay về [dashboard.jsp] và [login.jsp].

[LUỒNG THỰC THI (EXECUTION FLOW)]
1. Khi có ngoại lệ (Exception) không xử lý được trong bất kỳ Controller nào (khối catch)
   hoặc khi `action` không được hỗ trợ trong MainController.
2. Controller đặt `request.setAttribute("ERROR", ...)` và forward sang [error.jsp].
3. Người dùng xem thông báo và có thể click "Back to dashboard" hoặc "Back to login".

[LUỒNG DỮ LIỆU (DATA FLOW)]
- Dữ liệu nhận từ Controller (Request attribute):
  + `${ERROR}`: Chuỗi thông báo lỗi (ví dụ "Action not supported", "System error, please try again later",...).

[LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
1. Sửa tên Request Attribute `ERROR`:
   -> BẮT BUỘC phải sửa tương ứng ở tất cả các Controller có lệnh `request.setAttribute("ERROR", ...)`.
2. Sửa đường dẫn link quay lại:
   -> [dashboard.jsp] hoặc [login.jsp].
============================================================================
--%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Error</title>
        <link rel="stylesheet" href="css/styles.css"/>
    </head>
    <body>
        <div class="container">
            <h2>Something went wrong</h2>

            <%-- HIỂN THỊ THÔNG BÁO LỖI TỪ CONTROLLER HOẶC THÔNG BÁO MẶC ĐỊNH --%>
            <c:choose>
                <c:when test="${empty ERROR}">
                    <p class="error">An unexpected error has occurred</p>
                </c:when>
                <c:otherwise>
                    <p class="error"><c:out value="${ERROR}"/></p>
                </c:otherwise>
            </c:choose>

            <%-- ĐƯỜNG DẪN THOÁT KHỎI TRANG LỖI CHO NGƯỜI DÙNG --%>
            <p>
                <a href="dashboard.jsp">Back to dashboard</a> |
                <a href="login.jsp">Back to login</a>
            </p>
        </div>
    </body>
</html>
