
package com.idii.project_idii.main;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.idii.project_idii.project.ProjectService;

import jakarta.servlet.http.HttpSession;

import org.springframework.ui.Model;

@ControllerAdvice(annotations = Controller.class)
public class SidebarAdvice {

    private final ProjectService projectService;

    public SidebarAdvice(ProjectService projectService) {
        this.projectService = projectService;
    }

    @ModelAttribute
    public void addSidebarProjects(HttpSession session, Model model) {

        Long userId = (Long) session.getAttribute("userId");

        if (userId != null) {
            var activeProjects = projectService.getProjectList(userId).stream()
                .filter(p -> !"1".equals(p.getStatus())).toList();
            model.addAttribute("sidebarProjects", activeProjects.stream().limit(7).toList());
            model.addAttribute("sidebarHasMore", activeProjects.size() > 7);
        }
    }
}
