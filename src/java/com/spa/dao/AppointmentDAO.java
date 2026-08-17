package com.spa.dao;

import com.spa.dto.Appointment;
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
 * - Lớp DAO (Data Access Object) quản lý toàn bộ thao tác CSDL liên quan tới bảng `tblAppointments`.
 * - Cung cấp các thao tác: Lấy danh sách lịch hẹn của khách (getByUser), lịch hẹn của nhân viên (getByStaff),
 *   lấy 1 lịch hẹn theo ID (getByID), đặt lịch mới (insert), cập nhật trạng thái (updateStatus),
 *   lưu ghi chú tư vấn (updateNotes), và tự động sinh mã lịch hẹn kế tiếp (getNextID).
 *
 * [NGUYÊN TẮC THIẾT KẾ & TỐI ƯU TRUY VẤN]
 * 1. Phép JOIN bảng: SELECT_COLUMNS kết hợp INNER JOIN với tblServices (lấy serviceName),
 *    tblUsers (lấy tên khách - userName) và LEFT JOIN với tblUsers (lấy tên nhân viên - staffName).
 *    -> Dùng LEFT JOIN cho nhân viên vì lịch mới tạo có `staffID = NULL`. Nếu dùng INNER JOIN
 *       thì các lịch chưa được phân công nhân viên sẽ bị mất khỏi danh sách.
 * 2. Xử lý giá trị NULL: Khi `staffID` hoặc `consultationNotes` là null, bắt buộc dùng
 *    `ptm.setNull(index, Types.VARCHAR / Types.NVARCHAR)` thay vì `ptm.setString(index, null)`
 *    để tương thích tuyệt đối với JDBC driver của SQL Server.
 * 3. Tự sinh mã ID (getNextID): Tìm mã lớn nhất có dạng 'appN' và tăng lên 1 (ví dụ 'app8' -> 'app9').
 *
 * [LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
 * 1. Sửa bảng `tblAppointments` hoặc các cột:
 *    -> Phải cập nhật các hằng số SQL (SELECT_COLUMNS, INSERT, UPDATE_STATUS, UPDATE_NOTES, GET_MAX_ID).
 *    -> Phải cập nhật DTO [Appointment.java].
 * 2. Sửa phương thức `getByUser(userID)`:
 *    -> Ảnh hưởng tới [ViewAppointmentsController.java] -> [appointmentList.jsp].
 * 3. Sửa phương thức `getByStaff(staffID)`:
 *    -> Ảnh hưởng tới [ViewConsultationController.java] -> [consultation.jsp].
 * 4. Sửa phương thức `getByID(appointmentID)`:
 *    -> Ảnh hưởng tới kiểm tra quyền sở hữu và trạng thái trong:
 *       [CancelAppointmentController.java], [SaveNotesController.java], [CompleteAppointmentController.java].
 * 5. Sửa phương thức `insert(app)` hoặc `getNextID()`:
 *    -> Ảnh hưởng tới [BookAppointmentController.java].
 * 6. Sửa phương thức `updateStatus(id, status)`:
 *    -> Ảnh hưởng tới [CancelAppointmentController.java] (status='Cancelled')
 *       và [CompleteAppointmentController.java] (status='Completed').
 * 7. Sửa phương thức `updateNotes(id, notes)`:
 *    -> Ảnh hưởng tới [SaveNotesController.java].
 * ============================================================================
 */
public class AppointmentDAO {

    // [CÂU LỆNH SQL CỐT LÕI VỚI PHÉP JOIN]
    // INNER JOIN tblServices (lấy tên dịch vụ)
    // INNER JOIN tblUsers (lấy tên khách hàng u.fullName)
    // LEFT JOIN tblUsers (lấy tên nhân viên st.fullName, cho phép staffID là NULL)
    // [TÁC ĐỘNG]: Sửa ở đây ảnh hưởng tới getByUser, getByStaff, getByID và hiển thị trên JSP
    private static final String SELECT_COLUMNS =
        "SELECT a.appointmentID, a.userID, a.serviceID, a.appointmentDate, "
      + "       a.status, a.staffID, a.consultationNotes, "
      + "       s.serviceName, u.fullName AS userName, "
      + "       st.fullName AS staffName "
      + "FROM tblAppointments a "
      + "     INNER JOIN tblServices s ON a.serviceID = s.serviceID "
      + "     INNER JOIN tblUsers    u ON a.userID    = u.userID "
      + "     LEFT  JOIN tblUsers    st ON a.staffID  = st.userID ";

    // Lấy danh sách lịch hẹn của một khách hàng cụ thể
    // [TÁC ĐỘNG]: Khớp với getByUser() -> ViewAppointmentsController
    private static final String GET_BY_USER =
        SELECT_COLUMNS + "WHERE a.userID = ? ORDER BY a.appointmentDate DESC";

    // Lấy danh sách lịch hẹn phân công cho một nhân viên cụ thể
    // [TÁC ĐỘNG]: Khớp với getByStaff() -> ViewConsultationController
    private static final String GET_BY_STAFF =
        SELECT_COLUMNS + "WHERE a.staffID = ? ORDER BY a.appointmentDate DESC";

    // Lấy thông tin 1 lịch hẹn theo mã khóa chính
    // [TÁC ĐỘNG]: Khớp với getByID() -> Cancel, Complete, SaveNotes Controller
    private static final String GET_BY_ID =
        SELECT_COLUMNS + "WHERE a.appointmentID = ?";

    // Thêm lịch hẹn mới vào bảng tblAppointments
    // [TÁC ĐỘNG]: Thứ tự 7 tham số: 1:appointmentID, 2:userID, 3:serviceID, 4:appointmentDate,
    //             5:status, 6:staffID (có thể null), 7:consultationNotes (có thể null)
    private static final String INSERT =
        "INSERT INTO tblAppointments "
      + "(appointmentID, userID, serviceID, appointmentDate, "
      + " status, staffID, consultationNotes) "
      + "VALUES (?, ?, ?, ?, ?, ?, ?)";

    // Cập nhật trạng thái lịch hẹn ('Completed' hoặc 'Cancelled')
    // [TÁC ĐỘNG]: Khớp với updateStatus() -> CancelAppointmentController & CompleteAppointmentController
    private static final String UPDATE_STATUS =
        "UPDATE tblAppointments SET status = ? WHERE appointmentID = ?";

    // Cập nhật ghi chú tư vấn của nhân viên
    // [TÁC ĐỘNG]: Khớp với updateNotes() -> SaveNotesController
    private static final String UPDATE_NOTES =
        "UPDATE tblAppointments SET consultationNotes = ? "
      + "WHERE appointmentID = ?";

    // Lấy mã ID lớn nhất có tiền tố 'app' để sinh mã tự động tiếp theo
    // [TÁC ĐỘNG]: Khớp với getNextID() -> BookAppointmentController
    private static final String GET_MAX_ID =
        "SELECT TOP 1 appointmentID FROM tblAppointments "
      + "WHERE appointmentID LIKE 'app%' "
      + "  AND ISNUMERIC(SUBSTRING(appointmentID, 4, 50)) = 1 "
      + "ORDER BY CAST(SUBSTRING(appointmentID, 4, 50) AS INT) DESC";

    /**
     * Lấy toàn bộ lịch hẹn của một khách hàng (UserID).
     *
     * [LUỒNG THỰC THI & DỮ LIỆU]:
     * - Được gọi từ: [ViewAppointmentsController.java] (action=ViewAppointments).
     * - Input: `userID` lấy từ Session (`loginUser.getUserID()`).
     * - Output: `List<Appointment>` (chứa đầy đủ thông tin + tên dịch vụ + tên staff).
     * - Dữ liệu tiếp tục đi tới: [appointmentList.jsp] qua request attribute `LIST_APPOINTMENT`.
     */
    public List<Appointment> getByUser(String userID)
            throws SQLException, ClassNotFoundException {
        return queryList(GET_BY_USER, userID);
    }

    /**
     * Lấy toàn bộ lịch hẹn được phân công cho một nhân viên (StaffID).
     *
     * [LUỒNG THỰC THI & DỮ LIỆU]:
     * - Được gọi từ: [ViewConsultationController.java] (action=ViewConsultation).
     * - Input: `staffID` lấy từ Session (`loginUser.getUserID()`).
     * - Output: `List<Appointment>` (chứa thông tin khách hàng + ghi chú tư vấn).
     * - Dữ liệu tiếp tục đi tới: [consultation.jsp] qua request attribute `LIST_CONSULTATION`.
     */
    public List<Appointment> getByStaff(String staffID)
            throws SQLException, ClassNotFoundException {
        return queryList(GET_BY_STAFF, staffID);
    }

    /**
     * Lấy chi tiết 1 lịch hẹn theo `appointmentID`.
     *
     * [LUỒNG THỰC THI & DỮ LIỆU]:
     * - Được gọi từ:
     *   1. [CancelAppointmentController.java] (Kiểm tra lịch có đúng của user và đang là Pending không).
     *   2. [SaveNotesController.java] (Kiểm tra lịch có đúng được giao cho staff đang login không).
     *   3. [CompleteAppointmentController.java] (Kiểm tra quyền staff và trạng thái Pending).
     * - Input: `appointmentID` (chuỗi mã lịch hẹn).
     * - Output: Đối tượng `Appointment` hoặc `null` nếu không tìm thấy.
     */
    public Appointment getByID(String appointmentID)
            throws SQLException, ClassNotFoundException {
        List<Appointment> list = queryList(GET_BY_ID, appointmentID);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Thêm lịch hẹn mới vào CSDL.
     *
     * [LUỒNG THỰC THI & DỮ LIỆU]:
     * - Được gọi từ: [BookAppointmentController.java] (action=BookAppointment).
     * - Input: Đối tượng `Appointment` (appointmentID, userID, serviceID, appointmentDate, status='Pending', staffID=null, notes=null).
     * - Output: `true` nếu lưu thành công vào tblAppointments, `false` nếu thất bại.
     */
    public boolean insert(Appointment app)
            throws SQLException, ClassNotFoundException {
        boolean inserted = false;
        Connection conn = null;
        PreparedStatement ptm = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn != null) {
                ptm = conn.prepareStatement(INSERT);
                ptm.setString(1, app.getAppointmentID());
                ptm.setString(2, app.getUserID());
                ptm.setString(3, app.getServiceID());
                ptm.setTimestamp(4, app.getAppointmentDate());
                ptm.setString(5, app.getStatus());
                if (app.getStaffID() == null) {
                    ptm.setNull(6, Types.VARCHAR);
                } else {
                    ptm.setString(6, app.getStaffID());
                }
                if (app.getConsultationNotes() == null) {
                    ptm.setNull(7, Types.NVARCHAR);
                } else {
                    ptm.setString(7, app.getConsultationNotes());
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
     * Cập nhật trạng thái lịch hẹn (Pending -> Completed hoặc Pending -> Cancelled).
     *
     * [LUỒNG THỰC THI & DỮ LIỆU]:
     * - Được gọi từ:
     *   1. [CancelAppointmentController.java] với status="Cancelled".
     *   2. [CompleteAppointmentController.java] với status="Completed".
     * - Input: `id` (mã lịch hẹn), `status` (chuỗi trạng thái mới).
     * - Output: `true` nếu cập nhật thành công, `false` nếu thất bại.
     */
    public boolean updateStatus(String id, String status)
            throws SQLException, ClassNotFoundException {
        boolean updated = false;
        Connection conn = null;
        PreparedStatement ptm = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn != null) {
                ptm = conn.prepareStatement(UPDATE_STATUS);
                ptm.setString(1, status);
                ptm.setString(2, id);
                updated = ptm.executeUpdate() > 0;
            }
        } finally {
            if (ptm != null) { ptm.close(); }
            if (conn != null) { conn.close(); }
        }
        return updated;
    }

    /**
     * Lưu ghi chú tư vấn của nhân viên phụ trách.
     *
     * [LUỒNG THỰC THI & DỮ LIỆU]:
     * - Được gọi từ: [SaveNotesController.java] (action=SaveNotes).
     * - Input: `id` (mã lịch hẹn), `notes` (nội dung ghi chú từ form consultation.jsp).
     * - Output: `true` nếu lưu thành công, `false` nếu thất bại.
     */
    public boolean updateNotes(String id, String notes)
            throws SQLException, ClassNotFoundException {
        boolean updated = false;
        Connection conn = null;
        PreparedStatement ptm = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn != null) {
                ptm = conn.prepareStatement(UPDATE_NOTES);
                if (notes == null || notes.trim().isEmpty()) {
                    ptm.setNull(1, Types.NVARCHAR);
                } else {
                    ptm.setString(1, notes.trim());
                }
                ptm.setString(2, id);
                updated = ptm.executeUpdate() > 0;
            }
        } finally {
            if (ptm != null) { ptm.close(); }
            if (conn != null) { conn.close(); }
        }
        return updated;
    }

    /**
     * Tự động tính toán mã lịch hẹn tiếp theo (ví dụ: 'app8' -> 'app9').
     *
     * [LUỒNG THỰC THI & DỮ LIỆU]:
     * - Được gọi từ: [BookAppointmentController.java].
     * - Output: Chuỗi mã mới (ví dụ: 'app1', 'app2', 'app9').
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
                    String maxID = rs.getString("appointmentID");
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
        return "app" + next;
    }

    /**
     * Hàm dùng chung để thực thi câu truy vấn đọc danh sách Appointment (query mapping).
     * [DỮ LIỆU ĐI TỚI ĐÂU]:
     * - Đọc kết quả từ ResultSet và ánh xạ vào các đối tượng Appointment,
     *   đồng thời gán các trường mở rộng (serviceName, userName, staffName).
     */
    private List<Appointment> queryList(String sql, String param)
            throws SQLException, ClassNotFoundException {
        List<Appointment> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ptm = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn != null) {
                ptm = conn.prepareStatement(sql);
                ptm.setString(1, param);
                rs = ptm.executeQuery();
                while (rs.next()) {
                    Appointment app = new Appointment(
                            rs.getString("appointmentID"),
                            rs.getString("userID"),
                            rs.getString("serviceID"),
                            rs.getTimestamp("appointmentDate"),
                            rs.getString("status"),
                            rs.getString("staffID"),
                            rs.getString("consultationNotes"));
                    app.setServiceName(rs.getString("serviceName"));
                    app.setUserName(rs.getString("userName"));
                    app.setStaffName(rs.getString("staffName"));
                    list.add(app);
                }
            }
        } finally {
            if (rs != null) { rs.close(); }
            if (ptm != null) { ptm.close(); }
            if (conn != null) { conn.close(); }
        }
        return list;
    }
}
