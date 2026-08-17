<%--
============================================================================
[VAI TRÒ & CHỨC NĂNG]
- Giao diện CẬP NHẬT DỊCH VỤ SPA (updateService.jsp) - CHỈ DÀNH CHO ADMIN.
- Hiển thị thông tin dịch vụ cũ lên form để Admin chỉnh sửa: `serviceID` (readonly), `serviceName`, `description`, `price`.
- Bảo vệ dữ liệu: Nếu đối tượng `SERVICE` trong Request bị rỗng (ví dụ người dùng vào thẳng URL mà không qua LoadServiceController),
  tự động redirect về `MainController?action=ViewServices`.

[LUỒNG THỰC THI (EXECUTION FLOW)]
1. Admin bấm Edit trên [serviceList.jsp] -> `MainController?action=LoadService&serviceID=...` -> LoadServiceController.
2. LoadServiceController nạp đối tượng `Service` vào Request attribute `SERVICE` và forward sang [updateService.jsp].
3. Admin sửa dữ liệu và submit form -> POST request gửi về `action="MainController"` kèm `<input type="submit" value="UpdateService" name="action"/>`.
4. MainController nhận action="UpdateService" -> Chuyển tiếp tới [UpdateServiceController].
5. UpdateServiceController xử lý:
   - Thành công -> Chuyển về [ViewServicesController] -> [serviceList.jsp].
   - Thất bại -> Quay lại [updateService.jsp] kèm thông báo lỗi `ERROR` và giữ lại dữ liệu vừa gõ.

[LUỒNG DỮ LIỆU (DATA FLOW)]
- Dữ liệu nhận từ LoadServiceController (Request attribute):
  + `${SERVICE.serviceID}`, `${SERVICE.serviceName}`, `${SERVICE.description}`, `${SERVICE.price}`.
- Dữ liệu gửi đi khi submit form (Request parameters):
  + `serviceID`, `serviceName`, `description`, `price`, `action=UpdateService`.

[LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
1. Ô nhập `serviceID`:
   -> BẮT BUỘC dùng `readonly=""` chứ KHÔNG DÙNG `disabled=""`. Vì ô disabled sẽ không được trình duyệt gửi đi,
      dẫn tới UpdateServiceController nhận `serviceID = null` và không thể cập nhật CSDL.
2. Nút submit `value="UpdateService"`:
   -> Phải khớp với UPDATE_SERVICE trong [MainController.java] và [UpdateServiceController.java].
3. Tên Request Attribute `SERVICE`:
   -> Phải khớp với `request.setAttribute("SERVICE", service)` trong [LoadServiceController.java] và [UpdateServiceController.java].
============================================================================
--%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Update Service</title>
        <link rel="stylesheet" href="css/styles.css"/>
    </head>
    <body>
        <%-- BẢO VỆ 1: Kiểm tra quyền Admin --%>
        <c:if test="${empty sessionScope.LOGIN_USER
                      or not sessionScope.LOGIN_USER.admin}">
            <c:redirect url="login.jsp"/>
        </c:if>

        <%-- BẢO VỆ 2: Kiểm tra dữ liệu dịch vụ có tồn tại trong Request không --%>
        <c:if test="${empty SERVICE}">
            <c:redirect url="MainController?action=ViewServices"/>
        </c:if>

        <div class="container">
            <h2>Update service</h2>

            <%-- FORM CẬP NHẬT DỊCH VỤ GỬI VỀ MAINCONTROLLER --%>
            <form action="MainController" method="POST">
                <p>
                    <label for="serviceID">Service ID:</label>
                    <%-- readonly: Khóa không cho sửa mã khóa chính nhưng VẪN gửi giá trị đi --%>
                    <input type="text" id="serviceID" name="serviceID"
                           value="<c:out value='${SERVICE.serviceID}'/>"
                           readonly=""/>
                </p>
                <p>
                    <label for="serviceName">Service name:</label>
                    <input type="text" id="serviceName" name="serviceName"
                           value="<c:out value='${SERVICE.serviceName}'/>"
                           maxlength="200" required=""/>
                </p>
                <p>
                    <label for="description">Description:</label><br/>
                    <textarea id="description" name="description"
                              rows="5" cols="50" maxlength="1000"
                              required=""><c:out value="${SERVICE.description}"/></textarea>
                </p>
                <p>
                    <label for="price">Price:</label>
                    <input type="number" step="0.01" min="0"
                           id="price" name="price"
                           value="<c:out value='${SERVICE.price}'/>" required=""/>
                </p>
                <p>
                    <input type="submit" value="UpdateService" name="action"/>
                </p>
            </form>

            <%-- HIỂN THỊ THÔNG BÁO LỖI NẾU CẬP NHẬT THẤT BẠI --%>
            <c:if test="${not empty ERROR}">
                <p class="error"><c:out value="${ERROR}"/></p>
            </c:if>

            <p><a href="MainController?action=ViewServices">Back to service list</a></p>
        </div>
    </body>
</html>
