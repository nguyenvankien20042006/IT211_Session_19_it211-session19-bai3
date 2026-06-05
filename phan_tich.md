# Phần 1 - Phân tích logic

# 1. Phân tích RefreshTokenService và RefreshTokenRepository hiện tại

Trong hầu hết các hệ thống JWT sử dụng Refresh Token, hai thành phần chính thường có cấu trúc tương tự như sau:

## RefreshTokenRepository

Các phương thức phổ biến:

```java
Optional<RefreshToken> findByToken(String token);

RefreshToken save(RefreshToken refreshToken);

void delete(RefreshToken refreshToken);
```

Chức năng:

- Tìm Refresh Token theo giá trị token.
- Lưu Refresh Token mới vào cơ sở dữ liệu.
- Xóa một Refresh Token cụ thể khi người dùng logout.

---

## RefreshTokenService

Các phương thức phổ biến:

```java
RefreshToken createRefreshToken(Long userId);

RefreshToken verifyExpiration(RefreshToken token);

void deleteByToken(String token);
```

Chức năng:

### createRefreshToken()

Tạo Refresh Token mới:

```java
UUID.randomUUID().

toString()
```

Lưu vào database cùng với:

- User
- Token
- Expiration Time

---

### verifyExpiration()

Kiểm tra Refresh Token đã hết hạn hay chưa.

Nếu hết hạn:

```java
refreshTokenRepository.delete(token);
```

Nếu còn hiệu lực:

```java
return token;
```

---

### deleteByToken()

Được gọi khi logout.

```java
refreshTokenRepository.delete(token);
```

Chỉ xóa đúng token được truyền vào.

---

# 2. Đánh giá cơ chế logout hiện tại

## Luồng hoạt động hiện tại

Giả sử người dùng đăng nhập trên:

### Thiết bị A

```text
iPhone
```

Tạo:

```text
Refresh Token A
```

---

### Thiết bị B

```text
iPad
```

Tạo:

```text
Refresh Token B
```

---

### Thiết bị C

```text
Laptop
```

Tạo:

```text
Refresh Token C
```

---

Database:

| User  | Refresh Token |
|-------|---------------|
| user1 | Token A       |
| user1 | Token B       |
| user1 | Token C       |

---

Người dùng logout trên:

```text
iPhone
```

Hệ thống thực hiện:

```java
deleteByToken(TokenA);
```

Sau khi xóa:

| User  | Refresh Token |
|-------|---------------|
| user1 | Token B       |
| user1 | Token C       |

---

## Đánh giá hiệu quả

### Ưu điểm

- Đơn giản.
- Dễ triển khai.
- Không ảnh hưởng các thiết bị khác.

---

### Nhược điểm

Không đáp ứng yêu cầu bảo mật trong hệ thống thanh toán điện tử.

Người dùng nghĩ rằng:

```text
Tôi đã đăng xuất tài khoản.
```

Nhưng thực tế:

```text
Các thiết bị khác vẫn đang đăng nhập.
```

Điều này tạo ra khoảng trống bảo mật rất lớn.

---

# 3. Các hạn chế hiện tại

## Hạn chế 1: Không hỗ trợ logout toàn bộ thiết bị

Hiện tại chỉ có:

```java
deleteByToken(token);
```

Hệ thống không có khả năng:

```text
Logout tất cả thiết bị
```

---

### Hậu quả

Nếu:

- Điện thoại bị mất.
- Máy tính bảng bị đánh cắp.
- Có phiên đăng nhập lạ.

Người dùng không thể:

```text
Thu hồi toàn bộ quyền truy cập.
```

---

## Hạn chế 2: Không định danh thiết bị

Entity hiện tại thường có dạng:

```java
public class RefreshToken {

    private Long id;

    private String token;

    private Instant expiryDate;

    private User user;
}
```

Không có:

```java
deviceId
```

---

### Hậu quả

Không thể biết:

```text
Token này thuộc thiết bị nào?
```

Ví dụ:

| Token   |
|---------|
| Token A |
| Token B |
| Token C |

Hệ thống không phân biệt được:

```text
A = iPhone
B = Laptop
C = Tablet
```

---

### Tác động

Không thể:

- Logout thiết bị cụ thể.
- Hiển thị danh sách thiết bị đang đăng nhập.
- Quản lý phiên theo từng thiết bị.

---

## Hạn chế 3: Không có cơ chế logout theo phiên

Hiện tại chỉ xóa theo:

```java
token
```

Trong khi thực tế nên hỗ trợ:

```java
userId +deviceId
```

để quản lý chính xác từng phiên.

---

## Hạn chế 4: Không có cơ chế thu hồi toàn bộ token của user

Repository thường chưa có:

```java
deleteByUserId(Long userId);
```

---

### Hậu quả

Khi phát hiện tài khoản bị xâm nhập:

```text
Không thể khóa tất cả phiên ngay lập tức.
```

---

## Hạn chế 5: Không tự động dọn dẹp token hết hạn

Token hết hạn vẫn tồn tại trong database.

Ví dụ:

```text
1 triệu token cũ
```

vẫn được lưu.

---

### Hậu quả

Tăng:

- Dung lượng lưu trữ.
- Kích thước bảng dữ liệu.
- Thời gian truy vấn.

---

### Rủi ro bảo mật

Token cũ vẫn xuất hiện trong database.

Nếu có lỗ hổng khác:

```text
Dữ liệu token có thể bị thu thập.
```

---

# 4. Đề xuất cải tiến

# Giải pháp 1: Bổ sung deviceId

Mở rộng Entity:

```java
public class RefreshToken {

    private Long id;

    private String token;

    private Instant expiryDate;

    private User user;

    private String deviceId;
}
```

---

## Vai trò của deviceId

Mỗi lần đăng nhập:

```text
Laptop → deviceId = D1
iPhone → deviceId = D2
Tablet → deviceId = D3
```

Database:

| User  | Device | Token   |
|-------|--------|---------|
| user1 | D1     | Token A |
| user1 | D2     | Token B |
| user1 | D3     | Token C |

---

Lợi ích:

- Quản lý phiên theo thiết bị.
- Logout đúng thiết bị.
- Hiển thị danh sách thiết bị đăng nhập.
- Phát hiện thiết bị lạ.

---

# Giải pháp 2: Logout theo thiết bị

Thêm phương thức:

```java
deleteByUserIdAndDeviceId(
        Long userId,
        String deviceId
);
```

---

Luồng hoạt động:

```text
Logout trên iPhone
```

Hệ thống chỉ xóa:

```text
Token của iPhone
```

Không ảnh hưởng:

```text
Laptop
Tablet
```

---

# Giải pháp 3: Logout tất cả thiết bị

Bổ sung:

```java
deleteAllByUserId(Long userId);
```

---

Luồng hoạt động:

```text
Logout All Devices
```

Kết quả:

| User  | Device | Token   |
|-------|--------|---------|
| user1 | D1     | Deleted |
| user1 | D2     | Deleted |
| user1 | D3     | Deleted |

---

Lợi ích:

- Khóa toàn bộ phiên ngay lập tức.
- Xử lý khi mất thiết bị.
- Giảm rủi ro bị chiếm quyền tài khoản.

---

# Giải pháp 4: Tự động dọn dẹp token hết hạn

Bổ sung:

```java
deleteExpiredTokens();
```

Logic:

```java
DELETE
FROM refresh_token
WHERE expiry_date <NOW()
```

---

Thực hiện định kỳ:

```text
Mỗi giờ
Hoặc mỗi ngày
```

---

Lợi ích

- Giảm dữ liệu rác.
- Tăng hiệu năng database.
- Giảm rủi ro bảo mật.

---

# 5. Tầm quan trọng của deviceId

Trong hệ thống thanh toán điện tử, một tài khoản có thể đăng nhập trên nhiều thiết bị cùng lúc.

Nếu không có:

```java
deviceId
```

hệ thống chỉ biết:

```text
Người dùng có token.
```

Nhưng không biết:

```text
Token thuộc thiết bị nào.
```

Việc bổ sung deviceId giúp:

- Quản lý phiên chính xác.
- Logout theo từng thiết bị.
- Logout toàn bộ thiết bị.
- Hiển thị lịch sử đăng nhập.
- Phát hiện truy cập bất thường.
- Nâng cao bảo mật cho các giao dịch tài chính.

---

# 6. Kết luận

Cơ chế Refresh Token hiện tại chỉ hỗ trợ thu hồi một token đơn lẻ và chưa đáp ứng yêu cầu quản lý phiên trên nhiều thiết
bị. Các hạn chế chính gồm: thiếu deviceId, không hỗ trợ logout toàn bộ thiết bị, không hỗ trợ quản lý phiên theo thiết
bị và không có cơ chế tự động dọn dẹp token hết hạn.

Giải pháp cải tiến là bổ sung trường `deviceId` vào RefreshToken, hỗ trợ hai cơ chế logout (theo thiết bị và toàn bộ
thiết bị), đồng thời xây dựng cơ chế tự động xóa Refresh Token hết hạn. Những cải tiến này giúp nâng cao bảo mật, tối ưu
tài nguyên hệ thống và mang lại trải nghiệm quản lý phiên làm việc chuyên nghiệp hơn cho người dùng.

# Phần 2 - Đề xuất cơ chế tự động dọn dẹp Refresh Token hết hạn

## 1. Mục tiêu

Trong quá trình hoạt động, hệ thống liên tục tạo mới Refresh Token khi người dùng đăng nhập hoặc làm mới phiên đăng
nhập. Theo thời gian, số lượng Refresh Token đã hết hạn hoặc đã bị thu hồi sẽ tăng lên đáng kể.

Nếu không có cơ chế dọn dẹp, cơ sở dữ liệu sẽ gặp các vấn đề:

- Tăng dung lượng lưu trữ không cần thiết.
- Làm giảm hiệu năng truy vấn.
- Tăng thời gian sao lưu (backup) dữ liệu.
- Gây khó khăn cho việc quản lý bảng Refresh Token.

Vì vậy cần xây dựng cơ chế tự động loại bỏ các Refresh Token không còn giá trị sử dụng.

---

## 2. Nguyên tắc dọn dẹp

Một Refresh Token được xem là không còn giá trị khi:

### Trường hợp 1: Token đã hết hạn

```java
refreshToken.getExpiresAt().before(new Date())
```

Ví dụ:

```text
Current Time: 2026-06-04 10:00

Token Expired At:
2026-06-03 22:00
```

Token này không thể tiếp tục được sử dụng để cấp Access Token mới.

---

### Trường hợp 2: Token đã bị thu hồi

```java
refreshToken.getRevoked() ==true
```

Ví dụ:

Người dùng đã logout hoặc quản trị viên đã vô hiệu hóa phiên đăng nhập.

---

## 3. Thiết kế Repository

Bổ sung các phương thức hỗ trợ dọn dẹp dữ liệu.

### Xóa các token đã hết hạn

```java
@Modifying
@Query("""
    delete from RefreshToken r
    where r.expiresAt < CURRENT_TIMESTAMP
""")
int deleteExpiredTokens();
```

Ý nghĩa:

- Xóa toàn bộ Refresh Token đã hết hạn.
- Trả về số lượng bản ghi bị xóa.

---

### Xóa token đã bị thu hồi

```java
@Modifying
@Query("""
    delete from RefreshToken r
    where r.revoked = true
""")
int deleteRevokedTokens();
```

Ý nghĩa:

- Xóa các token đã bị vô hiệu hóa.
- Giảm dung lượng dữ liệu lưu trữ.

---

## 4. Thiết kế Service dọn dẹp

Có thể xây dựng riêng một service chuyên xử lý việc dọn dẹp dữ liệu.

Ví dụ:

```java
@Service
public class RefreshTokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenCleanupService(
            RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens();
    }
}
```

---

## 5. Luồng xử lý đề xuất

### Bước 1

Service được gọi theo một cơ chế định kỳ hoặc từ một tiến trình nền.

---

### Bước 2

Tìm tất cả Refresh Token đã hết hạn.

```java
deleteExpiredTokens();
```

---

### Bước 3

Thực hiện xóa khỏi database.

```java
DELETE FROM
refresh_token
WHERE expires_at <NOW();
```

---

### Bước 4

Ghi log phục vụ theo dõi.

Ví dụ:

```text
[INFO] Deleted 523 expired refresh tokens
```

---

## 6. Đề xuất cải tiến nâng cao

Thay vì xóa ngay khi token vừa hết hạn, có thể giữ lại trong một khoảng thời gian ngắn để phục vụ kiểm tra lịch sử đăng
nhập.

Ví dụ:

```java
delete token
where expires_at < NOW() - 30 days
```

Lợi ích:

- Hỗ trợ điều tra sự cố bảo mật.
- Theo dõi lịch sử đăng nhập.
- Vẫn đảm bảo dữ liệu cũ được loại bỏ định kỳ.

---

## 7. Phác thảo lời gọi hàm

Ví dụ luồng gọi:

```java
public class RefreshTokenCleanupService {

    public void cleanup() {

        int expiredCount =
                refreshTokenRepository.deleteExpiredTokens();

        int revokedCount =
                refreshTokenRepository.deleteRevokedTokens();

        log.info(
            "Deleted {} expired tokens and {} revoked tokens",
            expiredCount,
            revokedCount
        );
    }
}
```

Luồng hoạt động:

```text
Cleanup Service
       |
       v
Delete Expired Tokens
       |
       v
Delete Revoked Tokens
       |
       v
Write Log
       |
       v
Finish
```

---

## 8. Kết luận

Cơ chế tự động dọn dẹp Refresh Token giúp duy trì hiệu năng và tính ổn định của hệ thống xác thực. Ý tưởng triển khai là
xây dựng một service chuyên trách thực hiện việc tìm kiếm và xóa các Refresh Token đã hết hạn hoặc đã bị thu hồi, sau đó
ghi nhận kết quả để theo dõi. Cách tiếp cận này giúp giảm dung lượng lưu trữ, tối ưu truy vấn cơ sở dữ liệu và đảm bảo
bảng Refresh Token luôn chứa các phiên đăng nhập còn hiệu lực.