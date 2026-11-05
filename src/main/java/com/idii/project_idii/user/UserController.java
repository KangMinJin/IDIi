package com.idii.project_idii.user;

import com.idii.project_idii.main.MainController;

import java.net.http.HttpRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RequestMapping("/user")
@Controller
public class UserController {
	
	private final MainController mainController;
	private final UserService userService;
	public UserController(UserService userService, MainController mainController) {
		this.userService = userService;
		this.mainController = mainController;
	}

	@GetMapping("/signup")
    public String signup() {
        return "user/signup";
    }
	
	@PostMapping("/signup")
	public String signupPro(HttpServletRequest request, Model model) {

	    String email = request.getParameter("email");
	    String password = request.getParameter("password");
	    String nickname = request.getParameter("nickname");
	    String passwordConfirm = request.getParameter("passwordConfirm");

	    if (email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
	        model.addAttribute("error", "올바른 이메일 형식으로 입력해 주세요.");
	        return "user/signup";
	    }

	    if (password == null || password.length() < 8 || password.length() > 20) {
	        model.addAttribute("error", "비밀번호는 8~20자로 입력해 주세요.");
	        return "user/signup";
	    }

	    if (!password.equals(passwordConfirm)) {
	        model.addAttribute("error", "비밀번호가 일치하지 않습니다.");
	        return "user/signup";
	    }

	    if (nickname == null || nickname.trim().isEmpty()) {
	        model.addAttribute("error", "닉네임을 입력해 주세요.");
	        return "user/signup";
	    }

	    User user = new User();
	    user.setEmail(email.trim());
	    user.setPassword(password);
	    user.setNickname(nickname.trim());

	    try {
	        int cnt = userService.countUserByEmail(user.getEmail());

	        if (cnt > 0) {
	            model.addAttribute("error", "이미 가입된 이메일입니다.");
	            return "user/signup";
	        }

	        int result = userService.addUser(user);

	        if (result == 1) {
	            return "redirect:/user/login";
	        }

	        model.addAttribute("error", "회원가입에 실패했습니다.");

	    } catch (Exception e) {
	        e.printStackTrace();
	        model.addAttribute("error", "오류가 발생했습니다. 다시 시도해 주세요.");
	    }

	    return "user/signup";
	}
	
	@GetMapping("/login")
    public String login() {
        return "user/login";
    }
	
	@PostMapping("/login")
	public String loginPro(HttpServletRequest request, HttpSession session, Model model) {
		String email = request.getParameter("email");
		String pw = request.getParameter("password");
		
		try {
			User user = userService.login(email, pw);
			
			if(user != null) {
				request.changeSessionId();
				session.setAttribute("userId", user.getUserId());
				
				return "redirect:/";
			}
			
			model.addAttribute("error", "이메일 또는 비밀번호가 일치하지 않습니다.");
			
		} catch (Exception e) {
			e.printStackTrace();
			model.addAttribute("error", "오류가 발생했습니다. 다시 시도해 주세요.");
		}
		
		return "user/login";		
	}
	
	@PostMapping("/logout")
	public String logout(HttpSession session) {
		session.invalidate();
		return "redirect:/";
	}

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/user/login";
        try {
            User user = userService.findActiveUser(userId);
            if (user == null) { session.invalidate(); return "redirect:/user/login"; }
            model.addAttribute("user", user);
            return "user/profile";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "회원정보를 불러오지 못했습니다.");
            return "user/profile";
        }
    }

    @PostMapping("/profile")
    public String updateProfile(HttpServletRequest request, HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/user/login";
        String nickname = request.getParameter("nickname");
        String currentPassword = request.getParameter("currentPassword");
        String newPassword = request.getParameter("newPassword");
        String newPasswordConfirm = request.getParameter("newPasswordConfirm");
        String error = null;
        if (nickname == null || nickname.isBlank() || nickname.trim().length() > 50) {
            error = "닉네임은 1~50자로 입력해 주세요.";
        } else if (currentPassword == null || currentPassword.isBlank()) {
            error = "현재 비밀번호를 입력해 주세요.";
        } else if (newPassword != null && !newPassword.isEmpty() &&
                (newPassword.length() < 8 || newPassword.length() > 72)) {
            error = "새 비밀번호는 8~72자로 입력해 주세요.";
        } else if (newPassword != null && !newPassword.equals(newPasswordConfirm)) {
            error = "새 비밀번호 확인이 일치하지 않습니다.";
        }
        try {
            User user = userService.findActiveUser(userId);
            if (user == null) { session.invalidate(); return "redirect:/user/login"; }
            if (error == null) {
                boolean updated = userService.updateProfile(userId, currentPassword, nickname.trim(), newPassword);
                if (updated) return "redirect:/user/profile?success=1";
                error = "현재 비밀번호가 올바르지 않습니다.";
            }
            user.setNickname(nickname == null ? "" : nickname);
            model.addAttribute("user", user);
            model.addAttribute("error", error);
            return "user/profile";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "수정 중 오류가 발생했습니다.");
            return "user/profile";
        }
    }

    @PostMapping("/withdraw")
    public String withdraw(HttpServletRequest request, HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/user/login";
        String password = request.getParameter("withdrawPassword");
        String confirmation = request.getParameter("withdrawConfirm");
        try {
            User user = userService.findActiveUser(userId);
            if (user == null) { session.invalidate(); return "redirect:/user/login"; }
            model.addAttribute("user", user);
            if (!"yes".equals(confirmation)) {
                model.addAttribute("withdrawError", "탈퇴 안내를 확인해 주세요.");
                return "user/profile";
            }
            if (password == null || password.isBlank()) {
                model.addAttribute("withdrawError", "현재 비밀번호를 입력해 주세요.");
                return "user/profile";
            }
            if (userService.hasOwnedProjects(userId)) {
                model.addAttribute("withdrawError", "소유한 프로젝트가 있습니다. 프로젝트를 삭제한 뒤 탈퇴할 수 있습니다.");
                return "user/profile";
            }
            if (!userService.withdraw(userId, password)) {
                model.addAttribute("withdrawError", "비밀번호가 올바르지 않거나 탈퇴할 수 없는 상태입니다.");
                return "user/profile";
            }
            session.invalidate();
            return "redirect:/user/login?withdrawn=1";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("withdrawError", "탈퇴 처리 중 오류가 발생했습니다.");
            try { model.addAttribute("user", userService.findActiveUser(userId)); }
            catch (Exception ignored) { }
            return "user/profile";
        }
    }

}
