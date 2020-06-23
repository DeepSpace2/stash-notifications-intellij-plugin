import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vcs.BranchChangeListener;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitUtil;
import git4idea.repo.GitRepository;
import git4idea.ui.GitUnstashDialog;
import git4idea.ui.StashInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class StashNotificationBranchChangeListener implements BranchChangeListener {
    @Override
    public void branchWillChange(@NotNull String branchName) { }

    @Override
    public void branchHasChanged(@NotNull String branchName) {
        Project project = ProjectManager.getInstance().getOpenProjects()[0];
        GitRepository repo = GitUtil.getRepositories(project).iterator().next();
        final VirtualFile root = repo.getRoot();
        List<StashInfo> stash;
        try {
            stash = git4idea.stash.GitStashUtils.loadStashStack(project, root);
        } catch (VcsException e) {
            e.printStackTrace();
            return;
        }

        for (StashInfo stashEntry: stash) {
            String stashBranch = stashEntry.getBranch();
            if (stashBranch == null) {
                continue;
            }
            // naively figuring out the branch name this stash entry was created on
            stashBranch = stashBranch.substring(stashBranch.indexOf("on") + 3).strip();

            if (stashBranch.equals(branchName)) {
                final NotificationGroup NOTIFICATION_GROUP = new NotificationGroup(
                        "Stash Notifications Plugin",
                        NotificationDisplayType.BALLOON,
                        true);
                Notification notification = NOTIFICATION_GROUP.createNotification(
                        "Notice: This branch has stash",
                        NotificationType.INFORMATION);
                notification.addAction(new NotificationAction("Show Stash Dialog") {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                        GitUnstashDialog.showUnstashDialog(project, List.of(root), root);
                    }
                });
                notification.notify(project);
                break;
            }
        }
    }
}
