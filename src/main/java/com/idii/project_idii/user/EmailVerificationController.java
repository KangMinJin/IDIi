package com.idii.project_idii.user;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/user/email")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;
    private final UserService userService;

    public EmailVerificationController(
            EmailVerificationService emailVerificationService,
            UserService userService) {

        this.emailVerificationService = emailVerificationService;
        this.userService = userService;
    }

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> send(
            @RequestParam("email") String email,
            HttpSession session) {

        String normalizedEmail = email.trim();

        if (normalizedEmail.isEmpty()
                || normalizedEmail.length() > 254
                || !normalizedEmail.matches(
                    "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {

            return response(false, "올바른 이메일을 입력해 주세요.");
        }

        try {
            if (userService.countUserByEmail(normalizedEmail) > 0) {
                return response(false, "이미 가입된 이메일입니다.");
            }

            boolean sent = emailVerificationService.sendCode(
                    normalizedEmail, session);

            if (!sent) {
                return response(false,
                        "발송할 수 없습니다. 잠시 후 다시 시도해 주세요.");
            }

            return response(true, "인증번호를 발송했습니다.");

        } catch (MailException e) {
            return response(false,
                    "메일 발송에 실패했습니다. 잠시 후 다시 시도해 주세요.");

        } catch (Exception e) {
            e.printStackTrace();
            return response(false, "처리 중 오류가 발생했습니다.");
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(
            @RequestParam("email") String email,
            @RequestParam("code") String code,
            HttpSession session) {

        boolean verified = emailVerificationService.verifyCode(
                email.trim(), code, session);

        if (!verified) {
            return response(false,
                    "인증번호가 올바르지 않거나 만료되었습니다.");
        }

        return response(true, "이메일 인증이 완료되었습니다.");
    }

    private ResponseEntity<Map<String, Object>> response(
            boolean success, String message) {

        return ResponseEntity.ok(
                Map.of(
                        "success", success,
                        "message", message
                )
        );
    }
}
