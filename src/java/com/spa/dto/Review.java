package com.spa.dto;

/**
 * ============================================================================
 * [VAI TRÒ & CHỨC NĂNG]
 * - Lớp DTO (Data Transfer Object) đại diện cho Đánh Giá Dịch Vụ (tblReviews).
 * - Đóng gói dữ liệu về một bài đánh giá của khách hàng (mã đánh giá, người đánh giá,
 *   dịch vụ, số sao từ 1-5, bình luận).
 * - Chứa thêm 2 trường mở rộng (userName, serviceName) lấy từ phép JOIN bảng để hiển thị.
 *
 * [LUỒNG DỮ LIỆU (DATA FLOW)]
 * 1. Từ CSDL (tblReviews JOIN tblServices, tblUsers):
 *    - ReviewDAO.getByUser(userID) -> List<Review> -> LoadReviewController -> [reviewService.jsp] (${LIST_REVIEW})
 * 2. Từ Người dùng gửi đánh giá:
 *    - [reviewService.jsp] -> ReviewServiceController -> new Review(...) -> ReviewDAO.insert() -> tblReviews
 *
 * [LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
 * 1. Sửa các thuộc tính gốc (reviewID, userID, serviceID, rating, comments):
 *    -> Phải sửa tương ứng các cột trong bảng tblReviews ở [SpaConsultationPortal.sql].
 *    -> Phải sửa câu lệnh SQL (GET_BY_USER, INSERT) trong [ReviewDAO.java].
 *    -> Phải sửa xử lý tham số trong [ReviewServiceController.java].
 * 2. Sửa các trường mở rộng (userName, serviceName):
 *    -> Phải sửa câu truy vấn GET_BY_USER và việc set trường trong ReviewDAO.
 *    -> Phải sửa hiển thị trên bảng lịch sử đánh giá trong [reviewService.jsp] (${review.serviceName}).
 * 3. Sửa giới hạn rating (1 đến 5 sao):
 *    -> Phải đồng bộ với:
 *       + Ràng buộc CSDL: CK_Rev_Rating CHECK (rating BETWEEN 1 AND 5) trong [SpaConsultationPortal.sql].
 *       + Validate trong [ReviewServiceController.java] (ratingValue < 1 || ratingValue > 5).
 *       + Thuộc tính min="1" max="5" trong [reviewService.jsp].
 * ============================================================================
 */
public class Review {

    // [CÁC CỘT TRONG BẢNG tblReviews]
    // [TÁC ĐỘNG]: Khóa chính trong bảng tblReviews VARCHAR(50) (ví dụ: 'rev1', 'rev2')
    private String reviewID;

    // [TÁC ĐỘNG]: Khóa ngoại tham chiếu tới tblUsers(userID)
    private String userID;

    // [TÁC ĐỘNG]: Khóa ngoại tham chiếu tới tblServices(serviceID)
    private String serviceID;

    // [TÁC ĐỘNG]: Điểm số đánh giá từ 1 đến 5 (INT)
    private int rating;

    // [TÁC ĐỘNG]: Bình luận của khách hàng NVARCHAR(1000) (có thể NULL)
    private String comments;

    // [CÁC TRƯỜNG MỞ RỘNG - LẤY TỪ PHÉP JOIN ĐỂ HIỂN THỊ LÊN JSP]
    // [TÁC ĐỘNG]: Lấy từ tblUsers.fullName của khách hàng (u.fullName AS userName)
    private String userName;

    // [TÁC ĐỘNG]: Lấy từ tblServices.serviceName (s.serviceName)
    private String serviceName;

    public Review() {
    }

    public Review(String reviewID, String userID, String serviceID,
                  int rating, String comments) {
        this.reviewID = reviewID;
        this.userID = userID;
        this.serviceID = serviceID;
        this.rating = rating;
        this.comments = comments;
    }

    public String getReviewID() { return reviewID; }
    public void setReviewID(String reviewID) { this.reviewID = reviewID; }

    public String getUserID() { return userID; }
    public void setUserID(String userID) { this.userID = userID; }

    public String getServiceID() { return serviceID; }
    public void setServiceID(String serviceID) {
        this.serviceID = serviceID;
    }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
}
