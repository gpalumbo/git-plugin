package hudson.plugins.git.browser;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.EnvironmentContributor;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.TaskListener;
import hudson.plugins.git.GitChangeSet;
import hudson.plugins.git.GitChangeSet.Path;
import hudson.scm.EditType;
import hudson.scm.RepositoryBrowser;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Git Browser URLs
 */
public class GitList extends GitRepositoryBrowser {

    private static final long serialVersionUID = 1L;

    @DataBoundConstructor
    public GitList(String repoUrl) {
        super(repoUrl);
    }

    @Override
    public URL getChangeSetLink(GitChangeSet changeSet) throws IOException {
        URL url = getUrl();
        return new URL(url, url.getPath() + "commit/" + changeSet.getId().toString());
    }

    /**
     * Creates a link to the file diff.
     * http://[GitList URL]/commit/6c99ffee4cb6d605d55a1cc7cb47f25a443f7f54#N
     *
     * @param path affected file path
     * @return diff link
     * @throws IOException
     */
    @Override
    public URL getDiffLink(Path path) throws IOException {
        if(path.getEditType() != EditType.EDIT || path.getSrc() == null || path.getDst() == null
            || path.getChangeSet().getParentCommit() == null) {
            return null;
        }
        return getDiffLinkRegardlessOfEditType(path);
    }

    /**
     * Return a diff link regardless of the edit type by appending the index of the pathname in the changeset.
     *
     * @param path
     * @return url for differences
     * @throws IOException
     */
    private URL getDiffLinkRegardlessOfEditType(Path path) throws IOException {
        final GitChangeSet changeSet = path.getChangeSet();
        final ArrayList<String> affectedPaths = new ArrayList<String>(changeSet.getAffectedPaths());
        Collections.sort(affectedPaths);
        final String pathAsString = path.getPath();
        final int i = Collections.binarySearch(affectedPaths, pathAsString);
        assert i >= 0;
        return new URL(getChangeSetLink(changeSet), "#" + String.valueOf(i + 1)); //GitList diff indices begin at 1
    }

    /**
     * Creates a link to the file.
     * http://[GitList URL]/blob/6c99ffee4cb6d605d55a1cc7cb47f25a443f7f54/src/gitlist/Application.php
     *
     * @param path file
     * @return file link
     * @throws IOException
     */
    @Override
    public URL getFileLink(Path path) throws IOException {
        if (path.getEditType().equals(EditType.DELETE)) {
            return getDiffLinkRegardlessOfEditType(path);
        } else {
            final String spec = "blob/" + path.getChangeSet().getId() + "/" + path.getPath();
            URL url = getUrl();
            return new URL(url, url.getPath() + spec);
        }
    }

    @Extension
    public static class GitListDescriptor extends Descriptor<RepositoryBrowser<?>> {
        public String getDisplayName() {
            return "gitlist";
        }

        @Override
        public GitList newInstance(StaplerRequest req, JSONObject jsonObject) throws FormException {
            return req.bindJSON(GitList.class, jsonObject);
        }
    }
}
