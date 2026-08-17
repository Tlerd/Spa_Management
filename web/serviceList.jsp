<%--
============================================================================
[VAI TRÒ & CHỨC NĂNG]
- Giao diện QUẢN LÝ DANH SÁCH DỊCH VỤ (serviceList.jsp) - CHỈ DÀNH CHO ADMIN.
- Hiển thị danh sách dịch vụ spa dạng bảng: Service ID, Service Name, Description, Price, nút Edit, nút Delete.
- Có ô tìm kiếm theo tên dịch vụ.

[LUỒNG THỰC THI (EXECUTION FLOW)]
1. Trang này KHÔNG THỂ tự truy vấn CSDL, mọi truy cập bắt buộc phải đi qua `ViewServicesController`.
2. Bảo vệ trang 2 lớp:
   - Lớp 1: Server-side check tại ViewServicesController.
   - Lớp 2: JSP-level check: `<c:if test="${empty sessionScope.LOGIN_USER or not sessionScope.LOGIN_USER.admin}">`.
3. Tìm kiếm: Submit form GET về `MainController?action=ViewServices&search=...` -> ViewServicesController.
4. Sửa: Bấm link GET `MainController?action=LoadService&serviceID=...` -> LoadServiceController -> [updateService.jsp].
5. Xóa: Submit form POST về `MainController?action=DeleteService&serviceID=...` -> DeleteServiceController -> ViewServicesController.

[LUỒNG DỮ LIỆU (DATA FLOW)]
- Dữ liệu nhận từ ViewServicesController (Request attributes):
  + `${LIST_SERVICE}`: `List<Service>` -> Đổ vào vòng lặp `<c:forEach var="service" items="${LIST_SERVICE}">`.
  + `${SEARCH}`: Chuỗi tìm kiếm -> Giữ lại trong ô text input.
  + `${MESSAGE}`: Thông báo thành công (tạo mới, cập nhật, xóa dịch vụ).
  + `${ERROR}`: Thông báo lỗi (ví dụ không thể xóa vì dịch vụ đang có lịch hẹn/đánh giá).
- Dữ liệu từng dòng trong bảng:
  + `${service.serviceID}`, `${service.serviceName}`, `${service.description}`, `${service.price}` (dùng `<fmt:formatNumber>` 2 chữ số thập phân).

[LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
1. Sửa tên Request Attribute `LIST_SERVICE`:
   -> BẮT BUỘC phải sửa tương ứng trong [ViewServicesController.java] dòng `request.setAttribute("LIST_SERVICE", ...)`.
2. Sửa link Sửa (`action=LoadService`):
   -> Phải khớp với LOAD_SERVICE trong [MainController.java] và [LoadServiceController.java].
3. Sửa nút Xóa (`action=DeleteService`):
   -> Phải khớp với DELETE_SERVICE trong [MainController.java] và [DeleteServiceController.java].
   -> Chú ý: Nút xóa dùng form POST kèm hộp thoại JavaScript `confirm(...)` để ngăn chặn xóa nhầm.
============================================================================
--%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%-- Cố định Locale chuẩn en_US để hiển thị dấu chấm thập phân và định dạng số tiền nhất quán --%>
<fmt:setLocale value="en_US"/>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Service List</title>
        <link rel="stylesheet" href="css/styles.css"/>
    </head>
    <body>
        <%-- LỚP BẢO VỆ PHÂN QUYỀN: Chỉ tài khoản Admin mới được xem trang này --%>
        <c:if test="${empty sessionScope.LOGIN_USER
                      or not sessionScope.LOGIN_USER.admin}">
            <c:redirect url="login.jsp"/>
        </c:if>

        <div class="container">
            <h2>Spa services</h2>

            <%-- FORM TÌM KIẾM DỊCH VỤ THEO TÊN (DÙNG PHƯƠNG THỨC GET) --%>
            <form action="MainController" method="GET">
                Service name:
                <input type="text" name="search"
                       value="<c:out value='${SEARCH}'/>"/>
                <input type="submit" value="ViewServices" name="action"/>
            </form>

            <p><a href="createService.jsp">Create a new service</a></p>

            <%-- THÔNG BÁO PHẢN HỒI HỆ THỐNG --%>
            <c:if test="${not empty MESSAGE}">
                <p class="message"><c:out value="${MESSAGE}"/></p>
            </c:if>
            <c:if test="${not empty ERROR}">
                <p class="error"><c:out value="${ERROR}"/></p>
            </c:if>

            <%-- BẢNG HIỂN THỊ DANH SÁCH DỊCH VỤ --%>
            <table border="1" cellpadding="5" cellspacing="0">
                <tr>
                    <th>Service ID</th>
                    <th>Service name</th>
                    <th>Description</th>
                    <th>Price</th>
                    <th>Edit</th>
                    <th>Delete</th>
                </tr>
                <c:choose>
                    <c:when test="${empty LIST_SERVICE}">
                        <tr>
                            <td colspan="6">No service found</td>
                        </tr>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="service" items="${LIST_SERVICE}">
                            <tr>
                                <td><c:out value="${service.serviceID}"/></td>
                                <td><c:out value="${service.serviceName}"/></td>
                                <td><c:out value="${service.description}"/></td>
                                <td>
                                    <%-- Định dạng hiển thị giá tiền 2 chữ số thập phân --%>
                                    <fmt:formatNumber value="${service.price}"
                                                      type="number"
                                                      minFractionDigits="2"
                                                      maxFractionDigits="2"/>
                                </td>
                                <%--
                                    Nút SỬA: Dùng liên kết GET vì chỉ nạp dữ liệu lên form sửa, không làm đổi dữ liệu CSDL.
                                --%>
                                <td>
                                    <a href="MainController?action=LoadService&serviceID=<c:out value='${service.serviceID}'/>">Edit</a>
                                </td>
                                <%--
                                    Nút XÓA: BẮT BUỘC dùng form POST kèm hộp thoại xác nhận JS để tránh xóa ngoài ý muốn.
                                --%>
                                <td>
                                    <form action="MainController" method="POST"
                                          onsubmit="return confirm('Delete service ${service.serviceID} ?');">
                                        <input type="hidden" name="serviceID"
                                               value="<c:out value='${service.serviceID}'/>"/>
                                        <input type="submit" value="DeleteService"
                                               name="action"/>
                                    </form>
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
