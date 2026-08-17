<%--
============================================================================
[VAI TRÒ & CHỨC NĂNG]
- Giao diện ĐÁNH GIÁ DỊCH VỤ (reviewService.jsp) - CHỈ DÀNH CHO CUSTOMER.
- Gồm 2 phần chính:
  1. Form viết đánh giá mới:
     - Ô chọn dịch vụ (`<select name="serviceID">`): Chỉ hiển thị các dịch vụ mà khách hàng ĐÃ HOÀN THÀNH và CHƯA ĐÁNH GIÁ.
     - Ô nhập điểm đánh giá (`<input type="number" name="rating" min="1" max="5">`).
     - Ô nhập bình luận (`<textarea name="comments">`).
     - Nếu không có dịch vụ nào đủ điều kiện (`empty LIST_SERVICE`), hiển thị thông báo "You can only review a service after the appointment is completed".
  2. Bảng lịch sử các đánh giá cũ ("Your previous reviews"): Hiển thị các dịch vụ khách đã đánh giá trước đó (`LIST_REVIEW`).

[LUỒNG THỰC THI (EXECUTION FLOW)]
1. Khách bấm "Review a service" trên [dashboard.jsp] hoặc "Write a review" trên [appointmentList.jsp]
   -> `MainController?action=LoadReview` -> LoadReviewController.
2. LoadReviewController nạp 2 danh sách (`LIST_SERVICE`, `LIST_REVIEW`) -> forward sang [reviewService.jsp].
3. Gửi đánh giá:
   - Submit form POST `action="MainController"` kèm `<input type="submit" value="ReviewService" name="action"/>`.
   - MainController nhận action="ReviewService" -> Chuyển tiếp tới [ReviewServiceController].
   - ReviewServiceController lưu đánh giá và forward về lại [LoadReviewController] -> [reviewService.jsp].

[LUỒNG DỮ LIỆU (DATA FLOW)]
- Dữ liệu nhận từ LoadReviewController (Request attributes):
  + `${LIST_SERVICE}`: `List<Service>` -> Đổ vào `<select name="serviceID">`.
  + `${LIST_REVIEW}`: `List<Review>` -> Đổ vào bảng "Your previous reviews".
  + `${MESSAGE}`: Thông báo "Thank you, your review has been saved".
  + `${ERROR}`: Thông báo lỗi nếu có.
- Dữ liệu gửi đi khi submit form (Request parameters):
  + `serviceID`, `rating`, `comments`, `action=ReviewService`.

[LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
1. Sửa nút submit `value="ReviewService"`:
   -> Phải khớp với REVIEW_SERVICE trong [MainController.java] và [ReviewServiceController.java].
2. Thuộc tính `min="1" max="5"`:
   -> Phải đồng bộ với validate ratingValue trong [ReviewServiceController.java] và ràng buộc CK_Rev_Rating trong [SpaConsultationPortal.sql].
3. Tên Request Attributes `LIST_SERVICE` và `LIST_REVIEW`:
   -> Phải khớp với [LoadReviewController.java].
============================================================================
--%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Review a Service</title>
        <link rel="stylesheet" href="css/styles.css"/>
    </head>
    <body>
        <%-- BẢO VỆ PHÂN QUYỀN: Chỉ Khách hàng (Customer) mới được truy cập --%>
        <c:if test="${empty sessionScope.LOGIN_USER
                      or not sessionScope.LOGIN_USER.customer}">
            <c:redirect url="login.jsp"/>
        </c:if>

        <div class="container">
            <h2>Review a service</h2>

            <%-- HIỂN THỊ THÔNG BÁO PHẢN HỒI HỆ THỐNG --%>
            <c:if test="${not empty MESSAGE}">
                <p class="message"><c:out value="${MESSAGE}"/></p>
            </c:if>
            <c:if test="${not empty ERROR}">
                <p class="error"><c:out value="${ERROR}"/></p>
            </c:if>

            <%-- PHẦN 1: FORM VIẾT ĐÁNH GIÁ MỚI --%>
            <c:choose>
                <%-- Nếu không có dịch vụ nào đủ điều kiện (chưa hoàn thành hoặc đã đánh giá hết) --%>
                <c:when test="${empty LIST_SERVICE}">
                    <p>You can only review a service after the appointment
                       is completed.</p>
                </c:when>
                <c:otherwise>
                    <form action="MainController" method="POST">
                        <p>
                            <label for="serviceID">Select service:</label>
                            <select id="serviceID" name="serviceID" required="">
                                <c:forEach var="service" items="${LIST_SERVICE}">
                                    <option value="<c:out value='${service.serviceID}'/>"><c:out
                                        value="${service.serviceName}"/></option>
                                </c:forEach>
                            </select>
                        </p>
                        <p>
                            <label for="rating">Rating from 1 to 5:</label>
                            <input type="number" id="rating" name="rating"
                                   min="1" max="5" required=""/>
                        </p>
                        <p>
                            <label for="comments">Comments:</label><br/>
                            <textarea id="comments" name="comments"
                                      rows="4" cols="50" maxlength="1000"></textarea>
                        </p>
                        <p>
                            <input type="submit" value="ReviewService" name="action"/>
                        </p>
                    </form>
                </c:otherwise>
            </c:choose>

            <%-- PHẦN 2: BẢNG LỊCH SỬ CÁC ĐÁNH GIÁ ĐÃ VIẾT TRƯỚC ĐÓ --%>
            <h3>Your previous reviews</h3>
            <table border="1" cellpadding="5" cellspacing="0">
                <tr>
                    <th>Service</th>
                    <th>Rating</th>
                    <th>Comments</th>
                </tr>
                <c:choose>
                    <c:when test="${empty LIST_REVIEW}">
                        <tr>
                            <td colspan="3">You have not written any review yet</td>
                        </tr>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="review" items="${LIST_REVIEW}">
                            <tr>
                                <td><c:out value="${review.serviceName}"/></td>
                                <td><c:out value="${review.rating}"/></td>
                                <td><c:out value="${review.comments}"/></td>
                            </tr>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </table>

            <p><a href="dashboard.jsp">Back to dashboard</a></p>
        </div>
    </body>
</html>
