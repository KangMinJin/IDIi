package com.idii.project_idii.project;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Comparator;
import java.util.ArrayList;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.idii.project_idii.issue.Issue;
import com.idii.project_idii.issue.IssueService;
import com.idii.project_idii.testcase.TestCase;
import com.idii.project_idii.testcase.TestCaseService;

import java.awt.Color;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;

@Service
public class ProjectExcelService {
    private final ProjectService projectService;
    private final TestCaseService testCaseService;
    private final IssueService issueService;

    public ProjectExcelService(ProjectService projectService, TestCaseService testCaseService,
                               IssueService issueService) {
        this.projectService = projectService;
        this.testCaseService = testCaseService;
        this.issueService = issueService;
    }

    public byte[] export(long projectId, long userId) throws IOException {
        // getProject() checks membership; do not export another user's project.
        Project project = projectService.getProject(projectId, userId);
        if (project == null) throw new IllegalArgumentException("접근할 수 없는 프로젝트입니다.");
        List<TestCase> cases = new ArrayList<>(testCaseService.getTestCaseList(projectId, userId));
        List<Issue> issues = new ArrayList<>(issueService.getIssueList(projectId, userId));
        // DB의 실제 PK 오름차순으로 정렬하며, PK 자체는 엑셀에 노출하지 않는다.
        cases.sort(Comparator.comparingLong(TestCase::getTestCaseId));
        issues.sort(Comparator.comparingLong(Issue::getIssueId));

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
        	XSSFCellStyle heading = wb.createCellStyle();

        	heading.setFillForegroundColor(
        	    new XSSFColor(new Color(91, 92, 226), null)
        	);
        	heading.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font font = wb.createFont(); font.setBold(true); font.setColor(IndexedColors.WHITE.getIndex());
            heading.setFont(font);
            CellStyle wrap = wb.createCellStyle(); wrap.setWrapText(true); wrap.setVerticalAlignment(VerticalAlignment.TOP);

            Sheet info = wb.createSheet("프로젝트 정보");
            rows(info, heading, wrap, new String[]{"항목", "내용"}, new String[][]{
                {"프로젝트 번호", String.valueOf(project.getProjectId())},
                {"프로젝트명", project.getTitle()}, {"설명", project.getDescription()},
                {"상태", "1".equals(project.getStatus()) ? "완료" : "진행 중"},
                {"시작일", str(project.getStartDate())}, {"마감일", str(project.getDueDate())}
            }, new int[]{22, 75});

            Sheet tc = wb.createSheet("테스트케이스");
            String[][] tcData = new String[cases.size()][9];
            for (int i = 0; i < cases.size(); i++) {
                TestCase t = cases.get(i);
                tcData[i] = new String[]{t.getCaseCode(), t.getTitle(), t.getPrecondition(),
                    t.getTestSteps(), t.getExpectedResult(), result(t.getResult()), t.getRemarks(),
                    t.getAuthorNickname(), str(t.getCreatedAt())};
            }
            rows(tc, heading, wrap, new String[]{"TC 코드", "제목", "사전 조건", "테스트 절차", "기대 결과", "결과", "비고", "작성자", "등록일"},
                tcData, new int[]{16, 36, 48, 65, 65, 14, 55, 18, 23});

            Sheet issue = wb.createSheet("이슈 및 결함");
            String[][] issueData = new String[issues.size()][10];
            for (int i = 0; i < issues.size(); i++) {
                Issue x = issues.get(i);
                issueData[i] = new String[]{String.valueOf(i + 1), type(x.getType()), x.getTitle(),
                    x.getContent(), status(x.getStatus()), priority(x.getPriority()),
                    x.getReporterNickname(), x.getAssigneeNickname(), str(x.getDueDate()),
                    str(x.getCreatedAt())};
            }
            rows(issue, heading, wrap, new String[]{"이슈 번호", "유형", "제목", "내용", "상태", "우선순위", "작성자", "담당자", "마감일", "등록일"},
                issueData, new int[]{15, 15, 40, 75, 16, 16, 18, 18, 18, 23});

            wb.write(out);
            return out.toByteArray();
        }
    }

    private static void rows(Sheet sheet, CellStyle heading, CellStyle wrap,
                             String[] headers, String[][] data, int[] widths) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell c = header.createCell(i); c.setCellValue(headers[i]); c.setCellStyle(heading);
            sheet.setColumnWidth(i, Math.min(widths[i] * 256, 255 * 256));
        }
        for (int i = 0; i < data.length; i++) {
            Row row = sheet.createRow(i + 1);
            for (int j = 0; j < headers.length; j++) {
                Cell c = row.createCell(j); c.setCellValue(safe(data[i][j])); c.setCellStyle(wrap);
            }
        }
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(1, data.length), 0, headers.length - 1));
    }
    // Avoid Excel interpreting user text as a formula.
    private static String safe(String s) {
        if (s == null) return "";
        return !s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0 ? "'" + s : s;
    }
    private static String str(Object x) { return x == null ? "" : x.toString(); }
    private static String result(String x) {
        return switch (str(x)) { case "0" -> "PASS"; case "1" -> "FAIL";
            case "2" -> "BLOCK"; case "3" -> "N/A"; default -> "NO RUN"; };
    }
    private static String type(String x) {
        return switch (str(x)) { case "0" -> "할 일"; case "1" -> "결함"; case "2" -> "요구사항"; default -> str(x); };
    }
    private static String status(String x) {
        return switch (str(x)) {
            case "0" -> "대기";
            case "1" -> "진행 중";
            case "2" -> "검토 대기";
            case "3" -> "완료";
            default -> str(x);
        };
    }
    private static String priority(String x) {
        return switch (str(x)) {
            case "0" -> "낮음";
            case "1" -> "보통";
            case "2" -> "높음";
            default -> str(x);
        };
    }
}
