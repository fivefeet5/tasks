package org.tasks.reminders;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.widget.ArrayAdapter;

import org.tasks.Broadcaster;
import org.tasks.R;
import org.tasks.injection.InjectingDialogFragment;
import org.tasks.intents.TaskIntents;
import org.tasks.notifications.NotificationManager;

import javax.inject.Inject;

import static java.util.Arrays.asList;

public class NotificationDialog extends InjectingDialogFragment {

    @Inject NotificationManager notificationManager;
    @Inject Broadcaster broadcaster;

    private long taskId;
    private String title;
    private DialogInterface.OnDismissListener onDismissListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, asList(
                getString(R.string.TAd_actionEditTask),
                getString(R.string.rmd_NoA_snooze),
                getString(R.string.rmd_NoA_done)
        ));
        return new AlertDialog.Builder(getActivity(), R.style.Tasks_Dialog)
                .setTitle(title)
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                TaskIntents
                                        .getEditTaskStack(getActivity(), null, taskId)
                                        .startActivities();
                                notificationManager.cancel(taskId);
                                dismiss();
                                break;
                            case 1:
                                dismiss();
                                startActivity(new Intent(getActivity(), SnoozeActivity.class) {{
                                    setFlags(FLAG_ACTIVITY_NEW_TASK);
                                    putExtra(SnoozeActivity.EXTRA_TASK_ID, taskId);
                                }});
                                break;
                            case 2:
                                broadcaster.completeTask(taskId);
                                dismiss();
                                break;
                        }
                    }
                })
                .show();
    }

    public void setOnDismissListener(DialogInterface.OnDismissListener onDismissListener) {
        this.onDismissListener = onDismissListener;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (onDismissListener != null) {
            onDismissListener.onDismiss(dialog);
        }
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }
}
