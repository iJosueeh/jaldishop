package com.jaldishop.backend.identity.application;

import com.jaldishop.backend.identity.domain.User;
import com.jaldishop.backend.identity.domain.UserRepository;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetAdminUsersService {

    private final UserRepository userRepository;

    public GetAdminUsersService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> execute(GetAdminUsersQuery query) {
        return userRepository.findAll(query.query(), query.role(), query.status());
    }

    public User execute(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + userId));
    }
}
