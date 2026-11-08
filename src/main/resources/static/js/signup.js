document.addEventListener("DOMContentLoaded", function () {

    const form = document.getElementById("signupForm");

    const email = document.getElementById("email");
    const password = document.getElementById("password");
    const passwordConfirm = document.getElementById("passwordConfirm");
    const nickname = document.getElementById("nickname");

    const code = document.getElementById("verificationCode");
    const sendBtn = document.getElementById("sendCodeBtn");
    const verifyBtn = document.getElementById("verifyCodeBtn");
    const timerText = document.getElementById("codeTimer");
    const hint = document.getElementById("verificationHint");
    const success = document.getElementById("verificationSuccess");

    let verifiedEmail = "";
    let sentEmail = "";
    let expiresAt = 0;
    let timerId = null;
    let busy = false;

    function setError(input, id, message) {
        const element = document.getElementById(id);

        element.textContent = message;
        element.hidden = message === "";

        input.classList.toggle("is-invalid", message !== "");
        input.setAttribute("aria-invalid", String(message !== ""));
		
		if (id === "emailError" && message !== "") {
		    emailSuccess.hidden = true;
		}
    }

    function validateEmail() {
        const value = email.value.trim();
        const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

        if (!regex.test(value) || value.length > 254) {
            setError(email, "emailError",
                "올바른 이메일 형식으로 입력해 주세요.");
            return false;
        }

        setError(email, "emailError", "");
        return true;
    }
	
	
	const emailSuccess = document.getElementById("emailSuccess");

	let checkedEmail = "";
	let emailCheckSequence = 0;

	async function checkEmailDuplicate() {

	    if (!validateEmail()) {
	        emailSuccess.hidden = true;
	        checkedEmail = "";
	        return;
	    }

	    const currentEmail = email.value.trim();
	    const sequence = ++emailCheckSequence;

	    emailSuccess.hidden = true;
	    checkedEmail = "";

	    try {
	        const response = await fetch(
	            "/user/check-email?email=" +
	            encodeURIComponent(currentEmail)
	        );

	        if (!response.ok) {
	            throw new Error("중복 확인 요청 실패");
	        }

	        const result = await response.json();

	        // 검사 도중 이메일이 변경된 경우 이전 응답 무시
	        if (sequence !== emailCheckSequence
	                || email.value.trim() !== currentEmail) {
	            return;
	        }

	        if (!result.available) {
	            setError(email, "emailError", result.message);
	            return;
	        }

	        checkedEmail = currentEmail;
	        setError(email, "emailError", "");

	        emailSuccess.textContent = result.message;
	        emailSuccess.hidden = false;

	    } catch (error) {
	        if (sequence !== emailCheckSequence) {
	            return;
	        }

	        setError(email, "emailError",
	            "이메일 중복 확인 중 오류가 발생했습니다.");
	    }
	}


    function validatePassword() {
        const length = password.value.length;

        if (length < 8 || length > 20) {
            setError(password, "passwordError",
                "비밀번호는 8~20자로 입력해 주세요.");
            return false;
        }

        setError(password, "passwordError", "");
        return true;
    }

    function validateConfirm() {
        if (!passwordConfirm.value ||
                password.value !== passwordConfirm.value) {
            setError(passwordConfirm, "confirmError",
                "비밀번호가 일치하지 않습니다.");
            return false;
        }

        setError(passwordConfirm, "confirmError", "");
        return true;
    }

    function validateNickname() {
        const value = nickname.value.trim();

        if (!value || value.length > 30) {
            setError(nickname, "nicknameError",
                "닉네임은 1~30자로 입력해 주세요.");
            return false;
        }

        setError(nickname, "nicknameError", "");
        return true;
    }

    function clearTimer() {
        if (timerId !== null) {
            clearInterval(timerId);
            timerId = null;
        }
        timerText.hidden = true;
    }

    function resetVerification() {
        verifiedEmail = "";
        sentEmail = "";
        expiresAt = 0;

        clearTimer();

        code.value = "";
        code.disabled = true;
        verifyBtn.disabled = true;

        success.hidden = true;
        hint.hidden = false;
        hint.textContent = "이메일 인증을 완료해 주세요.";

        setError(code, "verificationError", "");
        sendBtn.textContent = "인증번호 발송";
    }

    function updateTimer() {
        const remaining = Math.max(
            0, Math.ceil((expiresAt - Date.now()) / 1000)
        );

        const minutes = String(
            Math.floor(remaining / 60)
        ).padStart(2, "0");

        const seconds = String(
            remaining % 60
        ).padStart(2, "0");

        timerText.textContent = minutes + ":" + seconds;

        if (remaining === 0) {
			sendBtn.disabled = false;
            clearTimer();
            code.disabled = true;
            verifyBtn.disabled = true;
            hint.textContent = "인증번호가 만료되었습니다. 다시 발송해 주세요.";
        }
    }

    function startTimer() {
        clearTimer();
        expiresAt = Date.now() + 5 * 60 * 1000;
        timerText.hidden = false;

        updateTimer();
        timerId = setInterval(updateTimer, 1000);
    }

    async function postForm(url, data) {
        const body = new URLSearchParams(data);

        const response = await fetch(url, {
            method: "POST",
            headers: {
                "Content-Type":
                    "application/x-www-form-urlencoded;charset=UTF-8"
            },
            body: body.toString(),
            credentials: "same-origin"
        });

        if (!response.ok) {
            throw new Error("요청 처리 실패: " + response.status);
        }

        return await response.json();
    }
	
	// 이메일 변경 시 기존 인증 및 중복 확인 결과 초기화
	email.addEventListener("input", function () {
	    resetVerification();

	    // 이전 이메일 중복 확인 결과 초기화
	    checkedEmail = "";
	    emailCheckSequence++;
	    emailSuccess.hidden = true;

	    setError(email, "emailError", "");
	});

    email.addEventListener("blur", checkEmailDuplicate);
    password.addEventListener("blur", validatePassword);
    passwordConfirm.addEventListener("blur", validateConfirm);
    nickname.addEventListener("blur", validateNickname);

    password.addEventListener("input", function () {
        if (passwordConfirm.value) {
            validateConfirm();
        }
    });

    passwordConfirm.addEventListener("input", function () {
        if (passwordConfirm.value) {
            validateConfirm();
        }
    });

    code.addEventListener("input", function () {
        code.value = code.value.replace(/\D/g, "").slice(0, 6);
        setError(code, "verificationError", "");
    });

    // 인증번호 발송
    sendBtn.addEventListener("click", async function () {

        if (busy || !validateEmail()) {
            return;
        }

        busy = true;
        sendBtn.disabled = true;
        sendBtn.textContent = "발송 중...";

        try {
            const result = await postForm("/user/email/send", {
                email: email.value.trim()
            });

            if (!result.success) {
                setError(email, "emailError",
                    result.message || "인증번호 발송에 실패했습니다.");
                return;
            }

            verifiedEmail = "";
            sentEmail = email.value.trim();

            code.disabled = false;
            verifyBtn.disabled = false;
            code.value = "";

            success.hidden = true;
            hint.hidden = false;
			
			hint.textContent =
			"인증번호를 받지 못하셨다면 스팸메일함을 확인해 주세요.";

            setError(code, "verificationError", "");

            startTimer();
			sendBtn.disabled = true;
            code.focus();

        } catch (error) {
            setError(email, "emailError",
                "메일 발송 요청 중 오류가 발생했습니다.");

        } finally {
            busy = false;
            sendBtn.disabled = busy || Date.now() < expiresAt;
            sendBtn.textContent = "인증번호 재발송";
        }
    });

    // 인증번호 확인
    verifyBtn.addEventListener("click", async function () {

        if (busy) {
            return;
        }

        if (!/^\d{6}$/.test(code.value)) {
            setError(code, "verificationError",
                "6자리 인증번호를 입력해 주세요.");
            return;
        }

        if (!sentEmail || sentEmail !== email.value.trim()) {
            setError(code, "verificationError",
                "이메일이 변경되었습니다. 다시 인증해 주세요.");
            return;
        }

        busy = true;
        verifyBtn.disabled = true;

        try {
            const result = await postForm("/user/email/verify", {
                email: email.value.trim(),
                code: code.value
            });

            if (!result.success) {
                setError(code, "verificationError",
                    result.message || "인증번호가 일치하지 않습니다.");
                return;
            }

            verifiedEmail = email.value.trim();

            clearTimer();

            code.disabled = true;
            success.hidden = false;
            hint.hidden = true;

            setError(code, "verificationError", "");
            sendBtn.textContent = "인증 완료";

        } catch (error) {
            setError(code, "verificationError",
                "인증 확인 중 오류가 발생했습니다.");

        } finally {
            busy = false;
            verifyBtn.disabled = verifiedEmail !== "";
        }
    });

    // 회원가입 최종 검사
    form.addEventListener("submit", function (event) {

        const validEmail = validateEmail();
        const validPassword = validatePassword();
        const validConfirm = validateConfirm();
        const validNickname = validateNickname();

        const validVerification =
            verifiedEmail !== "" &&
            verifiedEmail === email.value.trim();

        if (!validVerification) {
            setError(code, "verificationError",
                "이메일 인증을 완료해 주세요.");
        }

        if (!validEmail || !validPassword ||
                !validConfirm || !validNickname ||
                !validVerification || busy) {

            event.preventDefault();

            const firstInvalid = form.querySelector(".is-invalid");

            if (firstInvalid) {
                firstInvalid.focus();
            }
        }
    });

});
