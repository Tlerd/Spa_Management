================================================================
 Spa Consultation and Service Management
 PRJ301 - Workshop 1 & 2
 Java web application theo mo hinh MVC2 voi Servlet, JSP, JSTL
 va SQL Server
================================================================

----------------------------------------------------------------
1. TAI KHOAN THU
----------------------------------------------------------------
Dang nhap bang EMAIL va MAT KHAU (khong phai bang userID).

  Vai tro | Email               | Mat khau  | userID
  --------+---------------------+-----------+--------
  Admin   | admin@example.com   | adminpass | admin1
  User    | john@example.com    | userpass  | user1
  User    | ha@example.com      | ha12345   | user2
  User    | tuan@example.com    | tuan123   | user3
  Staff   | staff@example.com   | staffpass | staff1
  Staff   | anh@example.com     | anh12345  | staff2

Tai khoan dang ky moi tu trang register.jsp LUON co vai tro USR.
Vai tro Admin va Staff chi tao truc tiep trong co so du lieu.

----------------------------------------------------------------
2. DUNG CO SO DU LIEU
----------------------------------------------------------------
Mo SQL Server Management Studio, mo tep SpaConsultationPortal.sql
o thu muc goc cua project, chay TOAN BO trong mot lan.

Tep do tu lam ba viec: tao co so du lieu SpaConsultationPortal,
tao bon bang, va chen du lieu mau (6 tai khoan, 6 dich vu,
8 lich hen trai du ba trang thai, 4 danh gia).

Neu doi mat khau tai khoan sa hoac ten may chu, sua NAM hang so
trong tep:

    src/java/com/spa/util/DatabaseConnection.java

Gia tri dang dung, lay theo cau hinh cua project UserManagement:

    SERVER    = localhost
    PORT      = 1433
    DB_NAME   = SpaConsultationPortal
    USER_NAME = sa
    PASSWORD  = 12345

Hai thu SQL Server tat san khi cai va phai bat bang tay:
  - Xac thuc hon hop: Management Studio -> chuot phai ten may chu
    -> Properties -> Security -> chon "SQL Server and Windows
    Authentication mode" -> khoi dong lai dich vu.
  - Giao thuc TCP: SQL Server Configuration Manager -> bat TCP/IP
    -> tab IP Addresses -> muc IPAll dien cong 1433.
    Thieu buoc nay thi Java bao khong ket noi duoc trong khi
    Management Studio van vao binh thuong, vi Management Studio
    noi bang bo nho dung chung chu khong qua TCP.

----------------------------------------------------------------
3. CHAY UNG DUNG
----------------------------------------------------------------
Mo project bang NetBeans 13 roi bam Run.

Chay tu dong lenh:

    ant -Duser.properties.file="<duong dan>/NetBeans/13/build.properties" dist

roi chep dist/Spa_Management.war vao thu muc webapps cua Tomcat.

Duong dan ung dung la /Spa_Management, xem web/META-INF/context.xml.

LUU Y VE CONG. Ban Tomcat trong D:/projectCODEfpt/apache-tomcat-9.0.118
da duoc doi sang cong 8084 chu khong phai 8080 mac dinh, xem the
Connector trong conf/server.xml. Voi ban do thi dia chi la:

    http://localhost:8084/Spa_Management/

Voi mot ban Tomcat 9 chua doi cong thi la cong 8080 nhu thuong le.

TUYET DOI khong dung Tomcat 10 tro len. Tu ban 10, goi javax.servlet
doi ten thanh jakarta.servlet nen ma nguon nay khong chay. Trieu
chung de gay hoang mang: may chu khoi dong tron tru, khong bao loi
gi, nhung moi dia chi deu tra ve 404.

----------------------------------------------------------------
4. THU VIEN TRONG THU MUC lib
----------------------------------------------------------------
Ca ba tep deu duoc goi bang DUONG DAN TUONG DOI trong
nbproject/project.properties, nen project chay duoc tren may khac
ma khong phai sua cau hinh.

  sqljdbc4.jar                     trinh dieu khien JDBC cho SQL Server
  taglibs-standard-spec-1.2.5.jar  phan giao dien cua JSTL 1.2
  taglibs-standard-impl-1.2.5.jar  phan cai dat cua JSTL 1.2

Hai tep JSTL la bat buoc vi Tomcat 9 KHONG kem san JSTL. Thieu
chung thi moi trang JSP bao loi khong tim thay thu vien the.

Ban dung dua ba tep nay vao WEB-INF/lib cua tep war nho mot dich
-post-compile trong build.xml.

----------------------------------------------------------------
5. CAU TRUC MA NGUON
----------------------------------------------------------------
  src/java/com/spa/util/        DatabaseConnection, noi duy nhat mo ket noi
  src/java/com/spa/dto/         4 lop cho du lieu, khong chua SQL
  src/java/com/spa/dao/         4 lop DAO, NOI DUY NHAT chua cau lenh SQL
  src/java/com/spa/controller/  MainController + 17 servlet chuc nang
  web/                          11 trang JSP viet bang JSTL
  web/css/styles.css            bang kieu chung
  web/WEB-INF/web.xml           khai bao ca 18 servlet
  SpaConsultationPortal.sql     tep dung co so du lieu

Moi bieu mau va lien ket can du lieu deu gui ve MainController kem
mot tham so ten action. Ngoai le hop le duy nhat la lien ket toi
createService.jsp, vi bieu mau them moi hoan toan trong.

Quy tac vang da ap dung: sau moi thao tac GHI du lieu, luon quay ve
mot servlet DOC du lieu chu khong ve thang trang JSP.

----------------------------------------------------------------
6. HAI PHUONG THUC DAO THEM NGOAI BANG 7 CUA DE BAI
----------------------------------------------------------------
  AppointmentDAO.getByID(String)  can de kiem tra ba rang buoc nghiep
                                  vu: huy lich phai dung cua minh va
                                  dang o trang thai Pending, ghi chu va
                                  hoan thanh phai dung nhan vien duoc
                                  phan cong.
  ReviewDAO.getNextID()           cot reviewID la khoa chinh kieu chuoi
                                  va khong tu sinh, giong het ly do cua
                                  AppointmentDAO.getNextID().

----------------------------------------------------------------
7. GHI CHU KHI CHAM BAI
----------------------------------------------------------------
Mat khau luu nguyen van trong bang, dung nhu pham vi bai tap. Trong
he thong that mat khau phai duoc bam truoc khi luu.

Lop User co y KHONG co thuoc tinh mat khau: ung dung da xac thuc xong
truoc khi tao doi tuong nen khong con ly do chuyen mat khau qua cac
tang va cat vao phien lam viec.

Moi cau lenh SQL deu dung dau hoi lam cho giu tham so, khong noi chuoi
o bat ky cho nao, ke ca hai dau phan tram cua toan tu LIKE.

Phep kiem tra vai tro lam o CA HAI noi, o tang Controller va o dau
trang JSP. Giau mot lien ket di khong phai la chan.
