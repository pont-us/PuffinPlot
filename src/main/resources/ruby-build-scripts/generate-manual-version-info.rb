require "java"

def main
  # We expect the git working directory to be passed as
  # the first argument.
  worktree = java.io.File.new(ARGV[0])
  repo = org.eclipse.jgit.storage.file
         .FileRepositoryBuilder.new()
         .setWorkTree(worktree)
         .build()
  git = org.eclipse.jgit.api.Git.new(repo)
  head_ref = repo.exactRef(org.eclipse.jgit.lib.Constants::HEAD)
  head_id = head_ref.getObjectId()

  # We write the version line to standard output and rely on
  # the caller to redirect it appropriately.
  puts ":revnumber: " + make_version_string(git, head_id)
  puts ":revdate: " + make_date_string(repo, head_id)
end

def make_version_string(git, head_id)
  names = git.nameRev().add(head_id).addPrefix("refs/tags/").call()
  tag_name = names.get(head_id)
  rev_id_sha1 = head_id.getName()

  if tag_name&.start_with?("version_")
    # The PuffinPlot convention is that official version tags are
    # prefixed with "version_".
    version_string = tag_name[8..-1]
  else
    # We prefix "R:" to make it (even more) clear that this is a revision
    # hash, not an official version number. The truncation to eight digits
    # is probably safe: at the time of writing, with 1011 commits in the
    # PuffinPlot repo, six digits are sufficient for uniqueness, and even
    # the Linux kernel got by with seven until 2010.
    version_string = "R:" + rev_id_sha1[0..7]
  end
  
  is_clean = git.status().call().isClean()
  version_string += " (modified)" if !is_clean
  
  return version_string
end

def make_date_string(repo, head_id)
  head_commit = repo.parseCommit(head_id)
  unix_seconds = head_commit.getCommitTime()
  time = Time.at(unix_seconds)

  # For consistency we convert the time to the UTC timezone -- otherwise
  # the commit date will depend on the timezone from which the commit was
  # made.
  time_utc = time.getgm()

  # The "-" modifier to omit the leading 0 from a single-digit date is not
  # supported in all strftime implementations, but fortunately jruby's
  # implementation does support it.
  return time_utc.strftime("%-d %B %Y")
end

main()
