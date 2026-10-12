"""Screen list of the IELTSPath FDS, in Screen Index order, plus helpers shared by the screen files."""

# Shared rows ---------------------------------------------------------------
AUTH = ("Valid session: access token, or a successful silent refresh (GG-01)", "Continue rendering",
        "Redirect → Login?returnUrl=…; GLB-01")


def role(roles):
    return (f"User holds role {roles} (GG-02)", "Continue rendering", "Redirect → /403; GLB-02")


EX_SESSION = ("Session expires, silent refresh fails", "Refresh token used, expired or revoked", "Login",
              "GLB-01; returnUrl kept")


def ex_menu(note="—"):
    return ("User picks another item in the main menu", "Any", "Selected screen", note)


def owner_only(what):
    return (f"The {what} in the URL belongs to the signed-in user", "Continue rendering",
            "API returns 404 → redirect to previous screen; GLB-03")


def screen(**kw):
    required = ("key", "name", "route", "roles", "ft", "uc", "status", "purpose", "nav_from", "nav_to", "pre",
                "entry", "exits", "comps", "apis", "interactions", "msgs", "rules")
    missing = [k for k in required if k not in kw]
    if missing:
        raise ValueError(f"screen {kw.get('key')} misses {missing}")
    return kw


from fds_screens_learner import SCREENS as _LEARNER  # noqa: E402
from fds_screens_prototype import SCREENS as _PROTOTYPE  # noqa: E402
from fds_screens_self_study import SCREENS as _SELF_STUDY  # noqa: E402
from fds_screens_staff import SCREENS as _STAFF  # noqa: E402

SCREENS = _LEARNER + _PROTOTYPE + _SELF_STUDY + _STAFF
