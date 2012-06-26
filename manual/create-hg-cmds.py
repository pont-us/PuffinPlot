#!/usr/bin/python

import os, time

tag = os.popen('hg id -t').read().strip()
revid = os.popen('hg id -i').read().strip()
hgdate = os.popen(r"hg log -l1 -r. --template '{date|hgdate}\n'").read().strip()

modified = revid.endswith('+')
revid = revid.replace('+','')

version_string = 'Unknown'
if (tag.startswith('version_')):
    version_string = tag[8:]
else:
    version_string = revid
if modified:
    version_string += ' (modified)'

seconds_since_epoch = int(hgdate.split()[0])
time_object = time.gmtime(seconds_since_epoch)
date_string = time.strftime('%d %B %Y', time_object)

outfile = open('hg-cmds.tex', 'w')
outfile.write('\\def\\HgVersion{%s}\n' % version_string)
outfile.write('\\def\\HgDate{%s}\n' % date_string)
outfile.close()
