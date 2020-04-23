require "java"

# Use jgit to determine some information about the head of a git
# repository (first argument) and append it to a specified file (second
# argument) in properties file format.

def main
  open(ARGV[1], "a") do |fh|
    write_data(fh)
  end
end

def write_data(fh)
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
  commit = repo.parseCommit(head_ref.getObjectId())
  
  # Print the committer date in Unix epoch seconds (UTC). This omits the
  # timezone information present in the commit file, but we don't use that
  # anyway.
  fh.puts "build.git.committerdate=" + commit.getCommitTime().to_s
  
  # Print the version tag, if any. The simple approach would be
  # git.nameRev().add(head_id).addPrefix("refs/tags/").call().get(head_id)
  # but this will only return one tag, which might fail if a commit
  # has a version tag and one or more non-version tags. The code below
  # loops through all the tags associated with the head ID and finds the
  # last one starting with "version_" -- which should also be the *only*
  # one starting with "version_", since a commit should never have
  # multiple version tags.
  version = "undefined"
  version_tag_refs = git.tagList().call()
                     .select {|t| t.getName()
                               .start_with?("refs/tags/version_")}
  for tag_ref in version_tag_refs
    peeled_ref = repo.getRefDatabase().peel(tag_ref)
    peeled_id = peeled_ref.getPeeledObjectId()
    if peeled_id and peeled_id.equals(head_id)
      version = tag_ref.getName()[10..-1]
    end
  end
  fh.puts "build.git.tag=" + version
  
  # Print the hexadecimal hash code.
  fh.puts "build.git.hash=" + head_id.getName()

  # Print the clean/dirty state.  
  fh.puts "build.git.dirty=" + (git.status().call().isClean() ? "false" : "true")
  
end

main()
