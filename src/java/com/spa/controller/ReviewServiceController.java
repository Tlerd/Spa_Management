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
 * - Xử lý chức năng GỬI ĐÁNH GIÁ DỊCH VỤ (Action: ReviewService - Dành cho Customer).
 * - Nhận thông tin từ form [reviewService.jsp]: `serviceID`, `rating` (số sao 1-5), `comments` (bình luận).
 * - Lấy `userID` trực tiếp từ Session (`loginUser.getUserID()`).
 * - Tự động sinh mã đánh giá qua `dao.getNextID()` ('rev1', 'rev2'...).
 * - KIỂM TRA RÀNG BUỘC NGHIỆP VỤ & BẢO MẬT PHÍA SERVER:
 *   1. rating: Bắt buộc là số nguyên từ 1 đến 5 (`ratingValue >= 1 && ratingValue <= 5`).
 *   2. comments: Độ dài <= 1000 ký tự (nếu có).
 *   3. HÀM KIỂM TRA `isReviewable()`:
 *      Không chỉ tin vào dropdown HTML (vì hacker có thể sửa mã nguồn trang và gửi bất kỳ serviceID nào),
 *      Controller gọi lại `dao.getReviewableServices(userID)` để kiểm tra xem dịch vụ đó có THỰC SỰ
 *      nằm trong danh sách được phép đánh giá hay không.
 * - Khi thành công: Đặt thông báo thành công `MESSAGE` và chuyển tiếp về `LoadReviewController`
 *   để tải lại dropdown (dịch vụ vừa đánh giá sẽ biến mất khỏi dropdown) và cập nhật bảng lịch sử.
 * - Khi có lỗi: Đặt thông báo lỗi `ERROR` và chuyển tiếp về `LoadReviewController`.
 *
 * [LUỒNG THỰC THI (EXECUTION FLOW)]
 * 1. Form [reviewService.jsp] submit POST -> MainController (action=ReviewService) -> ReviewServiceController.
 * 2. Kiểm tra quyền Customer trong Session (`loginUser.isCustomer()`).
 * 3. Đọc các tham số: `serviceID`, `rating`, `comments`.
 * 4. Parse & validate rating (1-5), comments (<= 1000).
 * 5. Gọi hàm `isReviewable(dao, userID, serviceID)`:
 *    - Nếu không hợp lệ -> Set ERROR "You can only review a service after one of your appointments for it is completed, and only once".
 * 6. Nếu hợp lệ:
 *    - Tạo đối tượng `Review(dao.getNextID(), userID, serviceID, ratingValue, comments)`.
 *    - Gọi `dao.insert(review)`.
 *    - Nếu insert thành công -> `request.setAttribute("MESSAGE", "Thank you, your review has been saved")`.
 * 7. Forward về `LoadReviewController` để nạp lại dữ liệu trang đánh giá.
 *
 * [LUỒNG DỮ LIỆU (DATA FLOW)]
 * - Dữ liệu vào:
 *   + `session.getAttribute("LOGIN_USER")` -> UserID của khách hàng.
 *   + `request.getParameter("serviceID")` -> Mã dịch vụ được đánh giá.
 *   + `request.getParameter("rating")` -> Điểm số 1-5 sao.
 *   + `request.getParameter("comments")` -> Bình luận cảm nhận.
 * - Dữ liệu xuống DB:
 *   + INSERT INTO tblReviews (reviewID, userID, serviceID, rating, comments).
 * - Dữ liệu ra Request:
 *   + Thành công: `request.setAttribute("MESSAGE", ...)` -> [LoadReviewController] -> [reviewService.jsp].
 *   + Thất bại: `request.setAttribute("ERROR", error)` -> [LoadReviewController] -> [reviewService.jsp].
 *
 * [LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
 * 1. Sửa giới hạn rating (1-5 sao):
 *    -> Phải đồng bộ với CK_Rev_Rating CHECK (rating BETWEEN 1 AND 5) trong [SpaConsultationPortal.sql]
 *       và thuộc tính min="1" max="5" trên [reviewService.jsp].
 * 2. Sửa LOAD_REVIEW = "LoadReviewController":
 *    -> Sau khi lưu đánh giá, luôn quay về LoadReviewController để cập nhật lại danh sách dịch vụ có thể đánh giá và bảng lịch sử.
 * ============================================================================
 */
public class ReviewServiceController extends HttpServlet {

    // [CÁC TRANG ĐÍCH]
    private static final String LOAD_REVIEW = "LoadReviewController";
    private static final String LOGIN_PAGE = "login.jsp";

    protected void processRequest(HttpServletRequest request,
                                  HttpServletResponse response)
            throws ServletException, IOException {
        String url = LOAD_REVIEW;
        try {
            // [1. KIỂM TRA PHÂN QUYỀN CUSTOMER VÀ LẤY THÔNG TIN ĐĂNG NHẬP TỪ SESSION]:
            HttpSession session = request.getSession(false);
            User loginUser = session == null
                    ? null : (User) session.getAttribute("LOGIN_USER");

            if (loginUser == null || !loginUser.isCustomer()) {
                request.setAttribute("ERROR", "You must log in as a customer");
                url = LOGIN_PAGE;
            } else {
                // [2. ĐỌC CÁC THAM SỐ GỬI LÊN TỪ FORM ĐÁNH GIÁ]:
                String serviceID = request.getParameter("serviceID");
                String rating = request.getParameter("rating");
                String comments = request.getParameter("comments");

                String error = null;
                int ratingValue = 0;

                // [3. KIỂM TRA HỢP LỆ VÀ ÉP KIỂU ĐIỂM SỐ RATING]:
                if (serviceID == null || serviceID.trim().isEmpty()) {
                    error = "Please select a service";
                } else if (rating == null || rating.trim().isEmpty()) {
                    error = "Please give a rating";
                } else {
                    try {
                        ratingValue = Integer.parseInt(rating.trim());
                        if (ratingValue < 1 || ratingValue > 5) {
                            error = "Rating must be a whole number from 1 to 5";
                        }
                    } catch (NumberFormatException nfe) {
                        error = "Rating must be a whole number from 1 to 5";
                    }
                }

                // [4. KIỂM TRA ĐỘ DÀI BÌNH LUẬN COMMENTS]:
                if (error == null && comments != null
                        && comments.trim().length() > 1000) {
                    error = "Comments must not exceed 1000 characters";
                }

                ReviewDAO dao = new ReviewDAO();

                // [5. BẢO MẬT: KIỂM TRA LẠI VỚI CSDL XEM DỊCH VỤ CÓ THỰC SỰ ĐỦ ĐIỀU KIỆN ĐÁNH GIÁ]:
                if (error == null
                        && !isReviewable(dao, loginUser.getUserID(),
                                         serviceID.trim())) {
                    error = "You can only review a service after one of your"
                          + " appointments for it is completed, and only once";
                }

                // [6. TẠO ĐỐI TƯỢNG VÀ THỰC HIỆN THÊM VÀO BẢNG TBLREVIEWS]:
                if (error == null) {
                    Review review = new Review(dao.getNextID(),
                            loginUser.getUserID(), serviceID.trim(),
                            ratingValue, comments);
                    if (dao.insert(review)) {
                        // Đánh giá thành công -> Chuyển về LoadReviewController để tải lại danh sách mới
                        request.setAttribute("MESSAGE",
                                "Thank you, your review has been saved");
                    } else {
                        error = "Could not save the review, please try again";
                    }
                }

                if (error != null) {
                    request.setAttribute("ERROR", error);
                }
            }
        } catch (Exception e) {
            log("Error at ReviewServiceController: " + e.toString());
            request.setAttribute("ERROR", "Could not save the review");
            url = LOAD_REVIEW;
        } finally {
            // [7. CHUYỂN TIẾP VỀ LOADREVIEWCONTROLLER ĐỂ TẢI LẠI TRANG REVIEWSVC.JSP]:
            request.getRequestDispatcher(url).forward(request, response);
        }
    }

    /**
     * Xác thực với CSDL xem khách hàng có thực sự đủ điều kiện đánh giá dịch vụ này không.
     * [DỮ LIỆU ĐI TỚI ĐÂU]:
     * - Gọi ReviewDAO.getReviewableServices(userID) và duyệt danh sách.
     */
    private boolean isReviewable(ReviewDAO dao, String userID, String serviceID)
            throws Exception {
        List<Service> reviewable = dao.getReviewableServices(userID);
        for (Service service : reviewable) {
            if (service.getServiceID().equals(serviceID)) {
                return true;
            }
        }
        return false;
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
