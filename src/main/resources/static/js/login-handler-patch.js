async function handleLogin(e) {
    e.preventDefault();
    const btn = document.getElementById('btn');
    const errBox = document.getElementById('error-box');

    btn.disabled = true;
    btn.textContent = 'Signing in...';
    errBox.style.display = 'none';

    try {
        const res = await fetch('/api/v1/auth/login', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                email: document.getElementById('email').value,
                password: document.getElementById('password').value
            })
        });
        const data = await res.json();

        if (!res.ok) {
            if (res.status === 403) {
                errBox.innerHTML = `
                        📧 Your email is not verified yet.
                        <br><span style="font-size:12px;opacity:0.8">
                            Check your inbox or
                            <a href="#" onclick="resendVerification(event)"
                               style="color:var(--accent2);text-decoration:underline">
                                resend verification email
                            </a>
                        </span>`;
            } else {
                errBox.textContent = data.detail || 'Invalid email or password';
            }
            errBox.style.display = 'block';
            return;
        }

        Auth.setToken(data.data.accessToken);
        window.location.href = '/dashboard';
    } catch {
        errBox.textContent = 'Something went wrong. Please try again.';
        errBox.style.display = 'block';
    } finally {
        btn.disabled = false;
        btn.textContent = 'Sign in';
    }
}

async function resendVerification(e) {
    e.preventDefault();
    const email = document.getElementById('email').value;
    if (!email) {
        document.getElementById('error-box').textContent = 'Please enter your email first.';
        return;
    }
    await fetch('/api/v1/auth/resend-verification', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({email})
    });
    document.getElementById('error-box').innerHTML =
        '✅ Verification email sent! Check your inbox.';
}