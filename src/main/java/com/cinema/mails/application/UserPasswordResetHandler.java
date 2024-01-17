package com.cinema.mails.application;

import com.cinema.mails.domain.Mail;
import com.cinema.mails.domain.MailSender;
import com.cinema.mails.domain.MailType;
import com.cinema.users.domain.UserPasswordResetEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@Profile("prod")
@RequiredArgsConstructor
@Slf4j
class UserPasswordResetHandler {

    private final MailSender mailSender;

    @EventListener
    void handle(UserPasswordResetEvent event) {
        String subject = "Password reset";
        String message = "Your password reset token: " + event.token();
        Mail passwordResetMail = new Mail(
                event.mail(),
                subject,
                message,
                MailType.USER_PASSWORD_RESET
        );
        mailSender.sendMail(passwordResetMail);
        log.info("Mail sent");
    }
}
