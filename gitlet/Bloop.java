package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Objects;
import java.util.ArrayDeque;
import java.util.Map;



import static gitlet.Utils.*;


public class Bloop implements Serializable {

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));

    /** The whole folder with name .gitlet it contains everything. */
    public static final File GITLET_FOLDER = join(CWD, ".gitlet");

    /** A blob folder which saves all blobs of files in gitlet. */
    public static final File BLOBS_FOLDER = join(GITLET_FOLDER, "blobs");

    /** A commit folder which saves all commits in gitlet, with names of
     * commit sha1. */
    public static final File COMMITS_FOLDER = join(GITLET_FOLDER, "commits");

    /** A branches folder which saves all branches in gitlet. */
    public static final File BRANCHES_FOLDER = join(GITLET_FOLDER, "branches");

    /** the branches, with key of branch name and value
     * of branch sha1. */
    private static TreeMap<String, String> branches = new TreeMap<>();



    public static void init() throws IOException {
        if (GITLET_FOLDER.exists()) {
            System.out.println("A Gitlet version-control system already "
                    + "exists in the current directory.");
        } else {
            GITLET_FOLDER.mkdir();
            BLOBS_FOLDER.mkdir();
            COMMITS_FOLDER.mkdir();
            StagingArea staging = new StagingArea();
            StagingArea.STAGINGAREA_FOLDER.createNewFile();
            writeObject(StagingArea.STAGINGAREA_FOLDER, staging);
            Commit initialCommit = new Commit("initial commit", null, null);
            String headSha1Code = getSha1FromCommit(initialCommit);
            File initialPath = join(COMMITS_FOLDER, headSha1Code);
            writeObject(initialPath, initialCommit);
            branches.put("HEAD", headSha1Code);
            branches.put("master", headSha1Code);
            branches.put("CURBRANCH", "master");
            BRANCHES_FOLDER.createNewFile();
            writeObject(BRANCHES_FOLDER, branches);
        }
    }

    public static void add(String fileName) throws IOException {
        File addFile = join(CWD, fileName);
        if (!addFile.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        } else {
            String addSha1 = sha1(serialize(readContentsAsString(addFile)));
            String headSha1 = pullHeadSha1();
            Commit headCommit = getCommit(headSha1);
            StagingArea staging =
                    readObject(StagingArea.STAGINGAREA_FOLDER,
                            StagingArea.class);
            File blob = join(BLOBS_FOLDER, addSha1);
            blob.createNewFile();
            writeContents(blob, readContentsAsString(addFile));
            if (staging.getAdd().containsKey(fileName)) {
                staging.getAdd().replace(fileName, readContentsAsString(blob));
            } else if (headCommit.fileExistInCommit(fileName)) {
                String headFileSha1 =
                        sha1(serialize(headCommit.getContents().get(fileName)));
                if (addSha1.equals(headFileSha1)) {
                    staging.getAdd().remove(fileName);
                    staging.getRm().remove(fileName);
                } else {
                    staging.getAdd().put(fileName, readContentsAsString(blob));
                }
            } else if (staging.getRm().contains(fileName)) {
                staging.getRm().remove(fileName);
            } else {
                staging.getAdd().put(fileName, readContentsAsString(blob));
            }
            writeObject(StagingArea.STAGINGAREA_FOLDER, staging);
        }
    }

    public static void commit(String msg, Commit parent2) throws IOException {
        if (Objects.equals(msg, "")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
        StagingArea staging =
                readObject(StagingArea.STAGINGAREA_FOLDER, StagingArea.class);
        boolean stagingIsEmpty;
        stagingIsEmpty = staging.getAdd().isEmpty()
                && staging.getRm().isEmpty();
        if (stagingIsEmpty) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        } else {
            TreeMap<String, String> branchess = getAllBranches();
            String currBranch = getCurrBranchName();
            String lastCommitSha1 = branchess.get("HEAD");
            Commit lastCommit = getCommit(lastCommitSha1);
            Commit newCommit;
            if (parent2 == null) {
                newCommit = new Commit(msg, lastCommit, null);
            } else {
                newCommit = new Commit(msg, lastCommit, parent2);
            }
            TreeMap<String, String> lastCommitFile = lastCommit.getContents();
            newCommit.getContents().putAll(lastCommitFile);
            TreeMap<String, String> newCommitFileInAdd = staging.getAdd();
            newCommit.getContents().putAll(newCommitFileInAdd);
            for (String rmKeys : staging.getRm()) {
                newCommit.getContents().remove(rmKeys);
            }
            String newCommitSha1 = getSha1FromCommit(newCommit);
            File saveCommitPath = join(COMMITS_FOLDER, newCommitSha1);
            writeObject(saveCommitPath, newCommit);
            staging.clearAll();
            writeObject(StagingArea.STAGINGAREA_FOLDER, staging);
            branchess.replace("HEAD", newCommitSha1);
            branchess.replace(currBranch, newCommitSha1);
            BRANCHES_FOLDER.createNewFile();
            writeObject(BRANCHES_FOLDER, branchess);
        }
    }

    public static void log() {
        String headSha1 = pullHeadSha1();
        Commit headCommit = getCommit(headSha1);
        Commit curr = headCommit;
        String currSha1 = headSha1;
        while (curr.getParent1() != null) {
            System.out.println("===");
            System.out.println("commit " + currSha1);
            System.out.println("Date: " + curr.getTimestamp());
            System.out.println(curr.getMessage());
            curr = curr.getParent1();
            currSha1 = curr.getSha1();
            System.out.println();
        }
        System.out.println("===");
        System.out.println("commit " + currSha1);
        System.out.println("Date: " + curr.getTimestamp());
        System.out.println(curr.getMessage());
        System.out.println();
    }

    public static void checkoutByFileName(String fileName) {
        String headSha1 = pullHeadSha1();
        Commit headCommit = getCommit(headSha1);
        if (!headCommit.fileExistInCommit(fileName)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        } else {
            String headVersionFile = headCommit.getBlobs(fileName);
            File putInCWD = join(CWD, fileName);
            writeContents(putInCWD, headVersionFile);
        }
    }

    public static void checkoutByCommitID(String commitID, String fileName) {
        Commit givenCommit = null;
        for (String sha1
                : Objects.requireNonNull(plainFilenamesIn(COMMITS_FOLDER))) {
            if (sha1.startsWith(commitID)) {
                givenCommit = getCommit(sha1);
                break;
            }
        }
        if (givenCommit == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        } else if (!givenCommit.fileExistInCommit(fileName)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        } else {
            String commitVersionFile = givenCommit.getBlobs(fileName);
            File putInCWD = join(CWD, fileName);
            writeContents(putInCWD, commitVersionFile);
        }
    }

    public static void checkoutByBranch(String branchName) throws IOException {
        TreeMap<String, String> allBranches = getAllBranches();
        String currBranch = getCurrBranchName();
        if (!allBranches.containsKey(branchName)) {
            System.out.println("No such branch exists.");
            System.exit(0);
        } else if (currBranch.equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        } else {
            String branchSha1 = pullBranchSha1(branchName);
            Commit branchCommit = getCommit(branchSha1);
            String headSha1 = pullHeadSha1();
            Commit headCommit = getCommit(headSha1);
            ArrayList<String> filesInBranch = branchCommit.getAllFileNames();
            ArrayList<String> filesInHead = headCommit.getAllFileNames();
            for (String files : filesInBranch) {
                if (!filesInHead.contains(files)) {
                    if (join(CWD, files).exists()) {
                        System.out.println("There is an "
                                + "untracked file in the way; "
                                + "delete it, or add and commit it first.");
                        System.exit(0);
                    }
                }
            }
            for (String files : filesInBranch) {
                File putInCWD = join(CWD, files);
                if (putInCWD.exists()) {
                    writeContents(putInCWD, branchCommit.getBlobs(files));
                } else {
                    putInCWD.createNewFile();
                    writeContents(putInCWD, branchCommit.getBlobs(files));
                }
            }
            for (String files : filesInHead) {
                if (!filesInBranch.contains(files)) {
                    File deleted = join(CWD, files);
                    deleted.delete();
                }
            }
            allBranches.replace("HEAD", branchSha1);
            allBranches.replace("CURBRANCH", branchName);
            writeObject(BRANCHES_FOLDER, allBranches);
            StagingArea staging =
                    readObject(StagingArea.STAGINGAREA_FOLDER,
                            StagingArea.class);
            staging.clearAll();
            writeObject(StagingArea.STAGINGAREA_FOLDER, staging);
        }
    }

    public static void rmCommand(String fileName) {
        StagingArea staging = getStaging();
        TreeMap<String, String> stageAdded = staging.getAdd();
        Commit headCommit = pullHeadCommit();
        ArrayList<String> headFileNames = headCommit.getAllFileNames();
        if (!stageAdded.containsKey(fileName)) {
            if (!headFileNames.contains(fileName)) {
                System.out.println("No reason to remove the file.");
                System.exit(0);
            }
        }
        stageAdded.remove(fileName);
        if (headFileNames.contains(fileName)) {
            ArrayList<String> stageRemoved = staging.getRm();
            File givenFile = join(CWD, fileName);
            if (givenFile.exists()) {
                givenFile.delete();
            }
            stageRemoved.add(fileName);
        }
        saveStage(staging);
    }

    public static void globalLog() {
        for (String sha1 : Objects.requireNonNull(
                plainFilenamesIn(COMMITS_FOLDER))) {
            Commit theCommit = getCommit(sha1);
            System.out.println("===");
            System.out.println("commit " + sha1);
            System.out.println("Date: " + theCommit.getTimestamp());
            System.out.println(theCommit.getMessage());
            System.out.println();
        }
    }

    public static void find(String commitMsg) {
        Commit theCommit = null;
        for (String sha1 : Objects.requireNonNull(
                plainFilenamesIn(COMMITS_FOLDER))) {
            if (getCommit(sha1).getMessage().equals(commitMsg)) {
                theCommit = getCommit(sha1);
                System.out.println(sha1);
            }
        }
        if (theCommit == null) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
    }

    public static void status() {
        TreeMap<String, String> allBranches = getAllBranches();
        String currBranchName = getCurrBranchName();
        TreeMap<String, String> realBranches = allBranches;
        realBranches.remove("CURBRANCH");
        realBranches.remove(currBranchName);
        realBranches.remove("HEAD");
        System.out.println("=== Branches ===");
        System.out.println("*" + currBranchName);
        for (String branchName : realBranches.keySet()) {
            System.out.println(branchName);
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        StagingArea staging = getStaging();
        TreeMap<String, String> addStageFiles = staging.getAdd();
        for (String fileName : addStageFiles.keySet()) {
            System.out.println(fileName);
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        ArrayList<String> rmStageFiles = staging.getRm();
        for (String fileName : rmStageFiles) {
            System.out.println(fileName);
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        Commit currCommit = pullHeadCommit();
        ArrayList<String> filesDeleted =
                modificationDeleted(staging, currCommit);
        ArrayList<String> filesModified =
                modificationModified(staging, currCommit);
        if (!filesDeleted.isEmpty()) {
            for (String deleted : filesDeleted) {
                System.out.println(deleted + " (deleted)");
            }
        }
        if (!filesModified.isEmpty()) {
            for (String modified : filesModified) {
                System.out.println(modified + " (modified)");
            }
        }
        System.out.println();
        System.out.println("=== Untracked Files ===");
        ArrayList<String> filesUntracked = untrackedFiles(staging, currCommit);
        for (String untracked : filesUntracked) {
            System.out.println(untracked);
        }
        System.out.println();
    }

    private static ArrayList<String> modificationDeleted(
            StagingArea staging, Commit currCommit) {
        ArrayList<String> result = new ArrayList<>();
        for (String fileName : staging.getAdd().keySet()) {
            File pathToCWD = join(CWD, fileName);
            if (!pathToCWD.exists()) {
                result.add(fileName);
            }
        }
        for (String fileName : currCommit.getAllFileNames()) {
            File pathToCWD = join(CWD, fileName);
            if (!pathToCWD.exists()) {
                if (!staging.getRm().contains(fileName)) {
                    result.add(fileName);
                }
            }
        }
        return result;
    }

    private static ArrayList<String> modificationModified(
            StagingArea staging, Commit currCommit) {
        ArrayList<String> result = new ArrayList<>();
        for (String file : currCommit.getAllFileNames()) {
            File pathToCWD = join(CWD, file);
            if (pathToCWD.exists()) {
                if (!readContentsAsString(pathToCWD).equals(
                        currCommit.getBlobs(file))) {
                    if (!staging.getAdd().containsKey(file)) {
                        result.add(file);
                    }
                }
            }
        }
        for (String file : staging.getAdd().keySet()) {
            File pathToCWD = join(CWD, file);
            if (pathToCWD.exists()) {
                if (!readContentsAsString(pathToCWD).equals(
                        staging.getAdd().get(file))) {
                    result.add(file);
                }
            }
        }
        return result;
    }

    private static ArrayList<String> untrackedFiles(
            StagingArea staging, Commit currCommit) {
        ArrayList<String> result = new ArrayList<>();
        for (String file : Objects.requireNonNull(plainFilenamesIn(CWD))) {
            if (!staging.getAdd().containsKey(file)) {
                if (!currCommit.fileExistInCommit(file)) {
                    result.add(file);
                }
            }
        }
        for (String file : staging.getRm()) {
            File pathToCWD = join(CWD, file);
            if (pathToCWD.exists()) {
                result.add(file);
            }
        }
        return result;
    }

    public static void branchCommand(String branchName) {
        TreeMap<String, String> allBranches = getAllBranches();
        if (allBranches.containsKey(branchName)) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        String headBranchSha1 = pullHeadSha1();
        allBranches.put(branchName, headBranchSha1);
        writeObject(BRANCHES_FOLDER, allBranches);
    }

    public static void rmBranch(String branchName) {
        TreeMap<String, String> allBranches = getAllBranches();
        String currBranchName = getCurrBranchName();
        if (!allBranches.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        } else if (currBranchName.equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        } else {
            allBranches.remove(branchName);
            writeObject(BRANCHES_FOLDER, allBranches);
        }
    }

    public static void reset(String commitSha1) {
        Commit givenCommit = null;
        for (String sha1 : Objects.requireNonNull(
                plainFilenamesIn(COMMITS_FOLDER))) {
            if (sha1.startsWith(commitSha1)) {
                givenCommit = getCommit(sha1);
                break;
            }
        }
        if (givenCommit == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        Commit currCommit = pullHeadCommit();
        for (String givenFileName : givenCommit.getAllFileNames()) {
            if (!currCommit.fileExistInCommit(givenFileName)) {
                File cwdPath = join(CWD, givenFileName);
                if (cwdPath.exists()) {
                    System.out.println("There is an untracked file in the way; "
                            + "delete it, or add and commit it first.");
                    System.exit(0);
                }
            }
        }
        for (String currFileName : currCommit.getAllFileNames()) {
            if (!givenCommit.fileExistInCommit(currFileName)) {
                join(CWD, currFileName).delete();
            }
        }
        for (String givenFileName : givenCommit.getAllFileNames()) {
            checkoutByCommitID(commitSha1, givenFileName);
        }
        TreeMap<String, String> allBranches = getAllBranches();
        String currName = null;
        for (Map.Entry<String, String> curr : allBranches.entrySet()) {
            if (curr.getValue().equals(commitSha1)) {
                currName = curr.getKey();
                break;
            }
        }
        if (currName != null) {
            allBranches.put("CURBRANCH", currName);
        }
        allBranches.put("HEAD", commitSha1);
        String curName = allBranches.get("CURBRANCH");
        allBranches.put("CURBRANCH", curName);
        allBranches.put(curName, commitSha1);
        writeObject(BRANCHES_FOLDER, allBranches);
        StagingArea staging = getStaging();
        staging.clearAll();
        saveStage(staging);
    }

    public static void mergeCommand(String otherName) throws IOException {
        boolean isConflict = false;
        TreeMap<String, String> allBranches = getAllBranches();
        String headSha1 = pullHeadSha1();
        Commit headCommit = pullHeadCommit();
        StagingArea staging = getStaging();

        mergeErrorCase1(staging, allBranches, otherName);

        String otherSha1 = pullBranchSha1(otherName);
        Commit otherCommit = getCommit(otherSha1);
        ArrayList<String> filesInOther = otherCommit.getAllFileNames();
        ArrayList<String> filesInHead = headCommit.getAllFileNames();

        mergeErrorCase2(filesInHead, filesInOther);

        String splitSha1 = splitPointSha1(otherName);
        Commit splitCommit = getCommit(splitSha1);

        mergeErrorCase3(headSha1, otherSha1, splitSha1, otherName);

        ArrayList<String> filesInSplit = splitCommit.getAllFileNames();

        isConflict = mergeInSplitCase(filesInSplit,
                otherCommit, splitSha1, otherSha1,
                staging, splitCommit, headSha1, isConflict);

        isConflict = mergeNotInSplitCase(filesInHead, otherCommit,
                splitCommit, isConflict, staging,
                headSha1, filesInOther, otherSha1);

        saveStage(staging);
        commit("Merged " + otherName + " into "
                + getCurrBranchName() + ".", otherCommit);
        if (isConflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    private static boolean mergeInSplitCase(ArrayList<String> filesInSplit,
                                            Commit otherCommit,
                                            String splitSha1,
                                            String otherSha1,
                                            StagingArea staging,
                                            Commit splitCommit, String headSha1,
                                            boolean isConflict) {
        Commit headCommit = pullHeadCommit();
        for (String aFileInSplit : filesInSplit) {
            boolean modifiedInOther =
                    modifiedInOther(aFileInSplit, otherCommit, splitSha1);
            boolean modifiedInHead = modifiedInHead(aFileInSplit, splitSha1);
            boolean existInHead = existInHead(aFileInSplit, headCommit);
            boolean existInOther = existInOther(aFileInSplit, otherCommit);
            if (modifiedInOther && !modifiedInHead && existInOther) {
                checkoutByCommitID(otherSha1, aFileInSplit);
                staging.getAdd().put(aFileInSplit,
                        splitCommit.getBlobs(aFileInSplit));
            } else if (modifiedInHead && !modifiedInOther && existInHead) {
                checkoutByCommitID(headSha1, aFileInSplit);
            } else if (modifiedInHead && modifiedInOther) {
                if (!existInHead && !existInOther) {
                    continue;
                } else if (existInHead && !existInOther) {
                    isConflict = true;
                    meetConflict(aFileInSplit, headCommit,
                            otherCommit, existInHead, existInOther);
                    staging.getAdd().put(aFileInSplit,
                            readContentsAsString(join(CWD, aFileInSplit)));
                } else if (existInOther && !existInHead) {
                    isConflict = true;
                    meetConflict(aFileInSplit, headCommit,
                            otherCommit, existInHead, existInOther);
                    staging.getAdd().put(aFileInSplit,
                            readContentsAsString(join(CWD, aFileInSplit)));
                } else if (otherCommit.getBlobs(aFileInSplit).equals(
                        headCommit.getBlobs(aFileInSplit))) {
                    continue;
                } else {
                    isConflict = true;
                    meetConflict(aFileInSplit, headCommit,
                            otherCommit, existInHead, existInOther);
                    staging.getAdd().put(aFileInSplit,
                            readContentsAsString(join(CWD, aFileInSplit)));
                }
            } else if (!modifiedInHead && !existInOther) {
                join(CWD, aFileInSplit).delete();
                staging.getRm().add(aFileInSplit);
            } else if (modifiedInHead && !existInOther) {
                isConflict = true;
                meetConflict(aFileInSplit, headCommit,
                        otherCommit, existInHead, existInOther);
                staging.getAdd().put(aFileInSplit,
                        readContentsAsString(join(CWD, aFileInSplit)));
            } else if (!modifiedInOther && !existInHead) {
                File path = join(CWD, aFileInSplit);
                if (path.exists()) {
                    path.delete();
                }
            }
        }
        saveStage(staging);
        return isConflict;
    }

    private static boolean mergeNotInSplitCase(
            ArrayList<String> filesInHead,
                                            Commit otherCommit,
                                            Commit splitCommit,
                                            boolean isConflict,
                                            StagingArea staging,
                                            String headSha1,
                                            ArrayList<String> filesInOther,
                                            String otherSha1) {
        Commit headCommit = pullHeadCommit();
        for (String aFileInHead : filesInHead) {
            boolean existInHead = existInHead(aFileInHead, headCommit);
            boolean existInOther = existInOther(aFileInHead, otherCommit);
            if (!existInSplit(aFileInHead, splitCommit)) {
                if (existInHead && existInOther) {
                    if (!otherCommit.getBlobs(aFileInHead).equals(
                            headCommit.getBlobs(aFileInHead))) {
                        isConflict = true;
                        meetConflict(aFileInHead, headCommit,
                                otherCommit, existInHead, existInOther);
                        staging.getAdd().put(aFileInHead,
                                readContentsAsString(join(CWD, aFileInHead)));
                    }
                } else if (!existInOther) {
                    checkoutByCommitID(headSha1, aFileInHead);
                }
            }
        }
        for (String aFileInOther : filesInOther) {
            boolean existInHead = existInHead(aFileInOther, headCommit);
            boolean existInOther = existInOther(aFileInOther, otherCommit);
            if (!existInSplit(aFileInOther, splitCommit)) {
                if (existInHead && existInOther) {
                    if (!otherCommit.getBlobs(aFileInOther).equals(
                            headCommit.getBlobs(aFileInOther))) {
                        isConflict = true;
                        meetConflict(aFileInOther, headCommit,
                                otherCommit, existInHead, existInOther);
                        staging.getAdd().put(aFileInOther,
                                readContentsAsString(join(CWD, aFileInOther)));
                    }
                } else if (!existInHead) {
                    checkoutByCommitID(otherSha1, aFileInOther);
                    staging.getAdd().put(aFileInOther,
                            otherCommit.getBlobs(aFileInOther));
                }
            }
        }
        return isConflict;
    }

    private static void mergeErrorCase1(StagingArea staging,
                                        TreeMap<String, String> allBranches,
                                        String otherName) {
        if (!staging.getAdd().isEmpty() || !staging.getRm().isEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        if (!allBranches.containsKey(otherName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (getCurrBranchName().equals(otherName)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
    }

    private static void mergeErrorCase2(ArrayList<String> filesInHead,
                                        ArrayList<String> filesInOther) {
        for (String files : filesInOther) {
            if (!filesInHead.contains(files)) {
                if (join(CWD, files).exists()) {
                    System.out.println("There is an untracked file in the way; "
                            + "delete it, or add and commit it first.");
                    System.exit(0);
                }
            }
        }
    }

    private static void mergeErrorCase3(String headSha1, String otherSha1,
                                        String splitSha1,
                                        String otherName) throws IOException {
        if (otherSha1.equals(splitSha1)) {
            System.out.println(
                    "Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
        if (headSha1.equals(splitSha1)) {
            checkoutByBranch(otherName);
            System.out.println("Current branch fast-forwarded.");
            System.exit(0);
        }
    }

    private static void meetConflict(String file, Commit headCommit,
                                     Commit givenCommit,
                                     boolean existInHead,
                                     boolean existInGiven) {
        File pathToCWD = join(CWD, file);
        if (existInHead && existInGiven) {
            writeContents(pathToCWD, "<<<<<<< HEAD\n"
                    + headCommit.getBlobs(file) + "=======\n"
                    + givenCommit.getBlobs(file)
                    + ">>>>>>>\n");
        } else if (existInHead && !existInGiven) {
            writeContents(pathToCWD, "<<<<<<< HEAD\n"
                    + headCommit.getBlobs(file) + "=======\n" + ">>>>>>>\n");
        } else if (!existInHead && existInGiven) {
            writeContents(pathToCWD, "<<<<<<< HEAD\n"
                    + "=======\n" + givenCommit.getBlobs(file) + ">>>>>>>\n");
        } else {
            writeContents(pathToCWD, "<<<<<<< HEAD\n"
                    + "=======\n" + ">>>>>>>\n");
        }
    }

    private static String splitPointSha1(String branchName) {
        TreeMap<String, String> allBranches = getAllBranches();
        String headCommitSha1 = pullHeadSha1();
        String branchSha1 = allBranches.get(branchName);
        ArrayList<String> headSeries = splitHelper(headCommitSha1);
        if (headSeries.contains(branchSha1)) {
            return branchSha1;
        }
        ArrayList<String> branchSeries = splitHelper(branchSha1);
        String result = null;
        for (String closest : headSeries) {
            if (branchSeries.contains(closest)) {
                result = closest;
                break;
            }
        }
        return result;
    }

    private static ArrayList<String> splitHelper(String headSha1) {
        ArrayList<String> headSeries = new ArrayList<>();
        ArrayDeque<String> headBFS = new ArrayDeque<>();
        headBFS.addLast(headSha1);
        while (!headBFS.isEmpty()) {
            String sha1 = headBFS.removeFirst();
            Commit commit = getCommit(sha1);
            headSeries.add(sha1);
            if (commit.getParent1() != null) {
                headBFS.addLast(commit.getParent1().getSha1());
                if (commit.getParent2() != null) {
                    headBFS.addLast(commit.getParent2().getSha1());
                }
            }
        }
        return headSeries;
    }

    private static boolean existInSplit(String fileName, Commit splitCommit) {
        return splitCommit.fileExistInCommit(fileName);
    }

    private static boolean modifiedInOther(String fileName,
                                           Commit otherCommit,
                                           String splitSha1) {
        Commit splitCommit = getCommit(splitSha1);
        String splitBlobs = splitCommit.getBlobs(fileName);
        String otherBlobs = otherCommit.getBlobs(fileName);
        boolean result = false;
        if (existInOther(fileName, otherCommit)) {
            if (!sha1(serialize(splitBlobs)).equals
                    (sha1(serialize(otherBlobs)))) {
                result = true;
            }
        } else {
            result = true;
        }
        return result;
    }

    private static boolean modifiedInHead(String fileName, String splitSha1) {
        boolean result = false;
        Commit splitCommit = getCommit(splitSha1);
        String splitBlobs = splitCommit.getBlobs(fileName);
        Commit headCommit = pullHeadCommit();
        String headBlobs = headCommit.getBlobs(fileName);
        if (existInHead(fileName, headCommit)) {
            if (!sha1(serialize(splitBlobs)).equals
                    (sha1(serialize(headBlobs)))) {
                result = true;
            }
        } else {
            result = true;
        }
        return result;
    }

    private static boolean existInOther(String fileName, Commit otherCommit) {
        return otherCommit.fileExistInCommit(fileName);
    }

    private static boolean existInHead(String fileName, Commit headCommit) {
        return headCommit.fileExistInCommit(fileName);
    }

    private static String getSha1FromCommit(Commit commit) {
        return sha1(serialize(commit));
    }

    private static Commit getCommit(String sha1) {
        File path = join(COMMITS_FOLDER, sha1);
        return readObject(path, Commit.class);
    }

    private static String pullHeadSha1() {
        @SuppressWarnings("unchecked")
        TreeMap<String, String> branchess =
                readObject(BRANCHES_FOLDER, TreeMap.class);
        String headCommitSha1 = branchess.get("HEAD");
        return headCommitSha1;
    }

    private static Commit pullHeadCommit() {
        String headSha1 = pullHeadSha1();
        return getCommit(headSha1);
    }

    private static String pullBranchSha1(String branchName) {
        @SuppressWarnings("unchecked")
        TreeMap<String, String> branchess =
                readObject(BRANCHES_FOLDER, TreeMap.class);
        String branchCommitSha1 = branchess.get(branchName);
        return branchCommitSha1;
    }

    @SuppressWarnings("unchecked")
    private static TreeMap<String, String> getAllBranches() {
        return readObject(BRANCHES_FOLDER, TreeMap.class);
    }

    private static String getCurrBranchName() {
        @SuppressWarnings("unchecked")
        TreeMap<String, String> branchess =
                readObject(BRANCHES_FOLDER, TreeMap.class);
        TreeMap<String, String> copyBranches = branchess;
        String curr = copyBranches.get("CURBRANCH");
        if (curr == null) {
            String headSha1 = pullHeadSha1();
            copyBranches.remove("HEAD");
            for (Map.Entry<String, String> i : copyBranches.entrySet()) {
                if (i.getValue().equals(headSha1)) {
                    copyBranches.put("CURBRANCH", i.getKey());
                    curr = copyBranches.get("CURBRANCH");
                    break;
                }
            }
        }
        return curr;
    }

    private static StagingArea getStaging() {
        return readObject(StagingArea.STAGINGAREA_FOLDER, StagingArea.class);
    }

    private static void saveStage(StagingArea staging) {
        writeObject(StagingArea.STAGINGAREA_FOLDER, staging);
    }


}
