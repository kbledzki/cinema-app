package com.cinema.users.application;

import com.cinema.shared.events.EventPublisher;
import com.cinema.users.application.dto.CreateUserDto;
import com.cinema.users.application.dto.SetNewUserPasswordDto;
import com.cinema.users.domain.*;
import com.cinema.users.domain.exceptions.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserFactory userFactory;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventPublisher eventPublisher;

    public void createUser(CreateUserDto dto) {
        User user = userFactory.createUser(dto.mail(), dto.password(), UserRole.COMMON);
        userRepository.add(user);
    }

    public void createAdmin(CreateUserDto dto) {
        if (!userRepository.existsByMail(dto.mail())) {
            User admin = userFactory.createUser(dto.mail(), dto.password(), UserRole.ADMIN);
            userRepository.add(admin);
            log.info("Admin added");
        }
    }

    public void resetUserPassword(String mail) {
        User user = userRepository
                .getByMail(mail)
                .orElseThrow(UserNotFoundException::new);
        UUID passwordResetToken = UUID.randomUUID();
        user.setPasswordResetToken(passwordResetToken);
        userRepository.add(user);
        UserPasswordResetEvent userPasswordResetEvent = new UserPasswordResetEvent(mail, passwordResetToken);
        eventPublisher.publish(userPasswordResetEvent);
    }

    public void setNewUserPassword(SetNewUserPasswordDto dto) {
        User user = userRepository
                .getByPasswordResetToken(dto.passwordResetToken())
                .orElseThrow(UserNotFoundException::new);
        String encodedPassword = passwordEncoder.encode(dto.newPassword());
        user.setNewPassword(encodedPassword);
        userRepository.add(user);
    }

    public User getLoggedUser() {
        String mail = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();
        return userRepository
                .getByMail(mail)
                .orElseThrow(() -> new UsernameNotFoundException(mail));
    }
}
