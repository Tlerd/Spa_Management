package com.spa.dao;

import com.spa.dto.Review;
import com.spa.dto.Service;
import com.spa.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * [VAI TRÒ & CHỨC NĂNG]
 * - Lớp DAO (Data Access Object) quản lý toàn bộ thao tác CSDL liên quan tới bảng `tblReviews`.
 * - Thực hiện nghiệp vụ đánh giá dịch vụ:
 *   1. getReviewableServices: Lọc các dịch vụ mà khách hàng đã hoàn thành (Completed) nhưng CHƯA đánh giá.
 *   2. getByUser: Lấy lịch sử các đánh giá mà khách hàng đã viết.
 *   3. insert: Lưu đánh giá mới vào bảng tblReviews.
 *   4. getNextID: Tự động sinh mã đánh giá tiếp theo ('rev1' -> 'rev2'...).
 *
 * [NGHIỆP VỤ CỐT LÕI - CÂU LỆNH SQL GET_REVIEWABLE]
 * - Ràng buộc: "Khách hàng chỉ được đánh giá dịch vụ sau khi đã có ít nhất 1 lịch hẹn ở trạng thái 'Completed',
 *   và mỗi dịch vụ chỉ được đánh giá DUY NHẤT 1 LẦN".
 * - Xử lý trong 1 câu SQL duy nhất:
 *   + INNER JOIN tblAppointments với tblServices.
 *   + Lọc theo userID và status = 'Completed'.
 *   + Dùng NOT EXISTS (SELECT 1 FROM tblReviews ...) để loại trừ dịch vụ đã từng đánh giá.
 *   + Dùng DISTINCT để tránh trùng lặp nếu khách đã đặt và hoàn thành dịch vụ đó nhiều lần.
 *
 * [LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
 * 1. Sửa câu lệnh GET_REVIEWABLE:
 *    -> Ảnh hưởng trực tiếp tới danh sách dịch vụ đổ vào thẻ <select> trên [reviewService.jsp].
 *    -> Ảnh hưởng tới hàm kiểm tra hợp lệ `isReviewable()` trong [ReviewServiceController.java].
 * 2. Sửa câu lệnh GET_BY_USER:
 *    -> Ảnh hưởng tới bảng "Your previous reviews" trên [reviewService.jsp].
 * 3. Sửa phương thức insert(review):
 *    -> Ảnh hưởng tới [ReviewServiceController.java].
 * 4. Sửa ràng buộc rating (1-5 sao) hoặc bình luận comments:
 *    -> Phải đồng bộ với bảng tblReviews trong [SpaConsultationPortal.sql].
 * ============================================================================
 */
public class ReviewDAO {

    // [CÂU TRUY VẤN LỌC DỊCH VỤ ĐỦ ĐIỀU KIỆN ĐÁNH GIÁ]
    // [TÁC ĐỘNG]: Rất quan trọng! Sửa ở đây ảnh hưởng tới LoadReviewController,
    //             ReviewServiceController và dropdown trên reviewService.jsp
    private static final String GET_REVIEWABLE =
        "SELECT DISTINCT s.serviceID, s.serviceName, s.description, s.price "
      + "FROM tblAppointments a "
      + "     INNER JOIN tblServices s ON a.serviceID = s.serviceID "
      + "WHERE a.userID = ? "
      + "  AND a.status = 'Completed' "
      + "  AND NOT EXISTS (SELECT 1 FROM tblReviews r "
      + "                  WHERE r.userID = a.userID "
      + "                    AND r.serviceID = a.serviceID) "
      + "ORDER BY s.serviceID";

    // Lấy toàn bộ đánh giá của 1 người dùng kèm tên dịch vụ
    // [TÁC ĐỘNG]: Khớp với getByUser() -> LoadReviewController -> reviewService.jsp
    private static final String GET_BY_USER =
        "SELECT r.reviewID, r.userID, r.serviceID, r.rating, r.comments, "
      + "       s.serviceName, u.fullName AS userName "
      + "FROM tblReviews r "
      + "     INNER JOIN tblServices s ON r.serviceID = s.serviceID "
      + "     INNER JOIN tblUsers    u ON r.userID    = u.userID "
      + "WHERE r.userID = ? ORDER BY r.reviewID";

    // Thêm đánh giá mới vào bảng tblReviews
    // [TÁC ĐỘNG]: Thứ tự tham số: 1:reviewID, 2:userID, 3:serviceID, 4:rating, 5:comments
    private static final String INSERT =
        "INSERT INTO tblReviews (reviewID, userID, serviceID, rating, comments) "
      + "VALUES (?, ?, ?, ?, ?)";

    // Lấy mã ID lớn nhất có tiền tố 'rev' để sinh mã tự động tiếp theo
    // [TÁC ĐỘNG]: Khớp với getNextID() -> ReviewServiceController
    private static final String GET_MAX_ID =
        "SELECT TOP 1 reviewID FROM tblReviews "
      + "WHERE reviewID LIKE 'rev%' "
      + "  AND ISNUMERIC(SUBSTRING(reviewID, 4, 50)) = 1 "
      + "ORDER BY CAST(SUBSTRING(reviewID, 4, 50) AS INT) DESC";

    /**
     * Lấy danh sách các dịch vụ mà người dùng ĐỦ ĐIỀU KIỆN ĐÁNH GIÁ.
     * (Đã từng hoàn thành dịch vụ và chưa từng đánh giá dịch vụ đó).
     *
     * [LUỒNG THỰC THI & DỮ LIỆU]:
     * - Được gọi từ:
     *   1. [LoadReviewController.java] để nạp danh sách cho dropdown chọn dịch vụ.
     *   2. [ReviewServiceController.java] (qua hàm isReviewable) để kiểm tra tính hợp lệ trước khi lưu.
     * - Input: `userID` lấy từ Session (`loginUser.getUserID()`).
     * - Output: `List<Service>` (danh sách dịch vụ hợp lệ).
     * - Dữ liệu tiếp tục đi tới: [reviewService.jsp] qua request attribute `LIST_SERVICE`.
     */
    public List<Service> getReviewableServices(String userID)
            throws SQLException, ClassNotFoundException {
        List<Service> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ptm = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn != null) {
                ptm = conn.prepareStatement(GET_REVIEWABLE);
                ptm.setString(1, userID);
                rs = ptm.executeQuery();
                while (rs.next()) {
                    list.add(new Service(
                            rs.getString("serviceID"),
                            rs.getString("serviceName"),
                            rs.getString("description"),
                            rs.getBigDecimal("price")));
                }
            }
        } finally {
            if (rs != null) { rs.close(); }
            if (ptm != null) { ptm.close(); }
            if (conn != null) { conn.close(); }
        }
        return list;
    }

    /**
     * Lấy danh sách lịch sử đánh giá do chính người dùng đó đã gửi.
     *
     * [LUỒNG THỰC THI & DỮ LIỆU]:
     * - Được gọi từ: [LoadReviewController.java] (action=LoadReview).
     * - Input: `userID` lấy từ Session (`loginUser.getUserID()`).
     * - Output: `List<Review>` (chứa reviewID, serviceName, rating, comments).
     * - Dữ liệu tiếp tục đi tới: [reviewService.jsp] qua request attribute `LIST_REVIEW`.
     */
    public List<Review> getByUser(String userID)
            throws SQLException, ClassNotFoundException {
        List<Review> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ptm = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn != null) {
                ptm = conn.prepareStatement(GET_BY_USER);
                ptm.setString(1, userID);
                rs = ptm.executeQuery();
                while (rs.next()) {
                    Review review = new Review(
                            rs.getString("reviewID"),
                            rs.getString("userID"),
                            rs.getString("serviceID"),
                            rs.getInt("rating"),
                            rs.getString("comments"));
                    review.setServiceName(rs.getString("serviceName"));
                    review.setUserName(rs.getString("userName"));
                    list.add(review);
                }
            }
        } finally {
            if (rs != null) { rs.close(); }
            if (ptm != null) { ptm.close(); }
            if (conn != null) { conn.close(); }
        }
        return list;
    }

    /**
     * Thêm một đánh giá mới vào CSDL.
     *
     * [LUỒNG THỰC THI & DỮ LIỆU]:
     * - Được gọi từ: [ReviewServiceController.java] (action=ReviewService).
     * - Input: Đối tượng `Review` (chứa reviewID, userID, serviceID, rating, comments).
     * - Output: `true` nếu thêm thành công, `false` nếu thất bại.
     */
    public boolean insert(Review review)
            throws SQLException, ClassNotFoundException {
        boolean inserted = false;
        Connection conn = null;
        PreparedStatement ptm = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn != null) {
                ptm = conn.prepareStatement(INSERT);
                ptm.setString(1, review.getReviewID());
                ptm.setString(2, review.getUserID());
                ptm.setString(3, review.getServiceID());
                ptm.setInt(4, review.getRating());
                if (review.getComments() == null
                        || review.getComments().trim().isEmpty()) {
                    ptm.setNull(5, Types.NVARCHAR);
                } else {
                    ptm.setString(5, review.getComments().trim());
                }
                inserted = ptm.executeUpdate() > 0;
            }
        } finally {
            if (ptm != null) { ptm.close(); }
            if (conn != null) { conn.close(); }
        }
        return inserted;
    }

    /**
     * Tự động sinh mã đánh giá tiếp theo (ví dụ 'rev4' -> 'rev5').
     *
     * [LUỒNG THỰC THI & DỮ LIỆU]:
     * - Được gọi từ: [ReviewServiceController.java].
     * - Output: Chuỗi mã mới (ví dụ 'rev1', 'rev5').
     */
    public String getNextID()
            throws SQLException, ClassNotFoundException {
        int next = 1;
        Connection conn = null;
        PreparedStatement ptm = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn != null) {
                ptm = conn.prepareStatement(GET_MAX_ID);
                rs = ptm.executeQuery();
                if (rs.next()) {
                    String maxID = rs.getString("reviewID");
                    try {
                        next = Integer.parseInt(maxID.substring(3)) + 1;
                    } catch (NumberFormatException ignored) {
                        next = 1;
                    }
                }
            }
        } finally {
            if (rs != null) { rs.close(); }
            if (ptm != null) { ptm.close(); }
            if (conn != null) { conn.close(); }
        }
        return "rev" + next;
    }
}
