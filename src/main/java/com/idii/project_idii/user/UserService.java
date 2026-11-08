package com.idii.project_idii.user;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
	
	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	private final UserDAO userDAO;
	
	public UserService(UserDAO userDAO) {
	    this.userDAO = userDAO;
	}
    
    public int addUser(User user) {

        if (user == null) {
            return 0;
        }

        String email = user.getEmail();
        String password = user.getPassword();
        String nickname = user.getNickname();

        // 이메일 검증
        if (email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return 0;
        }

        // 비밀번호 검증
        if(!isValidPassword(password)) {
        	return 0;
        }

        // 닉네임 검증
        if (!isValidNickname(nickname)) {
            return 0;
        }

        User newUser = new User();

        newUser.setEmail(email.trim());
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setNickname(nickname.trim());

        try {
            return userDAO.addUser(newUser);

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    public int countUserByEmail(String email) throws Exception {
        return userDAO.countUserByEmail(email);
    }
    
    public User login(String email, String pw) throws Exception {

        User user = userDAO.findByEmail(email);

        if (user == null) {
            return null;
        }

        if (!passwordEncoder.matches(pw, user.getPassword())) {
            return null;
        }

        // 비밀번호 해시가 호출자에게 전달되지 않도록 제거
        user.setPassword(null);

        return user;
    }

    // 로그인한 회원의 정보만 조회 (탈퇴 회원 제외)
    public User findActiveUser(long userId) throws Exception {
        return userDAO.findActiveUser(userId);
    }

    // 현재 비밀번호를 확인하고 닉네임 및 선택적인 새 비밀번호를 함께 수정
    public boolean updateProfile(long userId, String currentPassword, String nickname, String newPassword) throws Exception {
    	
    	// 닉네임 검증
    	if(!isValidNickname(nickname)) {
    		return false;
    	}
    	
    	// 기존 비밀번호 입력 검증
    	if(currentPassword == null || currentPassword.isBlank()) {
    		return false;
    	}
    	
    	// 새 비밀번호 입력 여부
    	boolean newPwChk = newPassword != null && !newPassword.isBlank();
    	
    	// 새 비밀번호 입력했을때만 유효성 검증
    	if(newPwChk && !isValidPassword(newPassword)) {
    		return false;
    	}
    	
    	// DB 에서 기존 해시비번 조회
    	String hash = userDAO.findPwByUserId(userId);
    	
    	// 기존 비밀번호 해시 조회
    	if(hash == null || !passwordEncoder.matches(currentPassword, hash)) {
    		return false;
    	}
    	
    	// 새 비밀번호 암호화
    	String encodedPw = newPwChk
    			? passwordEncoder.encode(newPassword)
				: null;
    	
    	return userDAO.updateProfile(userId, nickname, encodedPw) == 1;
    	
    }

    // 소유 프로젝트가 남아 있다면 탈퇴를 제한하여 관리자 없는 프로젝트를 방지
    public boolean hasOwnedProjects(long userId) throws Exception {
        return userDAO.hasOwnedProjects(userId);
    }

    // 비밀번호 재확인 후 소프트 삭제 (협업 기록은 보존)
    public boolean withdraw(long userId, String currentPassword) throws Exception {

        if (currentPassword == null || currentPassword.isBlank()) {
            return false;
        }

        String hash = userDAO.findPwByUserId(userId);

        if (hash == null || !passwordEncoder.matches(currentPassword, hash)) {
            return false;
        }

        return userDAO.withdraw(userId);
    }
    
    // 비밀번호 유효성 검사 (8~20자)
    private boolean isValidPassword(String password) {
        return password != null
                && password.length() >= 8
                && password.length() <= 20;
    }

    // 닉네임 유효성 검사
    private boolean isValidNickname(String nickname) {
        return nickname != null
                && !nickname.trim().isEmpty();
    }
}
