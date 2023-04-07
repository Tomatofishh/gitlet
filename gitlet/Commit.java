package gitlet;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TreeMap;

import static gitlet.Utils.sha1;

public class Commit implements Serializable {

    /** Commit message, every commit has one. */
    private final String message;

    /** Time stamp for log(). */
    private String timestamp;

    /** Time stamp format for log(). */
    private static final SimpleDateFormat TIMESTAMPFORMAT =
            new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");

    /** commit parent1, every commit has to have a parent, except initial
     * commit, to keep track of the previous commits.*/
    private Commit parent1;

    /** merge commit parent2, it would be null when there's no merge. */
    private Commit parent2;

    /** contents in commit, as a TreeMap with key is file name, and value is
     * blob of the file as strings. */
    private final TreeMap<String, String> contents = new TreeMap<>();


    public Commit(String messageee, Commit parenttt1, Commit parenttt2) {
        this.message = messageee;
        this.parent1 = parenttt1;
        this.parent2 = parenttt2;
        this.timestamp = TIMESTAMPFORMAT.format(new Date());
        if (this.parent1 == null) {
            Date startTime = new Date(0);
            this.timestamp = TIMESTAMPFORMAT.format(startTime);
        }
    }

    public String getMessage() {
        return this.message;
    }

    public String getTimestamp() {
        return this.timestamp;
    }

    public Commit getParent1() {
        return this.parent1;
    }

    public Commit getParent2() {
        return this.parent2;
    }

    public TreeMap<String, String> getContents() {
        return this.contents;
    }

    public ArrayList<String> getAllFileNames() {
        ArrayList<String> result = new ArrayList<>();
        for (String fileName : this.contents.keySet()) {
            result.add(fileName);
        }
        return result;
    }

    public String getSha1() {
        return sha1((Object) Utils.serialize(this));
    }

    public String getBlobs(String fileName) {
        return this.contents.get(fileName);
    }

    public boolean fileExistInCommit(String fileName) {
        return this.contents.containsKey(fileName);
    }

}
