package com.group01.security;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.entity.Administrator;
import com.group01.entity.Branch;
import com.group01.entity.Category;
import com.group01.entity.User;
import com.group01.entity.Vehicle;
import com.group01.enums.UserRole;
import com.group01.enums.UserStatus;
import com.group01.repository.AdministratorRepository;
import com.group01.repository.BranchRepository;
import com.group01.repository.CategoryRepository;
import com.group01.repository.UserRepository;
import com.group01.repository.VehicleRepository;
import com.group01.support.RequiresPostgres;
import com.group01.service.StripeCheckoutGateway;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Guest / Member / Admin rules, JSON errors and the catalogue API end to end (needs PostgreSQL). */
@RequiresPostgres
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityApiTest {

    static final String PASSWORD = "Secret@123";

    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;
    @Autowired UserRepository users;
    @Autowired AdministratorRepository admins;
    @Autowired CategoryRepository categories;
    @Autowired BranchRepository branches;
    @Autowired VehicleRepository vehicles;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @MockitoBean StripeCheckoutGateway stripe;

    private String memberEmail;
    private String adminEmail;
    private String userAdminEmail;
    private String lockedEmail;
    private Long categoryId;
    private Integer branchId;
    private String vehicleCode;

    private User user(String email, UserRole role, UserStatus status) {
        User u = new User();
        u.setFullName("Test " + role);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(PASSWORD));
        u.setPhone("0901234567");
        u.setLicenseNumber("079200001111");
        u.setRole(role);
        u.setStatus(status);
        return users.save(u);
    }

    @BeforeEach
    void data() {
        long n = System.nanoTime();
        memberEmail = "member-" + n + "@mail.com";
        adminEmail = "admin-" + n + "@morrow.vn";
        userAdminEmail = "staff-" + n + "@morrow.vn";
        lockedEmail = "locked-" + n + "@mail.com";
        user(memberEmail, UserRole.MEMBER, UserStatus.ACTIVE);
        user(userAdminEmail, UserRole.ADMIN, UserStatus.ACTIVE);
        user(lockedEmail, UserRole.MEMBER, UserStatus.INACTIVE);
        Administrator a = new Administrator();
        a.setFullName("Admin");
        a.setEmail(adminEmail);
        a.setPasswordHash(encoder.encode(PASSWORD));
        admins.save(a);

        Category c = new Category();
        c.setCategoryName("Cat-" + n);
        categoryId = categories.save(c).getCategoryId();
        Branch b = new Branch();
        b.setBranchName("Branch-" + n);
        b.setAddress("Somewhere");
        branchId = branches.save(b).getBranchId();
        for (int i = 1; i <= 3; i++) {
            Vehicle v = new Vehicle();
            v.setVehicleCode("SEC-" + n % 100000 + "-" + i);
            if (i == 1) vehicleCode = v.getVehicleCode();
            v.setCategory(c);
            v.setBranch(b);
            v.setName(i == 1 ? "Honda Vision" : "Yamaha Exciter " + i);
            v.setShortDescription("100% fun_ride");
            v.setDailyPrice(new BigDecimal(100000 * i));
            vehicles.save(v);
        }
    }

    // ------------------------------------------------------------------ guest
    @Test
    void guestCanReadHealthAndCatalogue() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk());
        mvc.perform(get("/api/categories")).andExpect(status().isOk());
        mvc.perform(get("/api/branches")).andExpect(status().isOk());
        mvc.perform(get("/api/vehicles").param("categoryId", String.valueOf(categoryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].vehicleCode").exists());
    }

    @Test
    void guestIsRejectedWithJson401OnProtectedEndpoints() throws Exception {
        mvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/me"));
        mvc.perform(get("/api/member/ping")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/ping")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/database")).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ member
    @Test
    void memberCanUseMemberAreaButNotAdminArea() throws Exception {
        mvc.perform(get("/api/me").with(httpBasic(memberEmail, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(memberEmail))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_MEMBER"));
        mvc.perform(get("/api/member/ping").with(httpBasic(memberEmail, PASSWORD))).andExpect(status().isOk());
        mvc.perform(get("/api/admin/ping").with(httpBasic(memberEmail, PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        mvc.perform(get("/api/database").with(httpBasic(memberEmail, PASSWORD))).andExpect(status().isForbidden());
    }

    @Test
    void wrongPasswordUnknownUserAndLockedUserAreRejected() throws Exception {
        mvc.perform(get("/api/me").with(httpBasic(memberEmail, "wrong"))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me").with(httpBasic("nobody@mail.com", PASSWORD))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me").with(httpBasic(lockedEmail, PASSWORD))).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ admin
    @Test
    void administratorsFromBothTablesGetAdminRole() throws Exception {
        for (String email : new String[] {adminEmail, userAdminEmail}) {
            mvc.perform(get("/api/admin/ping").with(httpBasic(email, PASSWORD))).andExpect(status().isOk());
            mvc.perform(get("/api/me").with(httpBasic(email, PASSWORD)))
                    .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"));
            mvc.perform(get("/api/database").with(httpBasic(email, PASSWORD))).andExpect(status().isOk());
        }
    }

    @Test
    void adminIsNotAllowedInMemberArea() throws Exception {
        mvc.perform(get("/api/member/ping").with(httpBasic(adminEmail, PASSWORD))).andExpect(status().isForbidden());
    }

    @Test
    void administratorCanSignInFromTheForm() throws Exception {
        var login = mvc.perform(post("/login").param("email", adminEmail).param("password", PASSWORD))
                .andExpect(redirectedUrl("/admin/bookings")).andReturn();
        var session = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(get("/admin/bookings").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Manage bookings")));
        mvc.perform(get("/profile").session(session)).andExpect(status().isForbidden());
    }

    @Test
    void memberCanSaveTheirProfileFromTheForm() throws Exception {
        var login = mvc.perform(post("/login").param("email", memberEmail).param("password", PASSWORD))
                .andExpect(redirectedUrl("/")).andReturn();
        var session = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(post("/profile/update").session(session)
                        .param("fullName", "Updated Member").param("phone", "0901112223")
                        .param("dateOfBirth", "2000-01-15").param("gender", "Female")
                        .param("address", "District 1").param("licenseNumber", "079200001111"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Profile updated successfully.")));
        mvc.perform(get("/profile").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Updated Member")))
                .andExpect(content().string(containsString("079200001111")));
    }

    private Long booking(String status, String method) {
        Long userId = users.findByEmail(memberEmail).orElseThrow().getUserId();
        return jdbc.queryForObject("""
                INSERT INTO bookings (user_id, pickup_branch_id, return_branch_id, pickup_location,
                    pickup_date, return_date, renter_name, renter_phone, renter_license_number,
                    total_amount, deposit_amount, payment_method, status)
                VALUES (?, ?, ?, 'Branch', ?, ?, 'Member', '0901234567', '079200001111', 1000000, 300000, ?, ?)
                RETURNING booking_id
                """, Long.class, userId, branchId, branchId, LocalDate.now(), LocalDate.now().plusDays(3), method, status);
    }

    @Test
    void earlyReturnIsRequestedByMemberAndRecordedByAdmin() throws Exception {
        Long id = booking("Picked up", "AT_PICKUP");
        String path = "/bookings/" + id;
        mvc.perform(get(path).with(httpBasic(memberEmail, PASSWORD)))
                .andExpect(content().string(containsString("Request early return")));
        mvc.perform(post(path + "/early-return").with(httpBasic(memberEmail, PASSWORD))
                        .param("date", LocalDate.now().toString()))
                .andExpect(redirectedUrl(path));
        mvc.perform(get("/admin" + path).with(httpBasic(adminEmail, PASSWORD)))
                .andExpect(content().string(containsString("Confirm vehicle returned")));
        mvc.perform(post("/admin" + path + "/status").with(httpBasic(adminEmail, PASSWORD))
                        .param("status", "COMPLETED"))
                .andExpect(redirectedUrl("/admin" + path));
        mvc.perform(get("/api" + path).with(httpBasic(memberEmail, PASSWORD)))
                .andExpect(jsonPath("$.booking.status").value("Completed"))
                .andExpect(jsonPath("$.booking.returnDate").value(LocalDate.now().plusDays(3).toString()))
                .andExpect(jsonPath("$.booking.requestedReturnDate").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.booking.actualReturnDate").value(LocalDate.now().toString()));
    }

    @Test
    void onlineDepositRequiresStripeToVerifyTheExactAmount() throws Exception {
        vehicles.flush();
        when(stripe.enabled()).thenReturn(true);
        var dates = json.createObjectNode().put("vehicleCode", vehicleCode)
                .put("pickupDate", LocalDate.now().plusDays(1).toString())
                .put("returnDate", LocalDate.now().plusDays(3).toString());
        mvc.perform(post("/api/carts/me/items").with(httpBasic(memberEmail, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(dates.toString()))
                .andExpect(status().isOk());
        var checkout = mvc.perform(post("/api/checkout").with(httpBasic(memberEmail, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                            {"renterName":"Member","renterPhone":"0901234567","renterLicenseNumber":"079200001111",
                             "pickupLocation":"Branch","paymentMethod":"ONLINE"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentMethod").value("ONLINE"))
                .andExpect(jsonPath("$.paymentStatus").value("Pending"))
                .andReturn();
        Long id = json.readTree(checkout.getResponse().getContentAsString()).path("bookingId").asLong();
        String path = "/bookings/" + id;
        when(stripe.create(eq(id), eq(60000L), anyString())).thenReturn(json.createObjectNode()
                .put("id", "cs_test_morrow").put("url", "https://checkout.stripe.com/c/pay/cs_test_morrow"));
        mvc.perform(get(path).with(httpBasic(memberEmail, PASSWORD)))
                .andExpect(content().string(containsString("Open Stripe sandbox checkout")));
        mvc.perform(post(path + "/payment").with(httpBasic(memberEmail, PASSWORD)))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "https://checkout.stripe.com/c/pay/cs_test_morrow"));

        var session = json.createObjectNode().put("id", "cs_test_morrow").put("payment_status", "paid")
                .put("currency", "vnd");
        when(stripe.retrieve("cs_test_morrow")).thenReturn(session.put("amount_total", 1));
        mvc.perform(get(path + "/payment/return").with(httpBasic(memberEmail, PASSWORD)))
                .andExpect(redirectedUrl(path));
        mvc.perform(get(path).with(httpBasic(memberEmail, PASSWORD)))
                .andExpect(content().string(containsString("Open Stripe sandbox checkout")));

        session.put("amount_total", 60000);
        mvc.perform(get(path + "/payment/return").with(httpBasic(memberEmail, PASSWORD)))
                .andExpect(redirectedUrl(path));
        mvc.perform(get("/admin" + path).with(httpBasic(adminEmail, PASSWORD)))
                .andExpect(content().string(containsString("Sandbox deposit status: <b>Paid</b>")));
    }

    // ------------------------------------------------------------------ catalogue + error format
    @Test
    void keywordSearchIsCaseInsensitiveAndLikeWildcardsAreLiteral() throws Exception {
        mvc.perform(get("/api/vehicles").param("keyword", "HONDA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/vehicles").param("keyword", "%")).andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/api/vehicles").param("keyword", "zz%zz")).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/vehicles").param("keyword", "' OR 1=1;--")).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void sortingAndPaging() throws Exception {
        mvc.perform(get("/api/vehicles").param("categoryId", String.valueOf(categoryId))
                        .param("sort", "price").param("order", "desc").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].dailyPrice").value(300000.0));
    }

    @Test
    void invalidParametersReturnJson400() throws Exception {
        mvc.perform(get("/api/vehicles").param("sort", "colour"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Sort field must be name, price, discount or rentals"));
        mvc.perform(get("/api/vehicles").param("status", "Broken")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/vehicles").param("order", "sideways")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/vehicles").param("categoryId", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'categoryId'"));
    }

    @Test
    void unknownVehicleReturnsJson404AndKnownVehicleReturnsDetail() throws Exception {
        mvc.perform(get("/api/vehicles/DOES-NOT-EXIST"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Vehicle not found: DOES-NOT-EXIST"));
        String code = vehicles.findAll().stream().filter(v -> v.getName().equals("Honda Vision")).findFirst().orElseThrow().getVehicleCode();
        mvc.perform(get("/api/vehicles/" + code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Honda Vision"))
                .andExpect(jsonPath("$.status").value("Available"))
                .andExpect(jsonPath("$.category.categoryId").value(categoryId));
    }
}
