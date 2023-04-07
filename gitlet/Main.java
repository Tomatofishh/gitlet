package gitlet;

import java.io.IOException;


/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Qingyi Fang
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) throws IOException {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        if (!args[0].equals("init")) {
            if (!Bloop.GITLET_FOLDER.exists()) {
                System.out.println("Not in an initialized Gitlet directory.");
                System.exit(0);
            }
        }

        if (args[0].equals("init") || args[0].equals("add")
                || args[0].equals("commit")
                || args[0].equals("log") || args[0].equals("checkout")) {
            mainHelper1(args);
        } else if (args[0].equals("rm") || args[0].equals("global-log")
                || args[0].equals("find")
                || args[0].equals("status") || args[0].equals("branch")
                || args[0].equals("rm-branch")) {
            mainHelper2(args);
        } else if (args[0].equals("reset") || args[0].equals("merge")) {
            mainHelper3(args);
        } else {
            System.out.println("No command with that name exists.");
            System.exit(0);
        }



    }

    public static void mainHelper1(String... args) throws IOException {
        if (args[0].equals("init")) {
            if (iHateStyleCheck(1, args)) {
                Bloop.init();
            } else {
                System.exit(0);
            }
        }
        if (args[0].equals("add")) {
            if (iHateStyleCheck(2, args)) {
                Bloop.add(args[1]);
            } else {
                System.exit(0);
            }
        }
        if (args[0].equals("commit")) {
            if (iHateStyleCheck(2, args)) {
                Bloop.commit(args[1], null);
            } else {
                System.exit(0);
            }
        }
        if (args[0].equals("log")) {
            if (iHateStyleCheck(1, args)) {
                Bloop.log();
            } else {
                System.exit(0);
            }
        }
        if (args[0].equals("checkout")) {
            if (args.length == 2) {
                Bloop.checkoutByBranch(args[1]);
            } else if (args.length == 3) {
                if (args[1].equals("--")) {
                    Bloop.checkoutByFileName(args[2]);
                } else {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
            } else if (args.length == 4) {
                if (args[2].equals("--")) {
                    Bloop.checkoutByCommitID(args[1], args[3]);
                    System.exit(0);
                } else {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
            } else {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
        }
    }

    public static void mainHelper2(String... args) throws IOException {
        if (args[0].equals("rm")) {
            if (iHateStyleCheck(2, args)) {
                Bloop.rmCommand(args[1]);
            } else {
                System.exit(0);
            }
        }
        if (args[0].equals("global-log")) {
            if (iHateStyleCheck(1, args)) {
                Bloop.globalLog();
            } else {
                System.exit(0);
            }
        }
        if (args[0].equals("find")) {
            if (iHateStyleCheck(2, args)) {
                Bloop.find(args[1]);
            } else {
                System.exit(0);
            }
        }
        if (args[0].equals("status")) {
            if (iHateStyleCheck(1, args)) {
                Bloop.status();
            } else {
                System.exit(0);
            }
        }
        if (args[0].equals("branch")) {
            if (iHateStyleCheck(2, args)) {
                Bloop.branchCommand(args[1]);
            } else {
                System.exit(0);
            }
        }
        if (args[0].equals("rm-branch")) {
            if (iHateStyleCheck(2, args)) {
                Bloop.rmBranch(args[1]);
            }
        } else {
            System.exit(0);
        }
    }

    public static void mainHelper3(String... args) throws IOException {
        if (args[0].equals("reset")) {
            if (iHateStyleCheck(2, args)) {
                Bloop.reset(args[1]);
            } else {
                System.exit(0);
            }
        }
        if (args[0].equals("merge")) {
            if (iHateStyleCheck(2, args)) {
                Bloop.mergeCommand(args[1]);
            } else {
                System.exit(0);
            }
        }
    }

    static boolean iHateStyleCheck(int length, String... args) {
        if (args.length == length) {
            return true;
        }
        System.out.println("Incorrect operands.");
        return false;
    }


}
