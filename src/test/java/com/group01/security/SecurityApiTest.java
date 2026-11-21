package com.group01.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
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

    private String memberEmail;
    private String adminEmail;
    private String userAdminEmail;
    private String lockedEmail;
    private Long categoryId;

    private User user(String email, UserRole role, UserStatus status) {
        User u = new User();
        u.setFullName("Test " + role);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(PASSWORD));
        u.setPhone("0901234567");
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
        branches.save(b);
        for (int i = 1; i <= 3; i++) {
            Vehicle v = new Vehicle();
            v.setVehicleCode("SEC-" + n % 100000 + "-" + i);
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
