"""Wireframe data, feature 1: Authentication and feature 2: Home and public pages. Content read from the web pages
(features/auth, features/home) and the mobile screens (app/(auth), welcome, onboarding)."""

FEATURE = 'Authentication'

SCREENS = [
    dict(name='Sign In', shell='site', ucs='Sign In with Password, Sign In with Google',
         note='The mobile screen has no navigation bar.',
         items=[
             ('tabs', ['Sign In', 'Sign Up'], 'Switch between signing in and registering. The active tab is highlighted.'),
             ('title', 'Sign in to start your journey!', 'Heading of the form.'),
             ('field', 'E-mail', 'Required, at most 255 characters. A message in red appears under the field when it is empty.', 'Enter your e-mail'),
             ('field', 'Password', 'Required. The text is hidden and can be shown with a button.', 'Enter your password'),
             ('link', 'Forgot password?', 'Opens Forgot Password.'),
             ('button', 'Sign In', 'Checks the form, then signs in (UC Sign In with Password). A wrong e-mail or password, an inactive account and a wait after many failures each show a message and the form shakes.'),
             ('button2', 'Continue with Google', 'Signs in with a Google account (UC Sign In with Google). A failure shows "Cannot sign in with Google. Please try again."'),
         ]),
    dict(name='Register', shell='site', ucs='Sign Up',
         note='The mobile screen also has an optional phone number field.',
         items=[
             ('tabs', ['Sign In', 'Sign Up'], 'Switch between signing in and registering.', 1),
             ('title', 'Create an account to start your journey', 'Heading of the form.'),
             ('field', 'Full name', 'Required, at most 150 characters.', 'Enter your full name'),
             ('field', 'E-mail', 'Required, must be a valid e-mail address.', 'Enter your e-mail'),
             ('field', 'Password', 'Required, 8 to 72 characters. The server also refuses common passwords, passwords that repeat the name or e-mail, and passwords that are too simple, and shows the reason under the field.', 'Enter your password'),
             ('field', 'Confirm password', 'Required, must be the same as the password.', 'Enter the password again'),
             ('button', 'Create account', 'Creates the Learner account and sends the verification e-mail (UC Sign Up).'),
             ('button2', 'Continue with Google', 'Registers or signs in with Google (UC Sign In with Google).'),
         ]),
    dict(name='Verify E-mail', shell='site', note='On mobile the learner types the verification code of the e-mail and can ask for a new one.', ucs='Verify E-mail Address',
         items=[
             ('title', 'Verify e-mail', 'Heading.'),
             ('text', 'Verifying…', 'Shown while the link in the e-mail is being checked.'),
             ('text', 'Result message', 'Success: the address is confirmed. Failure: "The verification link is not valid or has expired", or "The verification link is incomplete. Open the correct link in the e-mail."'),
             ('button', 'Sign In', 'Goes to the Sign In screen after a result.'),
             ('text', 'You can send the link again from the profile page after you sign in.', 'Hint under a failed result. The page itself has no field to ask for a new link.'),
         ]),
    dict(name='Forgot Password', shell='site', ucs='Reset Forgotten Password',
         items=[
             ('steps', ['Forgot password', 'Verify e-mail', 'Reset password'], 'Shows which step of the recovery the user is on.'),
             ('title', 'Forgot your password? Recover it fast!', 'Heading.'),
             ('field', 'E-mail', 'Required, a valid e-mail address.', 'Enter your e-mail'),
             ('button', 'Send request', 'Sends the request. The message "Request sent" is the same whether or not the address exists (UC Reset Forgotten Password). If sending fails: "Could not send the request. Try again."'),
             ('link', 'Back to sign in', 'Returns to Sign In.'),
         ]),
    dict(name='Reset Password', shell='site', ucs='Reset Forgotten Password',
         items=[
             ('title', 'Reset your password securely!', 'Heading. The text under it asks for the code and the new password.'),
             ('field', 'Reset code', 'Required. Filled in from the link in the e-mail, or pasted by the user.', 'Paste the code from the e-mail'),
             ('field', 'New password', 'Required, 8 to 72 characters; the same rules as when registering.', 'Enter a new password'),
             ('field', 'Confirm password', 'Required, must be the same as the new password.', 'Enter the new password again'),
             ('button', 'Reset password', 'Saves the new password and closes the other sessions. A code that is wrong, used or older than 15 minutes shows a message.'),
             ('link', 'Request a new code', 'Goes back to Forgot Password.'),
         ]),
    dict(name='Welcome', shell='mobile', tabs=[], ucs='Sign Up, Sign In with Password',
         items=[
             ('image', 'IELTSPath logo and mascot', 'Branding picture.'),
             ('title', 'Learn step by step. Keep the rhythm every day.', 'Tagline of the app.'),
             ('button', 'Get started', 'Opens Onboarding for a new user.'),
             ('button2', 'I already have an account', 'Opens Sign In.'),
         ]),
    dict(name='Onboarding', shell='mobile', tabs=[], ucs='Sign Up',
         items=[
             ('steps', ['1', '2', '3', '4', '5', '6'], 'Progress bar and step counter, for example 2/6. The back arrow returns to the earlier question.'),
             ('title', 'Target band?', 'One question at a time: target band (5.5 to 7.5+), the first skill to improve, daily minutes (5 to 20), why the user studies IELTS, earlier exams, and when to remind.'),
             ('chips', ['5.5', '6.0', '6.5', '7.0', '7.5+'], 'Answer options; exactly one can be chosen.'),
             ('button', 'Next', 'Disabled until an option is chosen. After the last question the answers are saved on the phone and the app opens Register.'),
         ]),
]
