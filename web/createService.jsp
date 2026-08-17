<%--
============================================================================
[VAI TRÒ & CHỨC NĂNG]
- Giao diện THÊM DỊCH VỤ SPA MỚI (createService.jsp) - CHỈ DÀNH CHO ADMIN.
- Form nhập: `serviceID` (khóa chính không tự sinh), `serviceName`, `description`, `price`.

[LUỒNG THỰC THI (EXECUTION FLOW)]
1. Được truy cập trực tiếp từ link trên [dashboard.jsp] hoặc [serviceList.jsp].
2. Submit form -> POST request gửi về `action="MainController"` kèm `<input type="submit" value="CreateService" name="action"/>`.
3. MainController nhận action="CreateService" -> Chuyển tiếp tới [CreateServiceController].
4. CreateServiceController xử lý:
   - Thành công -> Chuyển tiếp về [ViewServicesController] để nạp lại danh sách mới nhất và hiển thị [serviceList.jsp].
   - Thất bại -> Quay về [createService.jsp] kèm thông báo lỗi `ERROR` và giữ lại các giá trị đã gõ `OLD_*`.

[LUỒNG DỮ LIỆU (DATA FLOW)]
- Dữ liệu gửi đi (Request parameters):
  + `serviceID`, `serviceName`, `description`, `price`, `action=CreateService`.
- Dữ liệu nhận từ Controller (Request attributes khi có lỗi):
  + `${OLD_SERVICE_ID}`, `${OLD_SERVICE_NAME}`, `${OLD_DESCRIPTION}`, `${OLD_PRICE}`.
  + `${ERROR}`: Thông báo lỗi chi tiết.

[LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
1. Ô nhập `serviceID`:
   -> BẮT BUỘC phải có vì cột serviceID trong bảng tblServices là Primary Key và không tự sinh (non-identity).
2. Ô nhập `description` (<textarea>):
   -> Đặt giá trị cũ `<c:out value="${OLD_DESCRIPTION}"/>` GIỮA thẻ mở `<textarea>` và thẻ đóng `</textarea>`,
      không dùng thuộc tính `value=""`.
3. Nút submit `value="CreateService"`:
   -> Phải khớp với CREATE_SERVICE trong [MainController.java] và [CreateServiceController.java].
4. Link "Back to service list":
   -> Trỏ về `MainController?action=ViewServices` để nạp dữ liệu từ CSDL, KHÔNG trỏ thẳng tới `serviceList.jsp`.
============================================================================
--%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Create Service</title>
        <link rel="stylesheet" href="css/styles.css"/>
    </head>
    <body>
        <%-- BẢO VỆ PHÂN QUYỀN: Chỉ Admin mới được truy cập --%>
        <c:if test="${empty sessionScope.LOGIN_USER
                      or not sessionScope.LOGIN_USER.admin}">
            <c:redirect url="login.jsp"/>
        </c:if>

        <div class="container">
            <h2>Create a new spa service</h2>

            <%-- FORM TẠO MỚI DỊCH VỤ GỬI VỀ MAINCONTROLLER --%>
            <form action="MainController" method="POST">
                <p>
                    <label for="serviceID">Service ID:</label>
                    <input type="text" id="serviceID" name="serviceID"
                           value="<c:out value='${OLD_SERVICE_ID}'/>"
                           maxlength="50" required=""/>
                </p>
                <p>
                    <label for="serviceName">Service name:</label>
                    <input type="text" id="serviceName" name="serviceName"
                           value="<c:out value='${OLD_SERVICE_NAME}'/>"
                           maxlength="200" required=""/>
                </p>
                <p>
                    <label for="description">Description:</label><br/>
                    <textarea id="description" name="description"
                              rows="5" cols="50" maxlength="1000"
                              required=""><c:out value="${OLD_DESCRIPTION}"/></textarea>
                </p>
                <p>
                    <label for="price">Price:</label>
                    <input type="number" step="0.01" min="0"
                           id="price" name="price"
                           value="<c:out value='${OLD_PRICE}'/>" required=""/>
                </p>
                <p>
                    <input type="submit" value="CreateService" name="action"/>
                </p>
            </form>

            <%-- HIỂN THỊ THÔNG BÁO LỖI NẾU CÓ --%>
            <c:if test="${not empty ERROR}">
                <p class="error"><c:out value="${ERROR}"/></p>
            </c:if>

            <%-- LINK QUAY LẠI DANH SÁCH QUA VIEWSERVICESCONTROLLER --%>
            <p><a href="MainController?action=ViewServices">Back to service list</a></p>
        </div>
    </body>
</html>
