package com.jaldishop.backend.identity.application;

import com.jaldishop.backend.identity.domain.User;
import com.jaldishop.backend.identity.domain.UserRepository;
import com.jaldishop.backend.identity.domain.UserStatus;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ChangeUserStatusService {

    private final UserRepository userRepository;

    public ChangeUserStatusService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User execute(ChangeUserStatusCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + command.userId()));

        if (command.targetStatus() == UserStatus.SUSPENDED) {
            user.suspend(command.currentAdminId());
        } else if (command.targetStatus() == UserStatus.ACTIVE) {
            user.activate();
        } else {
            throw new IllegalArgumentException("Estado no soportado para cambio de estado de usuario: " + command.targetStatus());
        }

        return userRepository.save(user);
    }
}
