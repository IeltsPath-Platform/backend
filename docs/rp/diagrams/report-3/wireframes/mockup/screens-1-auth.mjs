// Authentication screens. Targets are listed in the order of the elements in wf_index.json (undefined: found by label).
const guest = [[/GET \/api\/users\/examiners/, []]]
const google = `const own = [...document.querySelectorAll('[role=dialog] *')].find((el) => el.childElementCount === 0 && el.textContent.trim() === 'Hoặc'); const button = document.createElement('button'); button.id = 'google-sign-in'; button.type = 'button'; button.textContent = 'Continue with Google'; button.style.cssText = 'display:block;width:100%;margin-top:14px;padding:10px;border:1px solid #888;border-radius:20px;background:#fff;color:#333;font-size:14px'; own.parentElement.after(button)`
const modal = (file, url, targets, extra = {}) => ({ file, url, targets, height: 800, wait: 5000, guest: true, root: '[role=dialog]', backdrop: true, fixtures: guest, ...extra })

export const screens = [
  modal('authentication-sign-in', '/login', ['[role=tablist]', 'text=Sign in to start your journey!', '[role=dialog] input:not([type=password])', '[role=dialog] input[type=password]', 'text=Forgot password', '[role=dialog] button[type=submit]', '#google-sign-in'], { script: google, wait: 12000 }),
  modal('authentication-register', '/register', ['[role=tablist]', 'text=Create an account to start', '[role=dialog] input:not([type=password])', '[role=dialog] input[type=email]', '[role=dialog] input[type=password]', 'input[placeholder="Enter your password again"]', '[role=dialog] button[type=submit]', '#google-sign-in'], { script: google }),
  modal('authentication-forgot-password', '/forgot-password', ['text=Verify e-mail', 'text=Forgot your password? Recover', '[role=dialog] input', '[role=dialog] button[type=submit]', 'text=Back to sign in']),
  modal('authentication-reset-password', '/reset-password', ['text=Reset your password securely', '[role=dialog] input:not([type=password])', '[role=dialog] input[type=password]', 'input[placeholder="Enter the new password again"]', '[role=dialog] button[type=submit]', 'text=New code']),
  { file: 'authentication-verify-e-mail', url: '/verify-email?token=abc', guest: true, height: 760, wait: 5000, root: '.auth-page', fixtures: guest,
    script: `document.querySelectorAll('.auth-backdrop, canvas').forEach((el) => el.remove())`,
    targets: ['#verify-email-title', null, '[role=alert]', '.auth-form-panel a', 'text=You can send the link'] },
]