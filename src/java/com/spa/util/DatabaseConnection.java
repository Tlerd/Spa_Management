package com.spa.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * ============================================================================
 * [VAI TRÒ & CHỨC NĂNG]
 * - Lớp tiện ích DatabaseConnection: Nơi duy nhất trong toàn bộ ứng dụng quản lý
 *   và khởi tạo kết nối JDBC tới cơ sở dữ liệu Microsoft SQL Server.
 * 
 * [LUỒNG THỰC THI (EXECUTION FLOW)]
 * 1. Các lớp DAO (UserDAO, ServiceDAO, AppointmentDAO, ReviewDAO) gọi:
 *    Connection conn = DatabaseConnection.getConnection();
 * 2. Nạp JDBC Driver "com.microsoft.sqlserver.jdbc.SQLServerDriver".
 * 3. DriverManager mở kết nối TCP tới SQL Server (localhost:1433) với database SpaConsultationPortal.
 * 4. Trả về đối tượng Connection cho DAO thực hiện PreparedStatement và đóng kết nối ở finally block.
 *
 * [LUỒNG DỮ LIỆU (DATA FLOW)]
 * - Input: Thông tin cấu hình tĩnh (SERVER, PORT, DB_NAME, USER_NAME, PASSWORD).
 * - Output: Đối tượng java.sql.Connection đang mở tới CSDL.
 *
 * [LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
 * 1. Sửa DB_NAME ("SpaConsultationPortal"):
 *    -> Phải đồng bộ với tên Database trong tệp CSDL: [SpaConsultationPortal.sql].
 * 2. Sửa USER_NAME ("sa") hoặc PASSWORD ("12345"):
 *    -> Phải khớp với tài khoản SQL Server trên máy tính đang chạy.
 * 3. Sửa PORT ("1433") hoặc SERVER ("localhost"):
 *    -> Phải khớp với cấu hình SQL Server Configuration Manager (TCP/IP Port).
 * 4. Nếu phương thức getConnection() ném ngoại lệ (sai pass, server tắt, thiếu driver):
 *    -> Mọi DAO sẽ quăng SQLException/ClassNotFoundException lên các Controller,
 *       các Controller sẽ bắt được (catch) và chuyển tiếp tới [error.jsp] kèm log lỗi.
 * ============================================================================
 */
public class DatabaseConnection {

    // [CẤU HÌNH KẾT NỐI]
    // Sửa các hằng số này sẽ ảnh hưởng tới toàn bộ 4 DAO: UserDAO, ServiceDAO, AppointmentDAO, ReviewDAO
    private static final String SERVER = "localhost";
    private static final String PORT = "1433";
    private static final String DB_NAME = "SpaConsultationPortal"; // Khớp với Database trong SpaConsultationPortal.sql
    private static final String USER_NAME = "sa";                  // Tài khoản SQL Server
    private static final String PASSWORD = "12345";                // Mật khẩu SQL Server

    /**
     * Khởi tạo kết nối CSDL SQL Server qua JDBC Driver.
     * 
     * @return java.sql.Connection
     * @throws ClassNotFoundException nếu thiếu thư viện sqljdbc4.jar trong WEB-INF/lib
     * @throws SQLException nếu thông tin kết nối sai hoặc SQL Server không chạy
     * 
     * [DỮ LIỆU ĐI TỚI ĐÂU]:
     * - Được gọi bởi:
     *   + UserDAO (checkLogin, isUserIDExisted, isEmailExisted, insert)
     *   + ServiceDAO (search, getByID, isServiceIDExisted, insert, update, delete)
     *   + AppointmentDAO (queryList, insert, updateStatus, updateNotes, getNextID)
     *   + ReviewDAO (getReviewableServices, getByUser, insert, getNextID)
     */
    public static Connection getConnection()
            throws ClassNotFoundException, SQLException {
        // Nạp Driver từ thư viện sqljdbc4.jar (nằm trong web/WEB-INF/lib/ và lib/)
        // Nếu sửa dòng này sai tên class -> ClassNotFoundException xảy ra ở tất cả các DAO.
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

        // Chuỗi kết nối JDBC URL
        String url = "jdbc:sqlserver://" + SERVER + ":" + PORT
                   + ";databaseName=" + DB_NAME;

        return DriverManager.getConnection(url, USER_NAME, PASSWORD);
    }
}
