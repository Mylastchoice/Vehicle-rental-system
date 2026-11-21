package com.group01.dto;

import java.util.List;

public record CurrentUserResponse(String email, List<String> roles) {
}
