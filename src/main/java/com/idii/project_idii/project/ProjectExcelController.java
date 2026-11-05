package com.idii.project_idii.project;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/project")
public class ProjectExcelController {
    private final ProjectExcelService excelService;
    private final ProjectService projectService;

    public ProjectExcelController(ProjectExcelService excelService, ProjectService projectService) {
        this.excelService = excelService;
        this.projectService = projectService;
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> download(@RequestParam("projectId") long projectId, HttpSession session)
            throws IOException {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        if (projectService.getProject(projectId, userId) == null) return ResponseEntity.notFound().build();

        byte[] bytes = excelService.export(projectId, userId);
        // 파일 이름에 DB의 프로젝트 제목을 사용하고 파일 시스템 예약 문자를 제거한다.
        String title = projectService.getProject(projectId, userId).getTitle();
        String safeTitle = title == null ? "" : title.replaceAll("[/:*?\"<>|]", "_").replaceAll("[\\p{Cntrl}]", "_").trim();
        if (safeTitle.isEmpty()) safeTitle = "프로젝트_" + projectId;
        if (safeTitle.length() > 80) safeTitle = safeTitle.substring(0, 80);
        String fileName = "IDIi_" + safeTitle + ".xlsx";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8).build());
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return ResponseEntity.ok().headers(headers).contentLength(bytes.length).body(bytes);
    }
}
