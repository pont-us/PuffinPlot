#!/usr/bin/python3

# Create LaTeX commands giving the current version string and date
# as determined from the Mercurial repository.

import datetime
from subprocess import check_output

def make_version_string():
    tag = check_output(["hg", "id", "-t"]).decode("utf-8").strip()
    revid = check_output(["hg", "id", "-i"]).decode("utf-8").strip()

    modified = revid.endswith("+")
    revid = revid.replace("+", "")

    version_string = "Unknown"

    if (tag.startswith("version_")):
        version_string = tag[8:]
    else:
        version_string = revid

    if modified:
        version_string += " (modified)"

    return version_string
    
def make_date_string():
    args = ["hg", "log", "-l1", "-r.", "--template", r"{date|hgdate}\n"]
    hgdate = check_output(args).decode("utf-8").strip()
    seconds_since_epoch = int(hgdate.split()[0])
    datetime_object = datetime.datetime.fromtimestamp(seconds_since_epoch)

    # We use dt.day rather than strftime-esque formatting for the day.
    # This is to avoid a leading zero for single-digit days, which can't
    # be done in a cross-platform way using strftime. See
    # http://stackoverflow.com/q/904928/6947739 for details.
    return '{dt.day} {dt:%B} {dt.year}'.format(dt=datetime_object)

def main():
    version_string = make_version_string()
    date_string = make_date_string()
    with open("hg-cmds.tex", "w") as outfile:
        outfile.write("\\def\\HgVersion{%s}\n" % version_string)
        outfile.write("\\def\\HgDate{%s}\n" % date_string)

if __name__ == "__main__":
    main()
