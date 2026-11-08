document.addEventListener("DOMContentLoaded", function () {

    // ========================================
    // 1. 탭 전환 및 페이지네이션
    // ========================================

    const PAGE_SIZE = 8;

    const tabs = Array.from(
        document.querySelectorAll("[data-work-tab]")
    );

    const configs = {
        issue: {
            panel: document.getElementById("issue-panel"),
            selector: ".issue-list-row",
            nav: document.getElementById("issue-pagination"),
            page: 1
        },
        tc: {
            panel: document.getElementById("tc-panel"),
            selector: ".tc-row",
            nav: document.getElementById("tc-pagination"),
            page: 1
        }
    };

    function renderPage(type) {
        const cfg = configs[type];

        if (!cfg.panel || !cfg.nav) return;

        const rows = Array.from(
            cfg.panel.querySelectorAll(cfg.selector)
        );

        const total = Math.ceil(rows.length / PAGE_SIZE);

        cfg.page = Math.min(
            Math.max(cfg.page, 1),
            Math.max(total, 1)
        );

        rows.forEach(function (row, index) {
            row.hidden =
                index < (cfg.page - 1) * PAGE_SIZE ||
                index >= cfg.page * PAGE_SIZE;
        });

        cfg.nav.replaceChildren();

        if (total <= 1) return;

        function addButton(label, target, disabled, current) {
            const button = document.createElement("button");

            button.type = "button";
            button.textContent = label;
            button.className =
                "work-page-button" + (current ? " is-active" : "");
            button.disabled = disabled;

            if (current) {
                button.setAttribute("aria-current", "page");
            }

            button.addEventListener("click", function () {
                cfg.page = target;
                renderPage(type);
            });

            cfg.nav.appendChild(button);
        }

        addButton("이전", cfg.page - 1, cfg.page === 1, false);

        for (let page = 1; page <= total; page++) {
            addButton(String(page), page, false, page === cfg.page);
        }

        addButton("다음", cfg.page + 1, cfg.page === total, false);
    }

    function activateTab(type) {
        Object.entries(configs).forEach(function ([name, cfg]) {
            if (cfg.panel) {
                cfg.panel.hidden = name !== type;
            }
        });

        tabs.forEach(function (tab) {
            const selected = tab.dataset.workTab === type;

            tab.classList.toggle("is-active", selected);
            tab.setAttribute("aria-selected", String(selected));
            tab.tabIndex = selected ? 0 : -1;
        });

        renderPage(type);
    }

    tabs.forEach(function (tab, index) {
        tab.addEventListener("click", function () {
            activateTab(tab.dataset.workTab);
        });

        tab.addEventListener("keydown", function (event) {
            if (event.key !== "ArrowLeft" &&
                event.key !== "ArrowRight") return;

            event.preventDefault();

            const nextIndex =
                (index + (event.key === "ArrowRight" ? 1 : tabs.length - 1))
                % tabs.length;

            const nextTab = tabs[nextIndex];

            activateTab(nextTab.dataset.workTab);
            nextTab.focus();
        });
    });

    renderPage("issue");
    renderPage("tc");
    activateTab("issue");


    // ========================================
    // 2. 멤버 팝오버
    // ========================================

    const anchor = document.getElementById("memberPopoverAnchor");
    const trigger = document.getElementById("memberPopoverTrigger");
    const panel = document.getElementById("memberPopoverPanel");

    function closePopover() {
        if (!panel || !trigger) return;

        panel.hidden = true;
        trigger.setAttribute("aria-expanded", "false");
    }

    if (anchor && trigger && panel) {
        trigger.addEventListener("click", function () {
            const opening = panel.hidden;

            panel.hidden = !opening;
            trigger.setAttribute("aria-expanded", String(opening));
        });

        document.addEventListener("click", function (event) {
            if (!anchor.contains(event.target)) {
                closePopover();
            }
        });

        document.addEventListener("keydown", function (event) {
            if (event.key === "Escape") {
                closePopover();
                trigger.focus();
            }
        });
    }


    // ========================================
    // 3. 멤버 추가 모달
    // ========================================

    const memberModal = document.getElementById("memberModal");

    document.querySelectorAll(".member-popover-add").forEach(function (button) {
        button.addEventListener("click", function () {
            closePopover();

            if (memberModal) {
                memberModal.showModal();
            }
        });
    });

    document.querySelectorAll(
        ".member-modal-close, .member-modal-cancel"
    ).forEach(function (button) {
        button.addEventListener("click", function () {
            if (memberModal) {
                memberModal.close();
            }
        });
    });


    // ========================================
    // 4. 프로젝트 상태 / 멤버 삭제 확인
    // ========================================

    document.querySelectorAll(
        ".project-status-form, .member-delete-form"
    ).forEach(function (form) {

        form.addEventListener("submit", function (event) {
            const message = form.classList.contains("project-status-form")
                ? "프로젝트 상태를 변경하시겠습니까?"
                : "이 멤버를 삭제하시겠습니까?";

            if (!confirm(message)) {
                event.preventDefault();
            }
        });
    });


    // ========================================
    // 5. 공통 AJAX 함수
    // 서버 엔드포인트 구현 후 사용
    // ========================================

    async function updateSelectAjax(select) {
        const previousValue = select.dataset.value;
        const newValue = select.value;
        const form = select.closest("form");
        const url = select.dataset.ajaxUrl;

        if (!form || !url) return;

        const formData = new URLSearchParams(new FormData(form));

        select.disabled = true;

        try {
            const response = await fetch(url, {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded",
                    "Accept": "application/json"
                },
                body: formData.toString()
            });

            if (!response.ok) {
                throw new Error("변경사항을 저장하지 못했습니다.");
            }

            const result = await response.json();

            if (!result.success) {
                throw new Error(
                    result.message || "변경사항을 저장하지 못했습니다."
                );
            }

            select.dataset.value = result.status ?? result.value ?? newValue;

            // 테스트케이스 결과에 따른 색상 변경
            if (select.name === "result") {
                for (let i = 0; i <= 4; i++) {
                    select.classList.remove("tc-result-" + i);
                }

                select.classList.add("tc-result-" + select.value);
            }

        } catch (error) {
            select.value = previousValue;
            alert(error.message || "서버 통신 중 오류가 발생했습니다.");

        } finally {
            select.disabled = false;
        }
    }

    // AJAX URL이 설정된 select만 활성화
    document.querySelectorAll("select[data-ajax-url]").forEach(function (select) {
        select.addEventListener("change", function () {
            updateSelectAjax(select);
        });
    });

});
