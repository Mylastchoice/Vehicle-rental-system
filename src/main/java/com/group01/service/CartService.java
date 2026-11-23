
package com.group01.service;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.group01.repository.CartRepository;

@Service
public class CartService {

    private final CartRepository repository;
    private final AvailabilityService availabilityService;

    public CartService(
            CartRepository repository,
            AvailabilityService availabilityService) {

        this.repository = repository;
        this.availabilityService = availabilityService;
    }

    private void checkUser(Long userId) {

        if (userId == null || userId <= 0
                || !repository.userExists(userId)) {// kiểm tra xem người dùng có tồn tại trong cơ sở dữ liệu hay không, nếu không thì ném ra lỗi 404 Not Found

            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "User not found"
            );// ném ra lỗi 404 Not Found nếu người dùng không tồn tại
        }
    }

    @Transactional// thêm xe vào giỏ hàng của người dùng, nếu xe đã có trong giỏ hàng thì cập nhật ngày thuê và ngày trả
    public Map<String, Object> addItem(
            Long userId,
            String vehicleCode,
            LocalDate pickupDate,
            LocalDate returnDate) {

        checkUser(userId);

        // Su dung lai UC14 de kiem tra xe
        boolean available = availabilityService
            .checkAvailability(
                vehicleCode,
                pickupDate,
                returnDate
            );

        if (!available) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Vehicle is not available"
            );// ném ra lỗi 409 Conflict nếu xe không khả dụng trong khoảng thời gian thuê/trả
        }

        Long cartId = repository.getOrCreateCart(userId);

        repository.saveItem(
            cartId,
            vehicleCode,
            pickupDate,
            returnDate
        );// thêm xe vào giỏ hàng hoặc cập nhật ngày thuê và ngày trả nếu xe đã có trong giỏ hàng

        return getCart(userId);
    }

    @Transactional(readOnly = true)// lấy thông tin giỏ hàng của người dùng, bao gồm danh sách các dòng trong giỏ hàng
    public Map<String, Object> getCart(Long userId) {

        checkUser(userId);// kiểm tra xem người dùng có tồn tại trong cơ sở dữ liệu hay không, nếu không thì ném ra lỗi 404 Not Found

        return Map.of(
            "userId", userId,
            "items", repository.getItems(userId)
        );
    }// trả về thông tin giỏ hàng dưới dạng Map, bao gồm userId và danh sách các dòng trong giỏ hàng

    @Transactional// cập nhật ngày thuê và ngày trả của một dòng trong giỏ hàng, nếu xe không khả dụng trong khoảng thời gian mới thì ném ra lỗi 409 Conflict
    public Map<String, Object> updateDates(Long userId, Long itemId,
            LocalDate pickupDate, LocalDate returnDate) {
        checkUser(userId);
        String vehicleCode = repository.findVehicleCodeForItem(userId, itemId);
        if (vehicleCode == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found");
        }
        if (!availabilityService.checkAvailability(vehicleCode, pickupDate, returnDate)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vehicle is not available");
        }
        if (repository.updateDates(userId, itemId, pickupDate, returnDate) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found");
        }
        return getCart(userId);
    }// cập nhật ngày thuê và ngày trả của một dòng trong giỏ hàng, nếu xe không khả dụng trong khoảng thời gian mới thì ném ra lỗi 409 Conflict, nếu dòng giỏ hàng không tồn tại thì ném ra lỗi 404 Not Found

    @Transactional// xóa một dòng trong giỏ hàng của người dùng, nếu dòng giỏ hàng không tồn tại thì ném ra lỗi 404 Not Found
    public void removeItem(Long userId, Long itemId) {

        checkUser(userId);

        int deleted = repository.deleteItem(userId, itemId);

        if (deleted == 0) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Cart item not found"
            );// ném ra lỗi 404 Not Found nếu dòng giỏ hàng không tồn tại
        }
    }
}

