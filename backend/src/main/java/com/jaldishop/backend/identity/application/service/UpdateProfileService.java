package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.application.command.UpdateProfileCommand;

import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UpdateProfileService {

    private final UserRepository userRepository;

    public UpdateProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User execute(UpdateProfileCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));

        user.updateProfile(command.firstName(), command.lastName(), command.phone());
        return userRepository.save(user);
    }

}
