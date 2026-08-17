package com.spa.dao;

import com.spa.dto.Service;
import com.spa.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * [VAI TRÒ & CHỨC NĂNG]
 * - Lớp DAO (Data Access Object) quản lý toàn bộ thao tác CSDL liên quan tới bảng `tblServices`.
 * - Thực hiện các chức năng CRUD dịch vụ (Tìm kiếm, xem chi tiết, kiểm tra trùng mã,
 *   thêm mới, cập nhật, xóa dịch vụ).
 *
 * [NGUYÊN TẮC THIẾT KẾ & AN TOÀN]
 * 1. Phương thức tìm kiếm/lấy danh sách luôn trả về List rỗng (new ArrayList<>()) thay vì `null`,
 *    giúp giao diện JSP (vòng lặp <c:forEach>) không bị ngoại lệ NullPointerException.
 * 2. Ký tự `%` trong toán tử LIKE được gắn vào giá trị tham số Java (`"%" + name + "%"`)
 *    chứ không nối chuỗi vào câu SQL, đảm bảo không có lỗ hổng SQL Injection.
 * 3. Không nuốt ngoại lệ vi phạm khóa ngoại (Foreign Key Violation - SQL Error Code 547)
 *    khi xóa dịch vụ, để DeleteServiceController bắt và đưa ra thông báo rõ ràng cho người dùng.
 *
 * [LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
 * 1. Sửa bảng `tblServices` hoặc tên các cột (serviceID, serviceName, description, price):
 *    -> Phải sửa các hằng số SQL (SEARCH, GET_BY_ID, CHECK_ID, INSERT, UPDATE, DELETE).
 *    -> Phải sửa DTO [Service.java].
 * 2. Sửa phương thức `search(name)`:
 *    -> Ảnh hưởng tới [ViewServicesController.java] (Admin xem & tìm dịch vụ).
 *    -> Ảnh hưởng tới [LoadBookingController.java] (Khách hàng nạp danh mục dịch vụ để đặt lịch).
 * 3. Sửa phương thức `getByID(serviceID)`:
 *    -> Ảnh hưởng tới [LoadServiceController.java] (Admin nạp dịch vụ lên form sửa).
 *    -> Ảnh hưởng tới [BookAppointmentController.java] (Kiểm tra dịch vụ còn tồn tại trước khi book).
 * 4. Sửa phương thức `insert(service)`, `isServiceIDExisted(id)`:
 *    -> Ảnh hưởng tới [CreateServiceController.java].
 * 5. Sửa phương thức `update(service)`:
 *    -> Ảnh hưởng tới [UpdateServiceController.java].
 * 6. Sửa phương thức `delete(serviceID)`:
 *    -> Ảnh hưởng tới [DeleteServiceController.java].
 * ============================================================================
 */
public class ServiceDAO {

    // [CÁC HẰNG SỐ CÂU LỆNH SQL]
    // Tìm kiếm dịch vụ theo tên có phân trang hoặc lọc theo từ khóa
    // [TÁC ĐỘNG]: Khớp với search() -> ViewServicesController & LoadBookingController
    private static final String SEARCH =
        "SELECT serviceID, serviceName, description, price "
      + "FROM tblServices WHERE serviceName LIKE ? ORDER BY serviceID";

    // Lấy thông tin 1 dịch vụ theo mã khóa chính
    // [TÁC ĐỘNG]: Khớp với getByID() -> LoadServiceController & BookAppointmentController
    private static final String GET_BY_ID =
        "SELECT serviceID, serviceName, description, price "
      + "FROM tblServices WHERE serviceID = ?";

    // Kiểm tra mã dịch vụ đã tồn tại chưa trước khi tạo mới
    // [TÁC ĐỘNG]: Khớp với isServiceIDExisted() -> CreateServiceController
    private static final String CHECK_ID =
        "SELECT serviceID FROM tblServices WHERE serviceID = ?";

    // Thêm mới dịch vụ vào bảng tblServices
    // [TÁC ĐỘNG]: Thứ tự tham số: 1:serviceID, 2:serviceName, 3:description, 4:price
    private static final String INSERT =
        "INSERT INTO tblServices (serviceID, serviceName, description, price) "
      + "VALUES (?, ?, ?, ?)";

    // Cập nhật dịch vụ theo mã khóa chính
    // [TÁC ĐỘNG]: CỰC KỲ QUAN TRỌNG: Thứ tự tham số là:
    // 1:serviceName, 2:description, 3:price, và 4:serviceID (trong WHERE)
    private static final String UPDATE =
        "UPDATE tblServices SET serviceName = ?, description = ?, price = ? "
      + "WHERE serviceID = ?";

    // Xóa dịch vụ khỏi bảng tblServices
    // [TÁC ĐỘNG]: Khớp với delete() -> DeleteServiceController
    private static final String DELETE =
        "DELETE FROM tblServices WHERE serviceID = ?";

    /**
     * Tìm kiếm danh sách dịch vụ theo tên (hoặc lấy tất cả nếu name="").
     *
     * [LUỒNG THỰC THI & DỮ LIỆU]:
     * - Được gọi từ:
     *   1. [ViewServicesController.java] (Admin tìm kiếm & xem danh sách dịch vụ).
     *   2. [LoadBookingController.java] (Khách hàng mở form đặt lịch với name="").
     * - Input: `name` (từ request.getParameter("search") hoặc chuỗi rỗng "").
     * - Output: `List<Service>` chứa các đối tượng Service (không bao giờ null).
     * - Dữ liệu tiếp tục đi tới:
     *   + [serviceList.jsp] qua request attribute `LIST_SERVICE`.
     *   + [bookAppointment.jsp] qua request attribute `LIST_SERVICE`.
     */
    public List<Service> search(String name)
            throws SQLException, ClassNotFoundException {
        List<Service> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ptm = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn != null) {
                ptm = conn.prepareStatement(SEARCH);
                ptm.setString(1, "%" + name + "%");
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
     * Lấy thông tin chi tiết một dịch vụ bằng mã `serviceID`.
     *
     * [LUỒNG THỰC THI & DỮ LIỆU]:
     * - Được gọi từ:
     *   1. [LoadServiceController.java] (Admin bấm Edit trên serviceList.jsp).
     *   2. [BookAppointmentController.java] (Kiểm tra dịch vụ còn tồn tại trước khi tạo lịch).
     * - Input: `serviceID` (chuỗi mã dịch vụ).
     * - Output: Đối tượng `Service` nếu tìm thấy, `null` nếu dịch vụ không tồn tại / đã bị xóa.
     * - Dữ liệu tiếp tục đi tới: [updateService.jsp] qua request attribute `SERVICE`.
     */
    public Service getByID(String serviceID)
            throws SQLException, ClassNotFoundException {
        Service service = null;
        Connection conn = null;
        PreparedStatement ptm = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn != null) {
                ptm = conn.prepareStatement(GET_BY_ID);
                ptm.setString(1, serviceID);
                rs = ptm.executeQuery();
                if (rs.next()) {
                    service = new Service(
                            rs.getString("serviceID"),
                            rs.getString("serviceName"),
                            rs.getString("description"),
                            rs.getBigDecimal("price"));
                }
            }
        } finally {
            if (rs != null) { rs.close(); }
            if (ptm != null) { ptm.close(); }
            if (conn != null) { conn.close(); }
        }
        return service;
    }

    /**
     * Kiểm tra mã dịch vụ `serviceID` đã tồn tại trong CSDL hay chưa.
     *
     * [LUỒNG THỰC THI & DỮ LIỆU]:
     * - Được gọi từ: [CreateServiceController.java].
     * - Input: `id` (mã dịch vụ người dùng nhập vào form tạo mới).
     * - Output: `true` nếu đã có (trùng khóa chính), `false` nếu chưa có (hợp lệ để thêm).
     */
    public boolean isServiceIDExisted(String id)
            throws SQLException, ClassNotFoundException {
        boolean existed = false;
        Connection conn = null;
        PreparedStatement ptm = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn != null) {
                ptm = conn.prepareStatement(CHECK_ID);
                ptm.setString(1, id);
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
     * Thêm mới một dịch vụ vào bảng `tblServices`.
     *
     * [LUỒNG THỰC THI & DỮ LIỆU]:
     * - Được gọi từ: [CreateServiceController.java] (action=CreateService).
     * - Input: Đối tượng `Service` (chứa serviceID, serviceName, description, price).
     * - Output: `true` nếu thêm thành công, `false` nếu thất bại.
     * - Khi thành công: Controller chuyển tiếp về [ViewServicesController] để tải lại danh sách.
     */
    public boolean insert(Service service)
            throws SQLException, ClassNotFoundException {
        boolean inserted = false;
        Connection conn = null;
        PreparedStatement ptm = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn != null) {
                ptm = conn.prepareStatement(INSERT);
                ptm.setString(1, service.getServiceID());
                ptm.setString(2, service.getServiceName());
                ptm.setString(3, service.getDescription());
                ptm.setBigDecimal(4, service.getPrice());
                inserted = ptm.executeUpdate() > 0;
            }
        } finally {
            if (ptm != null) { ptm.close(); }
            if (conn != null) { conn.close(); }
        }
        return inserted;
    }

    /**
     * Cập nhật thông tin dịch vụ theo `serviceID`.
     *
     * [LUỒNG THỰC THI & DỮ LIỆU]:
     * - Được gọi từ: [UpdateServiceController.java] (action=UpdateService).
     * - Input: Đối tượng `Service` mang dữ liệu mới từ form [updateService.jsp].
     * - Output: `true` nếu cập nhật thành công, `false` nếu không tìm thấy dòng để sửa.
     * 
     * [LƯU Ý THỨ TỰ THAM SỐ]:
     * - 1: serviceName, 2: description, 3: price, 4: serviceID.
     */
    public boolean update(Service service)
            throws SQLException, ClassNotFoundException {
        boolean updated = false;
        Connection conn = null;
        PreparedStatement ptm = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn != null) {
                ptm = conn.prepareStatement(UPDATE);
                ptm.setString(1, service.getServiceName());
                ptm.setString(2, service.getDescription());
                ptm.setBigDecimal(3, service.getPrice());
                ptm.setString(4, service.getServiceID());
                updated = ptm.executeUpdate() > 0;
            }
        } finally {
            if (ptm != null) { ptm.close(); }
            if (conn != null) { conn.close(); }
        }
        return updated;
    }

    /**
     * Xóa một dịch vụ khỏi CSDL theo `serviceID`.
     *
     * [LUỒNG THỰC THI & DỮ LIỆU]:
     * - Được gọi từ: [DeleteServiceController.java] (action=DeleteService).
     * - Input: `serviceID` (mã dịch vụ cần xóa).
     * - Output: `true` nếu xóa thành công, `false` nếu không tìm thấy dịch vụ.
     * - Xử lý lỗi khóa ngoại (Foreign Key):
     *   Nếu dịch vụ đã có trong tblAppointments hoặc tblReviews, SQL Server ném SQLException (mã 547),
     *   DeleteServiceController sẽ bắt lỗi này để hiển thị thông báo thân thiện.
     */
    public boolean delete(String serviceID)
            throws SQLException, ClassNotFoundException {
        boolean deleted = false;
        Connection conn = null;
        PreparedStatement ptm = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn != null) {
                ptm = conn.prepareStatement(DELETE);
                ptm.setString(1, serviceID);
                deleted = ptm.executeUpdate() > 0;
            }
        } finally {
            if (ptm != null) { ptm.close(); }
            if (conn != null) { conn.close(); }
        }
        return deleted;
    }
}
