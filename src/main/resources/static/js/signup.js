
document.addEventListener("DOMContentLoaded", function () {

    const signupForm = document.getElementById("signupForm");

    const email = document.getElementById("email");
    const password = document.getElementById("password");
    const passwordConfirm = document.getElementById("passwordConfirm");
    const nickname = document.getElementById("nickname");

    // 오류 메시지 표시
    function setError(input, errorId, message) {
        const error = document.getElementById(errorId);

        error.textContent = message;
        error.hidden = message === "";

        input.classList.toggle("is-invalid", message !== "");
        input.setAttribute("aria-invalid", message !== "");
    }

    // 이메일 형식 검사
    function validateEmail() {
        const value = email.value.trim();
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

        if (!emailRegex.test(value)) {
            setError(email, "emailError", "올바른 이메일 형식으로 입력해 주세요.");
            return false;
        }

        setError(email, "emailError", "");
        return true;
    }

    // 비밀번호 8~20자 검사
    function validatePassword() {
        const value = password.value;

        if (value.length < 8 || value.length > 20) {
            setError(password, "passwordError", "비밀번호는 8~20자로 입력해 주세요.");
            return false;
        }

        setError(password, "passwordError", "");
        return true;
    }

    // 비밀번호 일치 검사
    function validatePasswordConfirm() {
        if (password.value !== passwordConfirm.value) {
            setError(passwordConfirm, "confirmError", "비밀번호가 일치하지 않습니다.");
            return false;
        }

        setError(passwordConfirm, "confirmError", "");
        return true;
    }

    // 닉네임 필수 입력 검사
    function validateNickname() {
        if (nickname.value.trim() === "") {
            setError(nickname, "nicknameError", "닉네임을 입력해 주세요.");
            return false;
        }

        setError(nickname, "nicknameError", "");
        return true;
    }

    // 입력 후 포커스를 벗어나면 검사
    email.addEventListener("blur", validateEmail);
    password.addEventListener("blur", validatePassword);
    passwordConfirm.addEventListener("blur", validatePasswordConfirm);
    nickname.addEventListener("blur", validateNickname);

    // 비밀번호 확인은 입력하면서도 검사
    passwordConfirm.addEventListener("input", function () {
        if (passwordConfirm.value !== "") {
            validatePasswordConfirm();
        }
    });

    // 비밀번호 변경 시 확인란 다시 검사
    password.addEventListener("input", function () {
        if (passwordConfirm.value !== "") {
            validatePasswordConfirm();
        }
    });

    // 회원가입 제출 전 최종 검사
    signupForm.addEventListener("submit", function (event) {

        const validEmail = validateEmail();
        const validPassword = validatePassword();
        const validConfirm = validatePasswordConfirm();
        const validNickname = validateNickname();

        if (!validEmail || !validPassword || !validConfirm || !validNickname) {
            event.preventDefault();

            const firstInvalid = signupForm.querySelector(".is-invalid");

            if (firstInvalid) {
                firstInvalid.focus();
            }
        }
    });

});
