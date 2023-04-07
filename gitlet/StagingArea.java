package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.TreeMap;

import static gitlet.Utils.*;

public class StagingArea implements Serializable {
    /** creates a STAGINGAREA_FOLDER to store the treemap for staging. */
    static final File STAGINGAREA_FOLDER =
            join(Bloop.GITLET_FOLDER, "stagingArea");

    /** Treemap for stage of addition, the key is file name and the value
     * is blob of the file as strings. */
    private TreeMap<String, String> stageAdd;

    /** ArrayList for stage of removal, the key is file name. */
    private ArrayList<String> stageRm;


    public StagingArea() {
        stageAdd = new TreeMap<>();
        stageRm = new ArrayList<>();
    }

    public TreeMap<String, String> getAdd() {
        return stageAdd;
    }

    public ArrayList<String> getRm() {
        return stageRm;
    }

    public void clearAll() {
        stageRm.clear();
        stageAdd.clear();
    }

    public void clearAdd() {
        stageAdd.clear();
    }

    public void clearRm() {
        stageRm.clear();
    }

}
