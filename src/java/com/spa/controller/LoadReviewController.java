package com.spa.controller;

import com.spa.dao.ReviewDAO;
import com.spa.dto.Review;
import com.spa.dto.Service;
import com.spa.dto.User;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * ============================================================================
 * [VAI TRÒ & CHỨC NĂNG]
 * - Controller ĐỌC DỮ LIỆU trung tâm của nhóm đánh giá dịch vụ (Action: LoadReview - Dành cho Customer).
 * - Được kích hoạt khi Khách hàng bấm "Review a service" trên [dashboard.jsp], hoặc bấm "Write a review"
 *   tại một lịch hẹn đã Completed trên [appointmentList.jsp], hoặc sau khi gửi đánh giá thành công/thất bại.
 * - Chuẩn bị 2 danh sách dữ liệu để đặt vào Request:
 *   1. `LIST_SERVICE`: Danh sách các dịch vụ mà khách ĐỦ ĐIỀU KIỆN ĐÁNH GIÁ (đã hoàn thành & chưa từng đánh giá),
 *      lấy từ `ReviewDAO.getReviewableServices(userID)`.
 *   2. `LIST_REVIEW`: Lịch sử các bài đánh giá mà chính khách hàng đó đã viết, lấy từ `ReviewDAO.getByUser(userID)`.
 * - Forward dữ liệu sang [reviewService.jsp].
 *
 * [LUỒNG THỰC THI (EXECUTION FLOW)]
 * 1. Khách bấm "Review a service" trên [dashboard.jsp] -> MainController (action=LoadReview) -> LoadReviewController.
 * 2. Kiểm tra quyền Customer trong Session (`loginUser.isCustomer()`).
 * 3. Lấy `userID = loginUser.getUserID()`.
 * 4. Gọi `ReviewDAO.getReviewableServices(userID)` -> nhận `List<Service>`.
 * 5. Gọi `ReviewDAO.getByUser(userID)` -> nhận `List<Review>`.
 * 6. Đặt attributes:
 *    - `request.setAttribute("LIST_SERVICE", reviewable)`
 *    - `request.setAttribute("LIST_REVIEW", myReviews)`
 * 7. Forward sang [reviewService.jsp].
 *
 * [LUỒNG DỮ LIỆU (DATA FLOW)]
 * - Dữ liệu vào:
 *   + `session.getAttribute("LOGIN_USER")` -> Lấy `userID` của Customer.
 * - Dữ liệu ra Request:
 *   + `LIST_SERVICE`: `List<Service>` -> Đổ vào thẻ `<select name="serviceID">` trên [reviewService.jsp].
 *   + `LIST_REVIEW`: `List<Review>` -> Đổ vào bảng "Your previous reviews" trên [reviewService.jsp].
 *
 * [LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
 * 1. Sửa tên Request Attribute `"LIST_SERVICE"` hoặc `"LIST_REVIEW"`:
 *    -> BẮT BUỘC phải sửa các thẻ `<c:forEach items="${LIST_SERVICE}">` và `<c:forEach items="${LIST_REVIEW}">` trong [reviewService.jsp].
 * 2. Sửa REVIEW_PAGE = "reviewService.jsp":
 *    -> Đổi file giao diện đánh giá dịch vụ.
 * ============================================================================
 */
public class LoadReviewController extends HttpServlet {

    // [CÁC TRANG ĐÍCH]
    private static final String REVIEW_PAGE = "reviewService.jsp";
    private static final String LOGIN_PAGE = "login.jsp";
    private static final String ERROR_PAGE = "error.jsp";

    protected void processRequest(HttpServletRequest request,
                                  HttpServletResponse response)
            throws ServletException, IOException {
        String url = ERROR_PAGE;
        try {
            // [1. KIỂM TRA PHÂN QUYỀN CUSTOMER TỪ SESSION]:
            HttpSession session = request.getSession(false);
            User loginUser = session == null
                    ? null : (User) session.getAttribute("LOGIN_USER");

            if (loginUser == null || !loginUser.isCustomer()) {
                request.setAttribute("ERROR", "You must log in as a customer");
                url = LOGIN_PAGE;
            } else {
                ReviewDAO dao = new ReviewDAO();

                // [2. LỌC CÁC DỊCH VỤ ĐỦ ĐIỀU KIỆN ĐÁNH GIÁ TỪ CSDL]:
                // (Chỉ những dịch vụ có lịch hẹn Completed và chưa từng được đánh giá)
                List<Service> reviewable =
                        dao.getReviewableServices(loginUser.getUserID());

                // [3. LẤY LỊCH SỬ CÁC ĐÁNH GIÁ CŨ CỦA KHÁCH HÀNG NÀY]:
                List<Review> myReviews = dao.getByUser(loginUser.getUserID());

                // [4. ĐẶT 2 DANH SÁCH VÀO REQUEST ATTRIBUTE ĐỂ HIỂN THỊ TRÊN JSP]:
                request.setAttribute("LIST_SERVICE", reviewable);
                request.setAttribute("LIST_REVIEW", myReviews);
                url = REVIEW_PAGE;
            }
        } catch (Exception e) {
            log("Error at LoadReviewController: " + e.toString());
            request.setAttribute("ERROR", "Could not load the review page");
            url = ERROR_PAGE;
        } finally {
            // [5. CHUYỂN TIẾP SANG REVIEWSVC.JSP]:
            request.getRequestDispatcher(url).forward(request, response);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}
