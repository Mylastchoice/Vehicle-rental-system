package com.group01.service;

import com.group01.dto.BranchResponse;
import com.group01.dto.CategoryResponse;
import com.group01.dto.PageResponse;
import com.group01.dto.VehicleDetailResponse;
import com.group01.dto.VehicleSummaryResponse;
import com.group01.entity.Branch;
import com.group01.entity.Category;
import com.group01.entity.Vehicle;
import com.group01.enums.VehicleStatus;
import com.group01.enums.BookingStatus;
import com.group01.entity.BookingDetail;
import java.time.LocalDate;
import com.group01.exception.BadRequestException;
import com.group01.exception.ResourceNotFoundException;
import com.group01.repository.BranchRepository;
import com.group01.repository.CategoryRepository;
import com.group01.repository.VehicleRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only catalogue queries (browse / search / detail). Business rules of UC01-UC02 belong to M5. */
@Service
@Transactional(readOnly = true)
public class CatalogService {

    public static final int MAX_PAGE_SIZE = 100;
    public static final int DEFAULT_PAGE_SIZE = 30;

    private final VehicleRepository vehicleRepository;
    private final CategoryRepository categoryRepository;
    private final BranchRepository branchRepository;

    public CatalogService(VehicleRepository vehicleRepository, CategoryRepository categoryRepository,
                          BranchRepository branchRepository) {
        this.vehicleRepository = vehicleRepository;
        this.categoryRepository = categoryRepository;
        this.branchRepository = branchRepository;
    }

    public PageResponse<VehicleSummaryResponse> searchVehicles(Long categoryId, String keyword, String status,
                                                               String sort, String order, LocalDate pickupDate,
                                                               LocalDate returnDate, int page, int size) {
        if ((pickupDate == null) != (returnDate == null)) {
            throw new BadRequestException("pickupDate and returnDate must be provided together");
        }
        if (pickupDate != null && (!returnDate.isAfter(pickupDate) || pickupDate.isBefore(LocalDate.now()))) {
            throw new BadRequestException("Rental dates must be valid and returnDate must be after pickupDate");
        }
        VehicleStatus statusFilter = parseStatus(status);
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size), buildSort(sort, order));

        Specification<Vehicle> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("categoryId"), categoryId));
            }
            if (statusFilter != null) {
                predicates.add(cb.equal(root.get("status"), statusFilter));
            }
            if (pickupDate != null) {
                var subquery = query.subquery(Long.class);
                var detail = subquery.from(BookingDetail.class);
                subquery.select(cb.count(detail));
                subquery.where(
                        cb.equal(detail.get("vehicle").get("vehicleCode"), root.get("vehicleCode")),
                        cb.lessThan(detail.<LocalDate>get("pickupDate"), returnDate),
                        cb.greaterThan(detail.<LocalDate>get("returnDate"), pickupDate),
                        detail.get("booking").get("status").in(
                                BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.PICKED_UP));
                predicates.add(cb.equal(subquery, 0L));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + escapeLike(keyword.trim().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern, '\\'),
                        cb.like(cb.lower(root.get("vehicleCode")), pattern, '\\'),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("shortDescription"), "")), pattern, '\\')));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return PageResponse.of(vehicleRepository.findAll(spec, pageable).map(CatalogService::toSummary));
    }

    @org.springframework.transaction.annotation.Transactional
    public void recordVehicleView(String vehicleCode) {
        if (vehicleRepository.incrementViewCount(vehicleCode) == 0) {
            throw ResourceNotFoundException.of("Vehicle", vehicleCode);
        }
    }

    public VehicleDetailResponse getVehicle(String vehicleCode) {
        Vehicle v = vehicleRepository.findById(vehicleCode)
                .orElseThrow(() -> ResourceNotFoundException.of("Vehicle", vehicleCode));
        return new VehicleDetailResponse(v.getVehicleCode(), v.getName(), toResponse(v.getCategory()),
                toResponse(v.getBranch()), v.getLicensePlate(), v.getImageUrl(), v.getShortDescription(),
                v.getDetailedDescription(), v.getDailyPrice(), v.getDiscountRate(), v.getFinalDailyPrice(),
                v.getStatus().getDbValue(), v.getViewCount(), v.getRentalCount());
    }

    public List<CategoryResponse> listCategories() {
        return categoryRepository.findAll(Sort.by("categoryName")).stream().map(CatalogService::toResponse).toList();
    }

    public List<BranchResponse> listBranches() {
        return branchRepository.findAll(Sort.by("branchName")).stream().map(CatalogService::toResponse).toList();
    }

    // ------------------------------------------------------------------ helpers

    private static int clampSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    /** Only whitelisted fields may be used for sorting. */
    private static Sort buildSort(String sort, String order) {
        Sort.Direction direction = Sort.Direction.ASC;
        if (order != null && !order.isBlank()) {
            if ("desc".equalsIgnoreCase(order)) {
                direction = Sort.Direction.DESC;
            } else if (!"asc".equalsIgnoreCase(order)) {
                throw new BadRequestException("Sort order must be asc or desc");
            }
        }
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Order.asc("name"), Sort.Order.asc("vehicleCode"));
        }
        String property = switch (sort.toLowerCase(Locale.ROOT)) {
            case "price" -> "dailyPrice";
            case "name" -> "name";
            case "rentals" -> "rentalCount";
            case "discount" -> "discountRate";
            default -> throw new BadRequestException("Sort field must be name, price, discount or rentals");
        };
        return Sort.by(new Sort.Order(direction, property), Sort.Order.asc("vehicleCode"));
    }

    private static VehicleStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        for (VehicleStatus s : VehicleStatus.values()) {
            if (s.name().equalsIgnoreCase(status.trim()) || s.getDbValue().equalsIgnoreCase(status.trim())) {
                return s;
            }
        }
        throw new BadRequestException("Status must be Available, Rented or Maintenance");
    }

    /** '%' and '_' typed by the user must be matched literally. */
    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static VehicleSummaryResponse toSummary(Vehicle v) {
        return new VehicleSummaryResponse(v.getVehicleCode(), v.getName(), v.getCategory().getCategoryId(),
                v.getBranch().getBranchId().longValue(), v.getDailyPrice(), v.getStatus().getDbValue());
    }

    private static CategoryResponse toResponse(Category c) {
        return new CategoryResponse(c.getCategoryId(), c.getCategoryName(), c.getDescription());
    }

    private static BranchResponse toResponse(Branch b) {
        return new BranchResponse(b.getBranchId(), b.getBranchName(), b.getAddress(), b.getPhone());
    }
}
