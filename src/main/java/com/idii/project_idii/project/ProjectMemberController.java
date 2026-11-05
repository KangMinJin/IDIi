
package com.idii.project_idii.project;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/project/member")
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    public ProjectMemberController(
            ProjectMemberService projectMemberService) {

        this.projectMemberService = projectMemberService;
    }
    
 // 이메일로 멤버 추가
    @PostMapping("/add")
    public String addMember(HttpServletRequest request,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {

        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return "redirect:/user/login";
        }

        long projectId;

        try {
            projectId = Long.parseLong(request.getParameter("projectId"));
        } catch (Exception e) {
            return "redirect:/";
        }

        String email = request.getParameter("email");
        String role = request.getParameter("role");

        int result = projectMemberService.addMember(
                projectId, email, role, userId);

        if (result == 1) {
            redirectAttributes.addFlashAttribute(
                    "message", "멤버를 추가했습니다.");
        } else {
            redirectAttributes.addFlashAttribute(
                    "error", "멤버를 추가하지 못했습니다. 이메일이나 중복 여부를 확인해주세요.");
        }

        return "redirect:/project/detail?projectId=" + projectId;
    }

    // 멤버 제거
    @PostMapping("/delete")
    public String deleteMember(HttpServletRequest request,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {

        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return "redirect:/user/login";
        }

        long projectId;
        long memberId;

        try {
            projectId = Long.parseLong(request.getParameter("projectId"));
            memberId = Long.parseLong(request.getParameter("memberId"));
        } catch (Exception e) {
            return "redirect:/";
        }

        int result = projectMemberService.deleteMember(
                projectId, memberId, userId);

        if (result == 1) {
            redirectAttributes.addFlashAttribute(
                    "message", "멤버를 제거했습니다.");
        } else {
            redirectAttributes.addFlashAttribute(
                    "error", "멤버를 제거하지 못했습니다.");
        }

        return "redirect:/project/detail?projectId=" + projectId;
    }
}

