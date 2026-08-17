<%--
============================================================================
[VAI TRÒ & CHỨC NĂNG]
- Giao diện QUẢN LÝ LỊCH HẸN CỦA KHÁCH HÀNG (appointmentList.jsp) - CHỈ DÀNH CHO CUSTOMER.
- Hiển thị danh sách lịch hẹn của chính khách hàng đó:
  Mã lịch hẹn, Tên dịch vụ, Ngày giờ hẹn, Trạng thái, Tên nhân viên phụ trách, Cột hành động.
- Cột hành động (Action) chia 3 nhánh động theo nghiệp vụ:
  + Lịch hẹn đang 'Pending' (${app.pending}) -> Hiển thị Form POST nút "CancelAppointment".
  + Lịch hẹn đã 'Completed' (${app.completed}) -> Hiển thị link "Write a review" (`action=LoadReview`).
  + Lịch hẹn đã 'Cancelled' -> Chỉ hiển thị chữ trạng thái (không có nút thao tác).

[LUỒNG THỰC THI (EXECUTION FLOW)]
1. Khách hàng click "My appointments" trên [dashboard.jsp] -> `MainController?action=ViewAppointments` -> ViewAppointmentsController.
2. ViewAppointmentsController lấy danh sách lịch hẹn và đặt vào Request attribute `LIST_APPOINTMENT` -> forward sang [appointmentList.jsp].
3. HỦY LỊCH:
   - Khách bấm Cancel -> Submit form POST `MainController` (action=CancelAppointment&appointmentID=...) -> CancelAppointmentController -> ViewAppointmentsController.
4. ĐÁNH GIÁ:
   - Khách bấm "Write a review" -> Gửi GET `MainController?action=LoadReview&serviceID=...` -> LoadReviewController -> [reviewService.jsp].

[LUỒNG DỮ LIỆU (DATA FLOW)]
- Dữ liệu nhận từ ViewAppointmentsController (Request attributes):
  + `${LIST_APPOINTMENT}`: `List<Appointment>` -> Đổ vào vòng lặp `<c:forEach var="app" items="${LIST_APPOINTMENT}">`.
  + `${MESSAGE}`: Thông báo thành công (ví dụ "Appointment ... has been cancelled").
  + `${ERROR}`: Thông báo lỗi nếu có.
- Dữ liệu từng dòng:
  + `${app.appointmentID}`, `${app.serviceName}`, `${app.appointmentDate}` (format dạng dd/MM/yyyy HH:mm),
    `${app.status}`, `${app.staffName}` (nếu rỗng thì hiện "Not assigned").

[LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
1. Sửa tên Request Attribute `LIST_APPOINTMENT`:
   -> Phải sửa `request.setAttribute("LIST_APPOINTMENT", ...)` trong [ViewAppointmentsController.java].
2. Sửa nút Hủy lịch (`action=CancelAppointment`):
   -> Phải khớp với CANCEL_APPOINTMENT trong [MainController.java] và [CancelAppointmentController.java].
3. Sửa link Viết đánh giá (`action=LoadReview`):
   -> Phải khớp với LOAD_REVIEW trong [MainController.java] và [LoadReviewController.java].
============================================================================
--%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%--
    Cố định Locale en_US để formatDate luôn in theo định dạng chuẩn "dd/MM/yyyy HH:mm",
    tránh bị rơi về Timestamp.toString() mặc định của Java nếu Client không gửi header Accept-Language.
--%>
<fmt:setLocale value="en_US"/>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>My Appointments</title>
        <link rel="stylesheet" href="css/styles.css"/>
    </head>
    <body>
        <%-- BẢO VỆ PHÂN QUYỀN: Chỉ Khách hàng (Customer) mới được truy cập --%>
        <c:if test="${empty sessionScope.LOGIN_USER
                      or not sessionScope.LOGIN_USER.customer}">
            <c:redirect url="login.jsp"/>
        </c:if>

        <div class="container">
            <h2>Your appointments</h2>

            <%-- HIỂN THỊ THÔNG BÁO PHẢN HỒI HỆ THỐNG --%>
            <c:if test="${not empty MESSAGE}">
                <p class="message"><c:out value="${MESSAGE}"/></p>
            </c:if>
            <c:if test="${not empty ERROR}">
                <p class="error"><c:out value="${ERROR}"/></p>
            </c:if>

            <%-- BẢNG DANH SÁCH LỊCH HẸN CỦA KHÁCH HÀNG --%>
            <table border="1" cellpadding="5" cellspacing="0">
                <tr>
                    <th>Appointment ID</th>
                    <th>Service</th>
                    <th>Date</th>
                    <th>Status</th>
                    <th>Staff</th>
                    <th>Action</th>
                </tr>
                <c:choose>
                    <c:when test="${empty LIST_APPOINTMENT}">
                        <tr>
                            <td colspan="6">You have no appointment yet</td>
                        </tr>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="app" items="${LIST_APPOINTMENT}">
                            <tr>
                                <td><c:out value="${app.appointmentID}"/></td>
                                <td><c:out value="${app.serviceName}"/></td>
                                <td>
                                    <%-- Định dạng ngày giờ chuẩn dd/MM/yyyy HH:mm --%>
                                    <fmt:formatDate value="${app.appointmentDate}"
                                                    pattern="dd/MM/yyyy HH:mm"/>
                                </td>
                                <td><c:out value="${app.status}"/></td>
                                <td>
                                    <%-- Xử lý trường hợp chưa phân công nhân viên tư vấn --%>
                                    <c:choose>
                                        <c:when test="${empty app.staffName}">Not assigned</c:when>
                                        <c:otherwise><c:out value="${app.staffName}"/></c:otherwise>
                                    </c:choose>
                                </td>
                                <%--
                                    CỘT HÀNH ĐỘNG ĐỘNG THEO TRẠNG THÁI LỊCH HẸN:
                                    - Pending: Cho phép hủy lịch (POST kèm confirm dialog)
                                    - Completed: Cho phép viết đánh giá dịch vụ
                                    - Cancelled: Không có thao tác
                                --%>
                                <td>
                                    <c:choose>
                                        <c:when test="${app.pending}">
                                            <form action="MainController" method="POST"
                                                  onsubmit="return confirm('Cancel this appointment ?');">
                                                <input type="hidden" name="appointmentID"
                                                       value="<c:out value='${app.appointmentID}'/>"/>
                                                <input type="submit" value="CancelAppointment"
                                                       name="action"/>
                                            </form>
                                        </c:when>
                                        <c:when test="${app.completed}">
                                            <a href="MainController?action=LoadReview&serviceID=<c:out value='${app.serviceID}'/>">Write a review</a>
                                        </c:when>
                                        <c:otherwise>
                                            <c:out value="${app.status}"/>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </table>

            <p><a href="dashboard.jsp">Back to dashboard</a></p>
        </div>
    </body>
</html>
