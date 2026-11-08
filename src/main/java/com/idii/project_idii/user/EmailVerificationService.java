package com.idii.project_idii.user;

import java.security.SecureRandom;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpSession;

@Service
public class EmailVerificationService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final long EXPIRE_SECONDS = 300;
    private static final long RESEND_SECONDS = 300;
    private static final int MAX_ATTEMPTS = 5;

    private final JavaMailSender mailSender;
    private final String senderEmail;

    public EmailVerificationService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username}") String senderEmail) {

        this.mailSender = mailSender;
        this.senderEmail = senderEmail;
    }

    public synchronized boolean sendCode(
            String email, HttpSession session) {

        if (email == null
                || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return false;
        }

        Instant now = Instant.now();

        Instant lastSent = (Instant) session.getAttribute("emailLastSent");

        if (lastSent != null
                && now.isBefore(lastSent.plusSeconds(RESEND_SECONDS))) {
            return false;
        }

        String code = String.format("%06d", RANDOM.nextInt(1000000));

		
		try {
		    MimeMessage message = mailSender.createMimeMessage();

			MimeMessageHelper helper =
			        new MimeMessageHelper(message, true, "UTF-8");

		    helper.setFrom(senderEmail, "IDI! 2.0");
		    helper.setTo(email);
		    helper.setSubject("[IDI!] 이메일 인증번호 안내");
		
		    String html = """
		        <!DOCTYPE html>
		        <html lang="ko">
		        <body style="margin:0;padding:32px 16px;
		                     background-color:#f3f5fb;
		                     font-family:Arial,sans-serif;">
		
		            <div style="max-width:520px;margin:0 auto;
		                        background:#ffffff;
		                        border-radius:16px;
		                        overflow:hidden;">
		
		                <div style="background:#5B61E0;
		                            padding:28px 24px;
		                            text-align:center;">
		
		                    <h1 style="margin:0;color:#ffffff;
		                               font-size:28px;">
		                        IDI! QA Task Management
		                    </h1>
		
		                    <p style="margin:8px 0 0;
		                              color:#cbd0ed;font-size:13px;">
		                        함께 완성하는 프로젝트
		                    </p>
		                </div>
		
		                <div style="padding:32px 28px;">
		
		                    <h2 style="margin:0 0 20px;
		                               color:#292f50;font-size:22px;">
		                        이메일 인증 안내
		                    </h2>
		
		                    <p style="color:#515a73;font-size:14px;
		                              line-height:1.8;">
		                        안녕하세요!<br>
		                        IDI! 회원가입을 위한
		                        인증번호를 안내해 드립니다.
		                    </p>
		
		                    <div style="background:#f0f1ff;
		                                border-radius:12px;
		                                text-align:center;
		                                padding:26px 16px;
		                                margin:24px 0;">
		
		                        <p style="margin:0 0 10px;
		                                  color:#626a96;
		                                  font-size:13px;">
		                            인증번호
		                        </p>
		
		                        <strong style="font-size:34px;
		                                       letter-spacing:7px;
		                                       color:#555ee4;">
		                            %s
		                        </strong>
		
		                        <p style="margin:12px 0 0;
		                                  color:#626a96;
		                                  font-size:12px;">
		                            유효시간 5분
		                        </p>
		                    </div>
		
		                    <p style="color:#515a73;font-size:13px;
		                              line-height:1.8;">
		                        회원가입 화면에 인증번호를 입력해 주세요.<br>
		                        본인이 요청하지 않았다면
		                        이 메일을 무시하셔도 됩니다.
		                    </p>
		                </div>
		
		                <div style="border-top:1px solid #e7e9f2;
		                            padding:20px;text-align:center;
		                            color:#9097aa;font-size:11px;">
		                    IDI! QA Task Management<br>
		                    본 메일은 자동 발송되었습니다.
		                </div>
		            </div>
		        </body>
		        </html>
		        """.formatted(code);
		
		    helper.setText(
		            "IDI! 인증번호: " + code
		                    + "\n유효시간: 5분",
		            html
		    );
		
		    mailSender.send(message);
		
		} catch (Exception e) {
		    throw new IllegalStateException("인증 메일 발송 실패", e);
		}

        session.setAttribute("emailVerificationAddress", email);
        session.setAttribute("emailVerificationCode", code);
        session.setAttribute("emailVerificationExpires",
                now.plusSeconds(EXPIRE_SECONDS));
        session.setAttribute("emailVerificationAttempts", 0);
        session.setAttribute("emailVerificationCompleted", false);
        session.setAttribute("emailLastSent", now);

        return true;
    }

    public boolean verifyCode(
            String email, String code, HttpSession session) {

        String savedEmail = (String) session.getAttribute(
                "emailVerificationAddress");
        String savedCode = (String) session.getAttribute(
                "emailVerificationCode");
        Instant expires = (Instant) session.getAttribute(
                "emailVerificationExpires");

        Integer attempts = (Integer) session.getAttribute(
                "emailVerificationAttempts");

        if (savedEmail == null || savedCode == null || expires == null) {
            return false;
        }

        if (Instant.now().isAfter(expires)) {
            clearVerification(session);
            return false;
        }

        int count = attempts == null ? 0 : attempts;

        if (count >= MAX_ATTEMPTS) {
            clearVerification(session);
            return false;
        }

        if (email == null || !savedEmail.equals(email)
                || code == null || !savedCode.equals(code)) {

            session.setAttribute("emailVerificationAttempts", count + 1);
            return false;
        }

        session.setAttribute("emailVerificationCompleted", true);
        session.removeAttribute("emailVerificationCode");

        return true;
    }

    public boolean isVerified(String email, HttpSession session) {

        String savedEmail = (String) session.getAttribute(
                "emailVerificationAddress");

        Boolean completed = (Boolean) session.getAttribute(
                "emailVerificationCompleted");

        Instant expires = (Instant) session.getAttribute(
                "emailVerificationExpires");

        return email != null
                && email.equals(savedEmail)
                && Boolean.TRUE.equals(completed)
                && expires != null
                && Instant.now().isBefore(expires);
    }

    public void clearVerification(HttpSession session) {

        session.removeAttribute("emailVerificationAddress");
        session.removeAttribute("emailVerificationCode");
        session.removeAttribute("emailVerificationExpires");
        session.removeAttribute("emailVerificationAttempts");
        session.removeAttribute("emailVerificationCompleted");
    }
}
