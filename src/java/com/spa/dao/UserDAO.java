package com.spa.dao;

import com.spa.dto.User;
import com.spa.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * ============================================================================
 * [VAI TRÒ & CHỨC NĂNG]
 * - Lớp DAO (Data Access Object) quản lý toàn bộ thao tác CSDL liên quan tới bảng `tblUsers`.
 * - Cung cấp các hàm kiểm tra đăng nhập (checkLogin), kiểm tra trùng khóa chính (isUserIDExisted),
 *   kiểm tra trùng email (isEmailExisted), và tạo tài khoản mới (insert).
 *
 * [NGUYÊN TẮC THIẾT KẾ & AN TOÀN]
 * 1. Sử dụng PreparedStatement với tham số `?` để ngăn chặn hoàn toàn tấn công SQL Injection.
 * 2. Luôn đóng Connection, PreparedStatement, ResultSet trong khối `finally` theo thứ tự ngược
 *    với khi mở để tránh rò rỉ kết nối (Connection leak).
 * 3. Không nuốt ngoại lệ (không trả về lỗi giả tạo), ném SQLException lên tầng Controller xử lý.
 * 4. Tách biệt hoàn toàn với tầng Web (không phụ thuộc HttpServletRequest, HttpServletResponse).
 *
 * [LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
 * 1. Sửa tên bảng `tblUsers` hoặc các tên cột (userID, fullName, email, phoneNumber, roleID, password):
 *    -> Phải sửa câu lệnh SQL trong file này (CHECK_LOGIN, CHECK_USER_ID, CHECK_EMAIL, INSERT).
 *    -> Phải sửa bảng tblUsers trong [SpaConsultationPortal.sql].
 *    -> Phải sửa DTO [User.java].
 * 2. Sửa phương thức checkLogin(email, password):
 *    -> Ảnh hưởng trực tiếp tới [LoginController.java].
 * 3. Sửa các phương thức isUserIDExisted, isEmailExisted, insert:
 *    -> Ảnh hưởng trực tiếp tới [RegisterController.java].
 * ============================================================================
 */
public class UserDAO {

    // [CÂU LỆNH SQL CHO TỪNG THAO TÁC]
    // Truy vấn xác thực đăng nhập qua email & password
    // [TÁC ĐỘNG]: Sửa ở đây phải đồng bộ với bảng tblUsers trong CSDL và phương thức checkLogin()
    private static final String CHECK_LOGIN =
        "SELECT userID, fullName, email, phoneNumber, roleID "
      + "FROM tblUsers WHERE email = ? AND password = ?";

    // Kiểm tra xem userID đã tồn tại chưa khi đăng ký
    // [TÁC ĐỘNG]: Sửa ở đây phải đồng bộ với isUserIDExisted() và RegisterController
    private static final String CHECK_USER_ID =
        "SELECT userID FROM tblUsers WHERE userID = ?";

    // Kiểm tra xem email đã tồn tại chưa khi đăng ký (vì cột email có UNIQUE constraint)
    // [TÁC ĐỘNG]: Sửa ở đây phải đồng bộ với isEmailExisted() và RegisterController
    private static final String CHECK_EMAIL =
        "SELECT userID FROM tblUsers WHERE email = ?";

    // Thêm người dùng mới vào bảng tblUsers
    // [TÁC ĐỘNG]: Thứ tự 6 tham số (1:userID, 2:fullName, 3:email, 4:phoneNumber, 5:roleID, 6:password)
    //             phải khớp tuyệt đối với ptm.setString(...) trong phương thức insert()
    private static final String INSERT =
        "INSERT INTO tblUsers "
      + "(userID, fullName, email, phoneNumber, roleID, password) "
      + "VALUES (?, ?, ?, ?, ?, ?)";

    /**
     * Xác thực người dùng qua Email và Mật khẩu.
     *
     * [LUỒNG THỰC THI & DỮ LIỆU]:
     * - Được gọi từ: [LoginController.java] (action=Login).
     * - Input: `email` (từ request.getParameter("email")), `password` (từ request.getParameter("password")).
     * - Output: 
     *   + Trả về đối tượng User (chứa userID, fullName, email, phoneNumber, roleID) nếu khớp.
     *   + Trả về null nếu sai email hoặc sai mật khẩu.
     *
     * [TÁC ĐỘNG KHI SỬA]:
     * - Nếu đổi kiểu trả về hoặc tham số -> Phải sửa LoginController.java dòng gọi dao.checkLogin(...).
     */
    public User checkLogin(String email, String password)
            throws SQLException, ClassNotFoundException {
        User user = null;
        Connection conn = null;
        PreparedStatement ptm = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn != null) {
                ptm = conn.prepareStatement(CHECK_LOGIN);
                ptm.setString(1, email);
                ptm.setString(2, password);
                rs = ptm.executeQuery();
                if (rs.next()) {
                    user = new User(
                            rs.getString("userID"),
                            rs.getString("fullName"),
                            rs.getString("email"),
                            rs.getString("phoneNumber"),
                            rs.getString("roleID"));
                }
            }
        } finally {
            if (rs != null) { rs.close(); }
            if (ptm != null) { ptm.close(); }
            if (conn != null) { conn.close(); }
        }
        return user;
    }

    /**
     * Kiểm tra mã người dùng (userID) đã có trong CSDL hay chưa.
     *
     * [LUỒNG THỰC THI & DỮ LIỆU]:
     * - Được gọi từ: [RegisterController.java] (action=Register).
     * - Input: `userID` (từ ô nhập User ID trên form register.jsp).
     * - Output: `true` nếu đã tồn tại (báo lỗi trùng), `false` nếu chưa có (hợp lệ).
     *
     * [TÁC ĐỘNG KHI SỬA]:
     * - Phải đồng bộ với khối kiểm tra trong RegisterController.java.
     */
    public boolean isUserIDExisted(String userID)
            throws SQLException, ClassNotFoundException {
        boolean existed = false;
        Connection conn = null;
        PreparedStatement ptm = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn != null) {
                ptm = conn.prepareStatement(CHECK_USER_ID);
                ptm.setString(1, userID);
                rs = ptm.executeQuery();
                existed = rs.next();
            }
        } finally {
            if (rs != null) { rs.close(); }
            if (ptm != null) { ptm.close(); }
            if (conn != null) { conn.close(); }
        }
        return existed;
    }

    /**
     * Kiểm tra Email đã được đăng ký tài khoản nào chưa.
     *
     * [LUỒNG THỰC THI & DỮ LIỆU]:
     * - Được gọi từ: [RegisterController.java] (action=Register).
     * - Input: `email` (từ ô nhập Email trên form register.jsp).
     * - Output: `true` nếu đã tồn tại, `false` nếu chưa có.
     *
     * [TÁC ĐỘNG KHI SỬA]:
     * - Phải đồng bộ với khối kiểm tra email trong RegisterController.java.
     */
    public boolean isEmailExisted(String email)
            throws SQLException, ClassNotFoundException {
        boolean existed = false;
        Connection conn = null;
        PreparedStatement ptm = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn != null) {
                ptm = conn.prepareStatement(CHECK_EMAIL);
                ptm.setString(1, email);
                rs = ptm.executeQuery();
                existed = rs.next();
            }
        } finally {
            if (rs != null) { rs.close(); }
            if (ptm != null) { ptm.close(); }
            if (conn != null) { conn.close(); }
        }
        return existed;
    }

    /**
     * Thêm mới một người dùng (Khách hàng) vào CSDL.
     *
     * [LUỒNG THỰC THI & DỮ LIỆU]:
     * - Được gọi từ: [RegisterController.java] (action=Register).
     * - Input: Đối tượng `User` (chứa userID, fullName, email, phoneNumber, roleID="USR") và chuỗi `password`.
     * - Output: `true` nếu thêm thành công vào tblUsers, `false` nếu thất bại.
     *
     * [TÁC ĐỘNG KHI SỬA]:
     * - Nếu sửa thứ tự các cột INSERT: Phải cập nhật đúng chỉ số `ptm.setString(index, value)`.
     * - Sửa phương thức này ảnh hưởng trực tiếp tới kết quả tạo tài khoản ở RegisterController.java.
     */
    public boolean insert(User user, String password)
            throws SQLException, ClassNotFoundException {
        boolean inserted = false;
        Connection conn = null;
        PreparedStatement ptm = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn != null) {
                ptm = conn.prepareStatement(INSERT);
                ptm.setString(1, user.getUserID());
                ptm.setString(2, user.getFullName());
                ptm.setString(3, user.getEmail());
                ptm.setString(4, user.getPhoneNumber());
                ptm.setString(5, user.getRoleID());
                ptm.setString(6, password);
                inserted = ptm.executeUpdate() > 0;
            }
        } finally {
            if (ptm != null) { ptm.close(); }
            if (conn != null) { conn.close(); }
        }
        return inserted;
    }
}
