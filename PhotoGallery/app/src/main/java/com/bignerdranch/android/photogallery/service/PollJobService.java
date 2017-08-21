package com.bignerdranch.android.photogallery.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.bignerdranch.android.photogallery.FlickrFetchr;
import com.bignerdranch.android.photogallery.PhotoGalleryActivity;
import com.bignerdranch.android.photogallery.R;
import com.bignerdranch.android.photogallery.model.GalleryItem;
import com.bignerdranch.android.photogallery.model.QueryPreferences;

import java.util.List;

/**
 * Created by michaeltan on 2017/8/20.
 */

public class PollJobService extends JobService {
    public static final String ACTION_SHOW_NOTIFICATION = "com.bignerdranch.android.photogallery.SHOW_NOTIFICATION";
    public static final String PERM_PRIVATE = "com.bignerdranch.android.photogallery.PRIVATE";
    public static final String REQUEST_CODE = "REQUEST_CODE";
    public static final String NOTIFICATION = "NOTIFICATION";
    private static final String TAG = "PollJobService";
    private static final int JOB_ID = 1;
    private static final long POLL_INTERVAL = 60000;
    private PollTask mPollTask;

    public static void setServiceAlarm(Context context, boolean isOn) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        if (isOn) {
            JobInfo jobInfo = new JobInfo.Builder(JOB_ID, new ComponentName(context, PollJobService.class))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                    .setPeriodic(POLL_INTERVAL)
                    .setPersisted(true)
                    .build();

            jobScheduler.schedule(jobInfo);
        } else {
            jobScheduler.cancel(JOB_ID);
        }
        QueryPreferences.setAlarmOn(context, isOn);
    }

    public static boolean isServiceAlarmOn(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        boolean hasBeenScheduled = false;
        for (JobInfo jobInfo : jobScheduler.getAllPendingJobs()) {
            if (jobInfo.getId() == JOB_ID) {
                hasBeenScheduled = true;
            }
        }
        return hasBeenScheduled;
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        mPollTask = new PollTask();
        mPollTask.execute(jobParameters);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        if (mPollTask != null) {
            mPollTask.cancel(true);
        }
        return true;
    }

    private boolean isNetworkAvailableAndConnected() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        boolean isNetworkAvailable = manager.getActiveNetworkInfo() != null;
        boolean isNetworkConnected = isNetworkAvailable && manager.getActiveNetworkInfo().isConnected();
        return isNetworkConnected;
    }

    private class PollTask extends AsyncTask<JobParameters, Void, Void> {

        @Override
        protected Void doInBackground(JobParameters... jobParameterses) {
            JobParameters parameters = jobParameterses[0];

            if (!isNetworkAvailableAndConnected()) {
                jobFinished(parameters, false);
            }
            String query = QueryPreferences.getStoredQuery(PollJobService.this);
            String lastResultId = QueryPreferences.getLastResultId(PollJobService.this);
            List<GalleryItem> items;

            if (query == null) {
                items = new FlickrFetchr().fetchRecentPhotos(1);
            } else {
                items = new FlickrFetchr().searchPhotos(query, 1);
            }

            if (items.size() == 0) {
                jobFinished(parameters, false);
            }

            String resultId = items.get(0).getId();
            if (resultId.equals(lastResultId)) {
                Log.i(TAG, "Got an old result: " + resultId);
            } else {
                Log.i(TAG, "Got an new result: " + resultId);

                Resources resources = getResources();
                Intent i = PhotoGalleryActivity.newIntent(PollJobService.this);
                PendingIntent pendingIntent = PendingIntent.getActivity(PollJobService.this, 0, i, 0);

                Notification notification = new NotificationCompat.Builder(PollJobService.this)
                        .setTicker(resources.getString(R.string.new_pictures_title))
                        .setSmallIcon(android.R.drawable.ic_menu_report_image)
                        .setContentTitle(resources.getString(R.string.new_pictures_title))
                        .setContentText(resources.getString(R.string.new_pictures_text))
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .build();

                NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(PollJobService.this);
                notificationManagerCompat.notify(0, notification);

                sendBroadcast(new Intent(ACTION_SHOW_NOTIFICATION), PERM_PRIVATE);
            }

            QueryPreferences.setLastResultId(PollJobService.this, resultId);

            return null;
        }
    }
}
