:<<"::CMDLITERAL"
@ECHO OFF
GOTO :CMDSCRIPT
::CMDLITERAL

# installer.cmd builds IDEA Community installers
# THIS SCRIPTS WORKS FOR ALL SYSTEMS Linux/Windows/macOS
# See README.md for usage scenarios

set -eux
git rev-parse --short=13 HEAD
mkdir -p /home/ide_builder/.m2/foo
root="$(cd "$(dirname "$0")"; pwd)"
exec "$root/platform/jps-bootstrap/jps-bootstrap.sh" "$@" "$root" intellij.idea.community.build OpenSourceCommunityInstallersBuildTarget
:CMDSCRIPT

call "%~dp0\platform\jps-bootstrap\jps-bootstrap.cmd" %* "%~dp0." intellij.idea.community.build OpenSourceCommunityInstallersBuildTarget
EXIT /B %ERRORLEVEL%
