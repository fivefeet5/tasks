/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.reminders;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.Broadcaster;
import org.tasks.R;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingBroadcastReceiver;
import org.tasks.intents.TaskIntents;
import org.tasks.notifications.NotificationManager;
import org.tasks.preferences.Preferences;
import org.tasks.reminders.NotificationActivity;
import org.tasks.reminders.NotificationDialog;

import javax.inject.Inject;

public class Notifications extends InjectingBroadcastReceiver {

    private static final Logger log = LoggerFactory.getLogger(Notifications.class);

    /**
     * task id extra
     */
    public static final String ID_KEY = "id"; //$NON-NLS-1$

    /**
     * Action name for broadcast intent notifying that task was created from repeating template
     */
    public static final String EXTRAS_CUSTOM_INTENT = "intent"; //$NON-NLS-1$
    public static final String EXTRAS_NOTIF_ID = "notifId"; //$NON-NLS-1$

    /**
     * notification type extra
     */
    public static final String EXTRAS_TYPE = "type"; //$NON-NLS-1$
    public static final String EXTRAS_TITLE = "title"; //$NON-NLS-1$
    public static final String EXTRAS_TEXT = "text"; //$NON-NLS-1$
    public static final String EXTRAS_RING_TIMES = "ringTimes"; //$NON-NLS-1$

    @Inject TaskDao taskDao;
    @Inject @ForApplication Context context;
    @Inject NotificationManager notificationManager;
    @Inject Broadcaster broadcaster;
    @Inject Preferences preferences;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        handle(intent);
    }

    void handle(Intent intent) {
        long id = intent.getLongExtra(ID_KEY, 0);
        int type = intent.getIntExtra(EXTRAS_TYPE, (byte) 0);
        if (!showTaskNotification(id, type)) {
            notificationManager.cancel((int) id);
        }
    }

    /**
     * Show a new notification about the given task. Returns false if there was
     * some sort of error or the alarm should be disabled.
     */
    private boolean showTaskNotification(long id, int type) {
        Task task;
        try {
            task = taskDao.fetch(id, Task.ID, Task.TITLE, Task.HIDE_UNTIL, Task.COMPLETION_DATE,
                    Task.DUE_DATE, Task.DELETION_DATE, Task.REMINDER_FLAGS);
            if (task == null) {
                throw new IllegalArgumentException("cound not find item with id"); //$NON-NLS-1$
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }

        // you're done, or not yours - don't sound, do delete
        if (task.isCompleted() || task.isDeleted()) {
            return false;
        }

        // new task edit in progress
        if (TextUtils.isEmpty(task.getTitle())) {
            return false;
        }

        // it's hidden - don't sound, don't delete
        if (task.isHidden() && type == ReminderService.TYPE_RANDOM) {
            return true;
        }

        // task due date was changed, but alarm wasn't rescheduled
        boolean dueInFuture = task.hasDueTime() && task.getDueDate() > DateUtilities.now() ||
                !task.hasDueTime() && task.getDueDate() - DateUtilities.now() > DateUtilities.ONE_DAY;
        if ((type == ReminderService.TYPE_DUE || type == ReminderService.TYPE_OVERDUE) &&
                (!task.hasDueDate() || dueInFuture)) {
            return true;
        }

        // read properties
        String taskTitle = task.getTitle();
        boolean nonstopMode = task.isNotifyModeNonstop();
        boolean ringFiveMode = task.isNotifyModeFive();
        int ringTimes = nonstopMode ? -1 : (ringFiveMode ? 5 : 1);

        // update last reminder time
        task.setReminderLast(DateUtilities.now());
        taskDao.saveExisting(task);

        String text = context.getString(R.string.app_name);

        PendingIntent intent = createNotificationIntent(id, taskTitle);

        broadcaster.requestNotification((int) id, intent, type, taskTitle, text, ringTimes);
        return true;
    }

    private PendingIntent createNotificationIntent(final long id, final String taskTitle) {
        Intent intent = new Intent(context, NotificationActivity.class) {{
            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            setAction("NOTIFY" + id); //$NON-NLS-1$
            putExtra(NotificationActivity.EXTRA_TASK_ID, id);
            putExtra(NotificationActivity.EXTRA_TITLE, taskTitle);
        }};
        return PendingIntent.getActivity(context, (int) id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
