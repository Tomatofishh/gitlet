# Gitlet Design Document
author: Qingyi Fang

## Design Document Guidelines


## 1. Classes and Data Structures

### Commit
#### Instance Variable
* Message: contains the message of a commit
* Timestamp: time at which commit was created; Assigned by the constructor
* Parent: the parent commit of a commit object
* contents: a treemap with key of file name and value of file contents as strings

### StagingArea
#### Instance Variable
* getAdd: a treemap with key of file name and value of file contents as strings
* getRm: a treemap with key of file name and value of file contents as strings

### Bloop
#### Instance Variable
* CWD: file path to working directory
* GITLET_DIR: file path to gitlet_dir
* BLOBS_DIR: file path to blobs, where store all the blobs
* COMMITS_DIR: file path to commits, where store all commits, each commit with name of commit sha1
* BRANCHES_DIR: file path to branches, where store a treemap with key of branches and value of sha1;
meanwhile, it also contains a key of "CURBRANCH" with value of the name of the current branch, e.g. "MASTER"



## 2. Algorithms

### Commit
#### Methods
* Commit(String message, Commit parent); with message and its parent as a commit
* getMessage is to get message
* getTimestamp is to get time stamp
* getParent is to get parent
* getSha1: get sha1 code of the specific commit, e.g. commit.getSha1()
* getBlobs: get blobs as strings of the given fileName in a certain commit
* fileExistInCommit: a boolean to check whether a file is existed in certain commit

### StagingArea
#### Methods
* getAdd(): get all files in addition stage as a treemap with key of fileName with value of blobs as strings
* getRm(): get all files in removal stage as a treemap with key of fileName with value of blobs as strings
* clearAll(): clear all staging area

### Bloop
#### Methods
* init: Creates a new Gitlet version-control system in the current directory. This system will 
automatically start with one commit: a commit that contains no files and has the commit message 
initial commit (just like that, with no punctuation). It will have a single branch: master, which 
initially points to this initial commit, and master will be the current branch. 
The timestamp for this initial commit will be 00:00:00 UTC, Thursday, 1 January 1970 in whatever 
format you choose for dates (this is called "The (Unix) Epoch", represented internally by the time 0.) 
Since the initial commit in all repositories created by Gitlet will have exactly the same content, 
it follows that all repositories will automatically share this commit (they will all have the same UID) 
and all commits in all repositories will trace back to it.

* add: Adds a copy of the file as it currently exists to the staging area (see the description of the 
commit command). For this reason, adding a file is also called staging the file for addition. 
Staging an already-staged file overwrites the previous entry in the staging area with the new contents. 
The staging area should be somewhere in .gitlet. If the current working version of the file is identical 
to the version in the current commit, do not stage it to be added, and remove it from the staging area 
if it is already there (as can happen when a file is changed, added, and then changed back). The file 
will no longer be staged for removal (see gitlet rm), if it was at the time of the command.  

* commit: Saves a snapshot of tracked files in the current commit and staging area so they can be 
restored at a later time, creating a new commit. The commit is said to be tracking the saved files. 
By default, each commit's snapshot of files will be exactly the same as its parent commit's snapshot 
of files; it will keep versions of files exactly as they are, and not update them. A commit will only 
update the contents of files it is tracking that have been staged for addition at the time of commit, 
in which case the commit will now include the version of the file that was staged instead of the version 
it got from its parent. A commit will save and start tracking any files that were staged for addition 
but weren't tracked by its parent. Finally, files tracked in the current commit may be untracked in the 
new commit as a result being staged for removal by the rm command (below). The bottom line: By default a 
commit is the same as its parent. Files staged for addition and removal are the updates to the commit. 
Of course, the date (and likely the message) will also be different from the parent.

* log: Starting at the current head commit, display information about each commit backwards along the 
commit tree until the initial commit, following the first parent commit links, ignoring any second 
parents found in merge commits. (In regular Git, this is what you get with git log --first-parent). 
This set of commit nodes is called the commit's history.

* checkoutByFileName(String fileName): Takes the version of the file as it exists in the head commit, 
the front of the current branch, and puts it in the working directory, overwriting the version of the 
file that's already there if there is one. The new version of the file is not staged.

* checkoutByCommitID(String commitID, String fileName): Takes the version of the file as it exists in 
the commit with the given id, and puts it in the working directory, overwriting the version of the file 
that's already there if there is one. The new version of the file is not staged.

* checkoutByBranch(String branchName): makes all files in the commit at the head of the given branch, 
and puts them in the working directory, overwriting the versions of the files that are already there if 
they exist. Also, at the end of this command, the given branch will now be considered the current branch 
(HEAD). Any files that are tracked in the current branch but are not present in the checked-out branch 
are deleted. The staging area is cleared, unless the checked-out branch is the current branch

* getSha1FromCommit(Commit commit): get sha1 from a given commit

* String pullHeadSha1(): get head sha1 by first reading from branch directory and then get HEAD's sha1
* String pullBranchSha1(String branchName): get any branch sha1 by given a certain branch Name
* TreeMap<String, String> getAllBranches(): get all branches as a treemap where keys are branch name and values are sha1
* String getCurrBranchName(): get current branch name

## 3. Persistence

Describe your strategy for ensuring that you don’t lose the state of your program
across multiple runs.

* I writeObject everytime, e.g. writeObject(initialPath, initialCommit);

## 4. Design Diagram

Attach a picture of your design diagram illustrating the structure of your
classes and data structures. The design diagram should make it easy to 
visualize the structure and workflow of your program.

