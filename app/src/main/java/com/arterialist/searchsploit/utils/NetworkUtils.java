package com.arterialist.searchsploit.utils;

import android.app.DownloadManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;

import com.arterialist.searchsploit.R;

import static android.content.Context.DOWNLOAD_SERVICE;

public class NetworkUtils {

    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public static long downloadFile(Context context, DownloadOptions downloadOptions) {
        if (downloadOptions.isCreated()) {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadOptions.getUrl()));
            request.setVisibleInDownloadsUi(true);
            if (!TextUtils.isEmpty(downloadOptions.getTitle())) {
                request.setTitle(downloadOptions.getTitle());
            }
            if (!TextUtils.isEmpty(downloadOptions.getDescription())) {
                request.setDescription(downloadOptions.getDescription());
            }
            request.setNotificationVisibility(downloadOptions.getNotificationVisibility());
            request.setDestinationInExternalPublicDir(downloadOptions.getPathToFile(), downloadOptions.getFileName());
            if (downloadOptions.isAllowScanningByMediaScanner()) {
                request.allowScanningByMediaScanner();
            }

            DownloadManager downloadManager = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
            return downloadManager.enqueue(request);
        } else {
            return -1;
        }
    }

    public static class DownloadOptions {

        private String url;
        private String pathToFile;
        private String fileName;
        private String title;
        private String description;
        private int notificationVisibility;
        private boolean allowScanningByMediaScanner;

        private boolean created;

        public DownloadOptions() {
        }

        public DownloadOptions setUrl(String url) {
            this.url = url;
            return this;
        }

        public DownloadOptions setTitle(String title) {
            this.title = title;
            return this;
        }

        public DownloadOptions setDescription(String description) {
            this.description = description;
            return this;
        }

        public DownloadOptions setNotificationVisibility(int notificationVisibility) {
            this.notificationVisibility = notificationVisibility;
            return this;
        }

        public DownloadOptions setPathToFile(String pathToFile) {
            this.pathToFile = pathToFile;
            return this;
        }

        public DownloadOptions setFileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public DownloadOptions setAllowScanningByMediaScanner(boolean allowScanningByMediaScanner) {
            this.allowScanningByMediaScanner = allowScanningByMediaScanner;
            return this;
        }

        String getUrl() {
            return url;
        }

        String getPathToFile() {
            return pathToFile;
        }

        String getFileName() {
            return fileName;
        }

        String getTitle() {
            return title;
        }

        String getDescription() {
            return description;
        }

        int getNotificationVisibility() {
            return notificationVisibility;
        }

        boolean isAllowScanningByMediaScanner() {
            return allowScanningByMediaScanner;
        }

        boolean isCreated() {
            return created;
        }

        public void create() {
            created = !TextUtils.isEmpty(url) & !TextUtils.isEmpty(pathToFile) & !TextUtils.isEmpty(fileName);
        }

        public static DownloadOptions databaseOptions(Context context) {
            NetworkUtils.DownloadOptions downloadOptions = new NetworkUtils.DownloadOptions();
            downloadOptions
                    .setUrl(context.getString(R.string.text_url_database))
                    .setTitle(context.getString(R.string.text_normal_downloading_db))
                    .setDescription(context.getString(R.string.text_long_downloading_db_description))
                    .setAllowScanningByMediaScanner(true)
                    .setPathToFile(String.format("%s/Searchsploit", Environment.DIRECTORY_DOCUMENTS))
                    .setFileName("database.txt")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .create();
            return downloadOptions;
        }
    }
}
