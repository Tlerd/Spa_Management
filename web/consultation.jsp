<%--
============================================================================
[VAI TRÒ & CHỨC NĂNG]
- Giao diện TƯ VẤN VÀ THEO DÕI DỊCH VỤ (consultation.jsp) - CHỈ DÀNH CHO NHÂN VIÊN (STAFF).
- Hiển thị danh sách các lịch hẹn được phân công cho CHÍNH nhân viên đang đăng nhập.
- Cho phép nhân viên thực hiện 2 thao tác cốt lõi:
  1. Ghi chú tư vấn: Ô `<textarea name="notes">` có sẵn nội dung cũ và nút submit "SaveNotes".
  2. Đánh dấu hoàn thành: Nếu lịch hẹn đang 'Pending' (${item.pending}), hiển thị nút submit "CompleteAppointment".

[LUỒNG THỰC THI (EXECUTION FLOW)]
1. Staff click "View consultations" trên [dashboard.jsp] -> `MainController?action=ViewConsultation` -> ViewConsultationController.
2. ViewConsultationController nạp danh sách lịch hẹn của staff vào Request attribute `LIST_CONSULTATION` -> forward sang [consultation.jsp].
3. LƯU GHI CHÚ (SaveNotes):
   - Submit form POST `MainController` (action=SaveNotes&appointmentID=...&notes=...) -> SaveNotesController -> ViewConsultationController.
4. HOÀN THÀNH LỊCH (CompleteAppointment):
   - Submit form POST `MainController` (action=CompleteAppointment&appointmentID=...) -> CompleteAppointmentController -> ViewConsultationController.

[LUỒNG DỮ LIỆU (DATA FLOW)]
- Dữ liệu nhận từ ViewConsultationController (Request attributes):
  + `${LIST_CONSULTATION}`: `List<Appointment>` -> Đổ vào vòng lặp `<c:forEach var="item" items="${LIST_CONSULTATION}">`.
  + `${MESSAGE}`: Thông báo thành công (lưu ghi chú, hoàn thành lịch).
  + `${ERROR}`: Thông báo lỗi nếu có.
- Dữ liệu từng dòng:
  + `${item.appointmentID}`, `${item.userName}` (tên khách), `${item.serviceName}` (tên dịch vụ),
    `${item.appointmentDate}`, `${item.status}`, `${item.consultationNotes}`.

[LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
1. Sửa nút Lưu ghi chú (`value="SaveNotes"`):
   -> Phải khớp với SAVE_NOTES trong [MainController.java] và [SaveNotesController.java].
2. Sửa nút Hoàn thành (`value="CompleteAppointment"`):
   -> Phải khớp với COMPLETE_APPOINTMENT trong [MainController.java] và [CompleteAppointmentController.java].
3. Ô `<textarea name="notes">`:
   -> Phải đổ `<c:out value="${item.consultationNotes}"/>` ở giữa thẻ mở và đóng để hiển thị ghi chú đã lưu trước đó.
============================================================================
--%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<fmt:setLocale value="en_US"/>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Consultation and Service Tracking</title>
        <link rel="stylesheet" href="css/styles.css"/>
    </head>
    <body>
        <%-- BẢO VỆ PHÂN QUYỀN: Chỉ Nhân viên (Staff) mới được truy cập --%>
        <c:if test="${empty sessionScope.LOGIN_USER
                      or not sessionScope.LOGIN_USER.staff}">
            <c:redirect url="login.jsp"/>
        </c:if>

        <div class="container">
            <h2>Consultation and service tracking</h2>
            <p>Staff: <c:out value="${sessionScope.LOGIN_USER.fullName}"/></p>

            <%-- HIỂN THỊ THÔNG BÁO PHẢN HỒI HỆ THỐNG --%>
            <c:if test="${not empty MESSAGE}">
                <p class="message"><c:out value="${MESSAGE}"/></p>
            </c:if>
            <c:if test="${not empty ERROR}">
                <p class="error"><c:out value="${ERROR}"/></p>
            </c:if>

            <%-- BẢNG THEO DÕI TƯ VẤN VÀ TIẾN ĐỘ DỊCH VỤ --%>
            <table border="1" cellpadding="5" cellspacing="0">
                <tr>
                    <th>Appointment ID</th>
                    <th>Customer</th>
                    <th>Service</th>
                    <th>Date</th>
                    <th>Status</th>
                    <th>Consultation notes</th>
                    <th>Complete</th>
                </tr>
                <c:choose>
                    <c:when test="${empty LIST_CONSULTATION}">
                        <tr>
                            <td colspan="7">No appointment is assigned to you</td>
                        </tr>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="item" items="${LIST_CONSULTATION}">
                            <tr>
                                <td><c:out value="${item.appointmentID}"/></td>
                                <td><c:out value="${item.userName}"/></td>
                                <td><c:out value="${item.serviceName}"/></td>
                                <td>
                                    <fmt:formatDate value="${item.appointmentDate}"
                                                    pattern="dd/MM/yyyy HH:mm"/>
                                </td>
                                <td><c:out value="${item.status}"/></td>
                                <%--
                                    FORM LƯU GHI CHÚ TƯ VẤN (Gửi POST về MainController action=SaveNotes)
                                    Đổ sẵn nội dung ghi chú đã lưu trước đó vào ô textarea
                                --%>
                                <td>
                                    <form action="MainController" method="POST">
                                        <input type="hidden" name="appointmentID"
                                               value="<c:out value='${item.appointmentID}'/>"/>
                                        <textarea name="notes" rows="3" cols="30"
                                                  maxlength="1000"><c:out value="${item.consultationNotes}"/></textarea><br/>
                                        <input type="submit" value="SaveNotes" name="action"/>
                                    </form>
                                </td>
                                <%--
                                    FORM ĐÁNH DẤU HOÀN THÀNH LỊCH HẸN (Chuyển trạng thái Pending -> Completed)
                                --%>
                                <td>
                                    <c:choose>
                                        <c:when test="${item.pending}">
                                            <form action="MainController" method="POST"
                                                  onsubmit="return confirm('Mark this appointment as completed ?');">
                                                <input type="hidden" name="appointmentID"
                                                       value="<c:out value='${item.appointmentID}'/>"/>
                                                <input type="submit" value="CompleteAppointment"
                                                       name="action"/>
                                            </form>
                                        </c:when>
                                        <c:otherwise>
                                            <c:out value="${item.status}"/>
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
