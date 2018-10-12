#!/usr/bin/python3

# Create LaTeX commands giving the current version string and date
# as determined from the Mercurialgit repository.

import datetime
import re
from subprocess import check_output

def make_version_string():
    tag = check_output(["git", "name-rev", "--tags", "HEAD"]).\
          decode("utf-8").strip()
    revid = check_output(["git", "show-ref", "--head", "--hash", "HEAD"]).\
            decode("utf-8").strip()
    status = check_output(["git", "status", "--porcelain"]).\
            decode("utf-8").strip()
    
    modified = len(status) > 0

    version_string = "Unknown"

    if (tag.startswith("HEAD tags/version_")):
        version_string = tag[18:]
    else:
        version_string = revid[:12]

    if modified:
        version_string += " (modified)"

    return version_string
    
def make_date_string():
    lines = check_output(["git", "cat-file", "commit", "HEAD"]).\
            decode("utf-8").split("\n")

    committerline = [line for line in lines if line.startswith("committer ")][0]
    print(committerline)
    unix_seconds = int(re.search(r" (\d+) [+-]\d\d\d\d$", committerline).group(1))
    datetime_object = datetime.datetime.fromtimestamp(unix_seconds)

    # We use dt.day rather than strftime-esque formatting for the day.
    # This is to avoid a leading zero for single-digit days, which can't
    # be done in a cross-platform way using strftime. See
    # http://stackoverflow.com/q/904928/6947739 for details.
    return '{dt.day} {dt:%B} {dt.year}'.format(dt=datetime_object)

def main():
    version_string = make_version_string()
    date_string = make_date_string()
    with open("vcs-commands.tex", "w") as outfile:
        outfile.write("\\def\\VcsVersion{%s}\n" % version_string)
        outfile.write("\\def\\VcsDate{%s}\n" % date_string)

if __name__ == "__main__":
    main()
