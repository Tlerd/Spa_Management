<%--
============================================================================
[VAI TRÒ & CHỨC NĂNG]
- Giao diện ĐẶT LỊCH HẸN SPA (bookAppointment.jsp) - CHỈ DÀNH CHO CUSTOMER.
- Form gồm: Ô chọn dịch vụ (`<select name="serviceID">`) và ô chọn ngày giờ hẹn (`<input type="datetime-local" name="appointmentDate">`).
- Bảo vệ form: Nếu không có dịch vụ nào khả dụng trong CSDL (`empty LIST_SERVICE`), ẩn toàn bộ form
  và hiển thị thông báo "No service is available at the moment".

[LUỒNG THỰC THI (EXECUTION FLOW)]
1. Khách hàng click "Book an appointment" trên [dashboard.jsp] -> `MainController?action=LoadBooking` -> LoadBookingController.
2. LoadBookingController lấy toàn bộ dịch vụ và đặt vào Request attribute `LIST_SERVICE` -> forward sang [bookAppointment.jsp].
3. Khách chọn dịch vụ, chọn ngày giờ và submit form -> POST request gửi về `action="MainController"` kèm `<input type="submit" value="BookAppointment" name="action"/>`.
4. MainController nhận action="BookAppointment" -> Chuyển tiếp tới [BookAppointmentController].
5. BookAppointmentController xử lý:
   - Thành công -> Chuyển sang [ViewAppointmentsController] -> [appointmentList.jsp].
   - Thất bại (ngày quá khứ, dịch vụ hết tồn tại) -> Chuyển về [LoadBookingController] -> [bookAppointment.jsp] kèm thông báo lỗi `ERROR`.

[LUỒNG DỮ LIỆU (DATA FLOW)]
- Dữ liệu nhận từ LoadBookingController (Request attribute):
  + `${LIST_SERVICE}`: `List<Service>` -> Đổ vào các thẻ `<option value="${service.serviceID}">`.
- Dữ liệu gửi đi khi submit form (Request parameters):
  + `serviceID`: Mã dịch vụ khách chọn.
  + `appointmentDate`: Chuỗi ngày giờ (định dạng "yyyy-MM-ddTHH:mm").
  + `action`: "BookAppointment".

[LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
1. Thẻ `<select id="serviceID" name="serviceID">`:
   -> `name="serviceID"` phải khớp với `request.getParameter("serviceID")` trong [BookAppointmentController.java].
2. Thẻ `<input type="datetime-local" id="appointmentDate" name="appointmentDate">`:
   -> `name="appointmentDate"` phải khớp với `request.getParameter("appointmentDate")` và hàm parseDateTime() trong [BookAppointmentController.java].
3. Nút submit `value="BookAppointment"`:
   -> Phải khớp với BOOK_APPOINTMENT trong [MainController.java] và [BookAppointmentController.java].
============================================================================
--%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%-- Cố định Locale chuẩn en_US để định dạng giá tiền dịch vụ nhất quán --%>
<fmt:setLocale value="en_US"/>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Book an Appointment</title>
        <link rel="stylesheet" href="css/styles.css"/>
    </head>
    <body>
        <%-- BẢO VỆ PHÂN QUYỀN: Chỉ Khách hàng (Customer) mới được truy cập --%>
        <c:if test="${empty sessionScope.LOGIN_USER
                      or not sessionScope.LOGIN_USER.customer}">
            <c:redirect url="login.jsp"/>
        </c:if>

        <div class="container">
            <h2>Book an appointment</h2>

            <%--
                Bao toàn bộ form trong khối kiểm tra danh sách rỗng:
                Nếu danh sách dịch vụ rỗng -> Không hiển thị form để tránh gửi submit với serviceID=null
            --%>
            <c:choose>
                <c:when test="${empty LIST_SERVICE}">
                    <p>No service is available at the moment.</p>
                </c:when>
                <c:otherwise>
                    <%-- FORM ĐẶT LỊCH HẸN GỬI VỀ MAINCONTROLLER --%>
                    <form action="MainController" method="POST">
                        <p>
                            <label for="serviceID">Select service:</label>
                            <select id="serviceID" name="serviceID" required="">
                                <c:forEach var="service" items="${LIST_SERVICE}">
                                    <option value="<c:out value='${service.serviceID}'/>"><c:out
                                        value="${service.serviceName}"/> - <fmt:formatNumber
                                        value="${service.price}" type="number"
                                        minFractionDigits="2" maxFractionDigits="2"/></option>
                                </c:forEach>
                            </select>
                        </p>
                        <p>
                            <label for="appointmentDate">Appointment date:</label>
                            <input type="datetime-local" id="appointmentDate"
                                   name="appointmentDate" required=""/>
                        </p>
                        <p>
                            <input type="submit" value="BookAppointment" name="action"/>
                        </p>
                    </form>
                </c:otherwise>
            </c:choose>

            <%-- HIỂN THỊ THÔNG BÁO LỖI NẾU ĐẶT LỊCH THẤT BẠI --%>
            <c:if test="${not empty ERROR}">
                <p class="error"><c:out value="${ERROR}"/></p>
            </c:if>

            <p><a href="dashboard.jsp">Back to dashboard</a></p>
        </div>
    </body>
</html>
