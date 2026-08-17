package com.spa.dto;

import java.math.BigDecimal;

/**
 * ============================================================================
 * [VAI TRÒ & CHỨC NĂNG]
 * - Lớp DTO (Data Transfer Object) đại diện cho Dịch Vụ Spa (tblServices).
 * - Đóng gói dữ liệu về dịch vụ (mã, tên, mô tả, giá) để truyền tải giữa:
 *   ServiceDAO, các Controller (View, Load, Create, Update, Delete, Booking, Review) và JSP.
 * - Kiểu dữ liệu `price` dùng BigDecimal: Tương ứng chính xác với kiểu DECIMAL(18,2)
 *   trong SQL Server, tránh sai số làm tròn khi tính toán tiền bạc so với float/double.
 *
 * [LUỒNG DỮ LIỆU (DATA FLOW)]
 * 1. Từ CSDL (tblServices):
 *    - ServiceDAO.search() -> List<Service> -> ViewServicesController -> [serviceList.jsp] (${LIST_SERVICE})
 *    - ServiceDAO.search("") -> List<Service> -> LoadBookingController -> [bookAppointment.jsp] (${LIST_SERVICE})
 *    - ServiceDAO.getByID() -> Service -> LoadServiceController -> [updateService.jsp] (${SERVICE})
 *    - ReviewDAO.getReviewableServices() -> List<Service> -> LoadReviewController -> [reviewService.jsp] (${LIST_SERVICE})
 * 2. Từ Người dùng (Form HTML gửi lên Controller):
 *    - [createService.jsp] -> CreateServiceController -> new Service(...) -> ServiceDAO.insert(service) -> tblServices
 *    - [updateService.jsp] -> UpdateServiceController -> new Service(...) -> ServiceDAO.update(service) -> tblServices
 *
 * [LIÊN HỆ & TÁC ĐỘNG KHI SỬA ĐỔI (IMPACT ANALYSIS)]
 * 1. Sửa tên trường `serviceID`:
 *    -> Phải sửa getter/setter: getServiceID(), setServiceID().
 *    -> Phải sửa cột serviceID trong bảng [tblServices], [tblAppointments], [tblReviews] ở [SpaConsultationPortal.sql].
 *    -> Phải sửa câu lệnh SQL trong [ServiceDAO.java], [AppointmentDAO.java], [ReviewDAO.java].
 *    -> Phải sửa trên các form và bảng JSP:
 *       + [serviceList.jsp]: ${service.serviceID}
 *       + [createService.jsp]: name="serviceID", value="${OLD_SERVICE_ID}"
 *       + [updateService.jsp]: name="serviceID", value="${SERVICE.serviceID}"
 *       + [bookAppointment.jsp]: <option value="${service.serviceID}">
 *       + [reviewService.jsp]: <option value="${service.serviceID}">
 * 2. Sửa tên trường `serviceName`:
 *    -> Phải sửa ServiceDAO (SEARCH, GET_BY_ID, INSERT, UPDATE).
 *    -> Phải sửa hiển thị trên các JSP: [serviceList.jsp], [updateService.jsp], [bookAppointment.jsp], [reviewService.jsp].
 * 3. Sửa kiểu dữ liệu `price` (ví dụ đổi sang Double/Float):
 *    -> Phải sửa constructor, getter/setter của lớp này.
 *    -> Phải sửa cách đọc ResultSet (rs.getBigDecimal -> rs.getDouble) trong ServiceDAO, ReviewDAO.
 *    -> Phải sửa cách ép kiểu và validate trong CreateServiceController, UpdateServiceController.
 * ============================================================================
 */
public class Service {

    // [TÁC ĐỘNG]: Khóa chính trong bảng tblServices VARCHAR(50)
    private String serviceID;

    // [TÁC ĐỘNG]: Khớp cột serviceName NVARCHAR(200) trong bảng tblServices
    private String serviceName;

    // [TÁC ĐỘNG]: Khớp cột description NVARCHAR(1000) trong bảng tblServices
    private String description;

    // [TÁC ĐỘNG]: Khớp cột price DECIMAL(18,2) trong bảng tblServices
    private BigDecimal price;

    public Service() {
    }

    public Service(String serviceID, String serviceName,
                   String description, BigDecimal price) {
        this.serviceID = serviceID;
        this.serviceName = serviceName;
        this.description = description;
        this.price = price;
    }

    public String getServiceID() { return serviceID; }
    public void setServiceID(String serviceID) {
        this.serviceID = serviceID;
    }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}
