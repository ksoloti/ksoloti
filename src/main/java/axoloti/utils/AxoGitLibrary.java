package axoloti.utils;

import axoloti.Axoloti;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.StashApplyCommand;
import org.eclipse.jgit.api.StashCreateCommand;
import org.eclipse.jgit.api.StashDropCommand;
import org.eclipse.jgit.api.StashListCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

public class AxoGitLibrary extends AxolotiLibrary {

    private static final Logger LOGGER = Logger.getLogger(AxoGitLibrary.class.getName());

    public static String TYPE = "git";

    public AxoGitLibrary(String id, String type, String lloc, boolean e, String rloc, boolean auto) {
        super(id, type, lloc, e, rloc, auto);
    }

    public AxoGitLibrary() {
    }

    public AxoGitLibrary(AxoGitLibrary other) {
        super(other);
    }

    @Override
    public void reportStatus() {
        File f = new File(getLocalLocation());
        if (!f.exists()) {
            LOGGER.log(Level.WARNING, "Local directory missing: " + logDetails());
        }

        Git git = getGit();
        if (git != null) {
            reportStatus(git);
            git.getRepository().close();
        } else {
            LOGGER.log(Level.WARNING, "Status failed - Cannot find submodule: " + logDetails());
        }
    }

    @Override
    public void sync() {
        // get repository
        Git git = getGit();

        if (git != null) {
            if (!pull(git)) {
                git.getRepository().close();
                return;
            }
            boolean isDirty = isDirty(git);
            if (isDirty) {
                LOGGER.log(Level.INFO, "Modifications detected: " + logDetails());
            }
            if (isDirty && isAuth()) {
                if (!add(git)) {
                    git.getRepository().close();
                    return;
                }
                if (!commit(git)) {
                    git.getRepository().close();
                    return;
                }
                if (!push(git)) {
                    git.getRepository().close();
                    return;
                }
                LOGGER.log(Level.INFO, "Modifications uploaded: " + logDetails());
                reportStatus(git);
            }
            if (!checkout(git, false)) {
                git.getRepository().close();
                return;
            }
            LOGGER.log(Level.INFO, "Sync successful: " + logDetails());
            git.getRepository().close();
        } else {
            LOGGER.log(Level.WARNING, "Repo sync failed - Cannot find submodule: " + logDetails());
        }
    }
        
    @Override
    public void init(boolean delete) {
        File ldir = new File(getLocalLocation());

        if (!usingSubmodule()) {
            if (getRemoteLocation() == null || getRemoteLocation().length() == 0) {
                LOGGER.log(Level.WARNING, "Initialisation failed - no remote specified: " + logDetails());
                return;
            }

            if (delete && ldir.exists()) {
                try {
                    delete(ldir);
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, "Error trying to init repo: " + ex.getMessage());
                    ex.printStackTrace(System.out);
                }
            }

            if (!ldir.exists()) {
                ldir.mkdirs();
            }

            String branch = getBranch();
            CloneCommand cmd = Git.cloneRepository();
            cmd.setURI(getRemoteLocation());
            cmd.setDirectory(ldir);
            cmd.setBranch(branch);
            if (isAuth()) {
                cmd.setCredentialsProvider(new UsernamePasswordCredentialsProvider(getUserId(), getPassword()));
            }
            try {
                Git git = cmd.call();
                git.getRepository().close();
                LOGGER.log(Level.INFO, "Repo initialisation successful: " + logDetails());
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Repo initialisation failed: " + getId() + ", " + ex.getMessage());
                ex.printStackTrace(System.out);
                return;
            }
        } else {
            LOGGER.log(Level.INFO, "Developer mode - do NOT clone repo: " + logDetails());
        }
        // sync afterwards to ensure on correct branch
        sync();
    }

    @Override
    public boolean stashChanges(String ref) {
        Git git = getGit();
        if (git != null) {
            boolean ret = createStash(git, ref);
            git.getRepository().close();
            return ret;
        }
        LOGGER.log(Level.WARNING, "stashChanges failed - could not find repo: " + getId());
        return false;
    }

    @Override
    public boolean applyStashedChanges(String ref) {
        Git git = getGit();
        if (git != null) {
            boolean ret = applyStash(git, ref);

            if (ret) {
                ret = dropStash(git, ref);
            }
            git.getRepository().close();
            return ret;
        }
        LOGGER.log(Level.WARNING, "applyStashedChanges failed - could not find repo: " + getId());
        return false;
    }

    @Override
    public String getCurrentBranch() {
        Git git = getGit();
        if (git != null) {
            try {
                return git.getRepository().getBranch();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Error trying to get current branch: " + ex.getMessage());
                ex.printStackTrace(System.out);
                return getBranch();
            }
        }
        LOGGER.log(Level.WARNING, "getCurrentBranch failed - could not find repo: " + getId());
        return getBranch();
    }

    @Override
    public void upgrade() {
        String ref = "AxolotiUpgrade";
        Git git = getGit();
        if (git != null) {
            boolean ret = createStash(git, ref);
            if (ret) {
                // update remote branches
                fetch(git);
                // switch to branch
                checkout(git, true);
                ret = applyStash(git, ref);
                if (ret) {
                    dropStash(git, ref);
                }
            }
            git.getRepository().close();
            return;
        }
        LOGGER.log(Level.WARNING, "Upgrade failed - could not find repo: " + getId());
    }

    private Git getGit() {
        Git git = null;
        try {
            Repository repository;
            if (usingSubmodule()) {
                // special case, in developer mode, we have the repos as sub modules, these need to be accessed via the parent repo
                String relDir = System.getProperty(Axoloti.HOME_DIR);
                Git parent = Git.open(new File(relDir));
                File ldir = new File(getLocalLocation());
                String ldirstr = ldir.getName();
                repository = SubmoduleWalk.getSubmoduleRepository(parent.getRepository(), ldirstr);
                if (repository == null) {
                    LOGGER.log(Level.WARNING, "getGit failed - could not find submodule: " + logDetails());
                    return null;
                }
            } else {
                repository = Git.open(new File(getLocalLocation())).getRepository();
            }
            git = new Git(repository);

        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error trying to find git submodule: " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
        return git;
    }

    private boolean createStash(Git git, String ref) {
        StashCreateCommand cmd = git.stashCreate();
        cmd.setIncludeUntracked(true);
        try {
            cmd.setWorkingDirectoryMessage(ref);
            cmd.call();
            LOGGER.log(Level.INFO, "Changes stashed successfully: " + logDetails() + ", " + ref);

            return true;
        } catch (GitAPIException ex) {
            LOGGER.log(Level.WARNING, "Git stash failed: " + logDetails() + ", " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
        return false;

    }

    private int findStash(Git git, String ref) {
        try {
            StashListCommand stashList = git.stashList();
            Collection<RevCommit> stashedRefs = stashList.call();
            if (stashedRefs.isEmpty()) {
                return -1;
            }

            int idx = 0;
            for (RevCommit i : stashedRefs) {
                if (i.getShortMessage().equals(ref)) {
                    return idx;
                }
                idx++;
            }
            return -1;
        } catch (GitAPIException ex) {
            LOGGER.log(Level.WARNING, "git applyStash (findStash) failed: " + logDetails() + ", " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
        return -1;
    }

    private boolean applyStash(Git git, String ref) {
        int idx = findStash(git, ref);
        if (idx < 0) {
            return false;
        }

        String sref = "stash@{" + Integer.toString(idx) + "}";

        try {
            StashApplyCommand cmd = git.stashApply();
            cmd.setStashRef(sref);
            cmd.setRestoreUntracked(true);
            cmd.call();
            LOGGER.log(Level.INFO, "Changes applied successfully: " + logDetails() + ", " + ref);
            return true;
        } catch (GitAPIException ex) {
            LOGGER.log(Level.WARNING, "applyStash (stashApply) failed: " + logDetails() + ", " + ex.getMessage());
            ex.printStackTrace(System.out);
        }

        return false;
    }

    private boolean dropStash(Git git, String ref) {
        int idx = findStash(git, ref);
        if (idx < 0) {
            return false;
        }

        try {
            StashDropCommand cmd = git.stashDrop();
            cmd.setStashRef(idx);
            cmd.call();
            LOGGER.log(Level.INFO, "Drop stash successful: " + logDetails() + ", " + ref);
            return true;
        } catch (GitAPIException ex) {
            LOGGER.log(Level.WARNING, "applyStash (dropStash) failed: " + logDetails() + ", " + ex.getMessage());
            ex.printStackTrace(System.out);
        }

        return false;
    }

    private boolean pull(Git git) {
        PullCommand cmd = git.pull();
        if (isAuth()) {
            cmd.setCredentialsProvider(new UsernamePasswordCredentialsProvider(getUserId(), getPassword()));
        }
        try {
            PullResult res = cmd.call();
            if (!res.isSuccessful()) {
                LOGGER.log(Level.WARNING, "Sync (pull) failed: " + logDetails());
                return false;
            }
            return true;

        } catch (GitAPIException ex) {
            LOGGER.log(Level.WARNING, "Sync (pull) failed: " + logDetails() + ", " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
        return false;
    }

    private boolean checkout(Git git, boolean force) {
        String branch = getBranch();

        // check to see if already checked out
        try {
            if (branch.equals(git.getRepository().getBranch())) {
                // has the user changed a brannch, that they are not authorised to change
                boolean isDirty = isDirty(git);
                if (isDirty && !isAuth()) {
                    LOGGER.log(Level.INFO, "Unauthorised changes, resetting: " + logDetails());
                    CheckoutCommand cmd = git.checkout();
                    cmd.setForceRefUpdate(force);
                    cmd.setAllPaths(true);
                    //cmd.setName(branch);
                    try {
                        cmd.call();
                        CheckoutResult res = cmd.getResult();
                        if (!res.getStatus().equals(CheckoutResult.Status.OK)) {
                            LOGGER.log(Level.WARNING, "Sync (checkout) failed: " + logDetails());
                            return false;
                        }
                        return true;
                    } catch (GitAPIException ex) {
                        LOGGER.log(Level.WARNING, "Sync (checkout) failed: " + logDetails() + ", " + ex.getMessage());
                        ex.printStackTrace(System.out);
                    }
                }
                return true;
            }
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Sync (check local branch) failed: " + logDetails() + ", " + ex.getMessage());
            ex.printStackTrace(System.out);
        }

        // check to see if branch is already available locally
        String localref = "refs/heads/" + branch;
        boolean localAvailable = false;
        List<Ref> bl_call;
        try {
            bl_call = git.branchList().call();
            for (Ref ref : bl_call) {
                if (ref.getName().equals(localref)) {
                    localAvailable = true;
                }
            }
        } catch (GitAPIException ex) {
            LOGGER.log(Level.WARNING, "Sync (branch list) failed: " + logDetails() + ", " + ex.getMessage());
            ex.printStackTrace(System.out);
        }

        CheckoutCommand cmd = git.checkout();
        cmd.setName(branch);
        if (!localAvailable) {
            // create local branch pointing to remote branch
            cmd.setCreateBranch(true);
            cmd.setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM);
            cmd.setStartPoint("origin/" + branch);
        }
        try {
            cmd.call();
            CheckoutResult res = cmd.getResult();
            if (!res.getStatus().equals(CheckoutResult.Status.OK)) {
                LOGGER.log(Level.WARNING, "Sync (checkout) failed: " + logDetails());
                return false;
            }
            return true;
        } catch (GitAPIException ex) {
            LOGGER.log(Level.WARNING, "Sync (checkout) failed: " + logDetails() + ", " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
        return false;

    }

    private boolean add(Git git) {
        AddCommand cmd = git.add();
        if (getContributorPrefix() != null && getContributorPrefix().length() > 0) {
            cmd.addFilepattern("objects/" + getContributorPrefix());
            cmd.addFilepattern("patches/" + getContributorPrefix());
        } else {
            cmd.addFilepattern(".");
        }
        cmd.setUpdate(false);
        try {
            cmd.call();
            return true;
        } catch (GitAPIException ex) {
            LOGGER.log(Level.WARNING, "Sync (add) failed: " + logDetails() + ", " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
        return false;

    }

    private void fetch(Git git) {
        FetchCommand cmd = git.fetch();
        try {
            cmd.call();
        } catch (GitAPIException ex) {
            LOGGER.log(Level.WARNING, "Upgrade (fetch) failed: " + logDetails() + ", " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
    }

    private boolean commit(Git git) {
        CommitCommand cmd = git.commit();
        cmd.setAll(true);
        cmd.setMessage("commit from axoloti UI");
        cmd.setAllowEmpty(false);
        try {
            cmd.call();
            return true;
        } catch (GitAPIException ex) {
            LOGGER.log(Level.WARNING, "Sync (commit) failed: " + logDetails() + ", " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
        return false;
    }

    private boolean reportStatus(Git git) {
        StatusCommand cmd = git.status();
        try {
            String overallStatus = "OK";
            Status status = cmd.call();

            StringBuilder details = new StringBuilder();

            try {
                details.append(git.getRepository().getBranch());
            } catch (IOException ex) {
                details.append("branch error");
            }
            details.append(", ");
            if (status.isClean()) {
                details.append("clean");
            } else {
                details.append("dirty");
            }

            LOGGER.log(Level.INFO, "Status {0}: {1} ({2})", new Object[]{overallStatus, logDetails(), details.toString()});
            if (!status.isClean()) {
                LOGGER.log(Level.INFO, "Changes for: " + logDetails());
                for (String f : status.getAdded()) {
                    LOGGER.log(Level.INFO, "Added: " + f);
                }
                for (String f : status.getChanged()) {
                    LOGGER.log(Level.INFO, "Changed: " + f);
                }
                for (String f : status.getConflicting()) {
                    LOGGER.log(Level.INFO, "Conflicting: " + f);
                }
                for (String f : status.getMissing()) {
                    LOGGER.log(Level.INFO, "Missing: " + f);
                }
                for (String f : status.getModified()) {
                    LOGGER.log(Level.INFO, "Modified: " + f);
                }
                for (String f : status.getRemoved()) {
                    LOGGER.log(Level.INFO, "Removed: " + f);
                }
                for (String f : status.getUntracked()) {
                    LOGGER.log(Level.INFO, "Untracked: " + f);
                }
                for (String f : status.getUntrackedFolders()) {
                    LOGGER.log(Level.INFO, "Untracked folder(s): " + f);
                }
                for (String f : status.getUncommittedChanges()) {
                    LOGGER.log(Level.INFO, "Uncommited: " + f);
                }
            }
            return true;
        } catch (GitAPIException | NoWorkTreeException ex) {
            LOGGER.log(Level.INFO, "Status EXCEPTION - " + logDetails() + ", " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
        return false;
    }

    private boolean push(Git git) {
        PushCommand cmd = git.push();
        if (isAuth()) {
            cmd.setCredentialsProvider(new UsernamePasswordCredentialsProvider(getUserId(), getPassword()));
        }

        try {
            cmd.call();
            return true;
        } catch (GitAPIException ex) {
            LOGGER.log(Level.WARNING, "Sync (push) failed: " + logDetails() + ", " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
        return false;
    }

    private boolean isDirty(Git git) {
        try {
            StatusCommand cmd = git.status();
            String pre = "";
            if (getContributorPrefix() != null && getContributorPrefix().length() > 0) {
                pre = getContributorPrefix() + "/";
            }
            cmd.addPath("objects/" + pre);
            cmd.addPath("patches/" + pre);
            Status status = cmd.call();
            return !status.isClean();
        } catch (GitAPIException | NoWorkTreeException ex) {
            LOGGER.log(Level.WARNING, "Sync (isdirty) failed: " + logDetails() + ex.getMessage());
            ex.printStackTrace(System.out);
        }
        return false;
    }

    private boolean usingSubmodule() {
        return false;
    }

    private boolean isAuth() {
        return getUserId() != null && getUserId().length() > 0;
    }

    private String logDetails() {
        StringBuilder str = new StringBuilder();
        str.append(getId()).append(" (").append(getBranch());
        if (getUserId() == null || getUserId().length() == 0) {
            str.append(", anon");
        } else {
            str.append(", ").append(getUserId());
        }
        str.append(")");
        return str.toString();
    }
}
