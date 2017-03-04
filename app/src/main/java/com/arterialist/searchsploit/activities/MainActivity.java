package com.arterialist.searchsploit.activities;

import android.animation.ValueAnimator;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.arterialist.searchsploit.R;
import com.arterialist.searchsploit.common.IntentStrings;
import com.arterialist.searchsploit.models.Exploit;
import com.arterialist.searchsploit.utils.HawkUtil;
import com.arterialist.searchsploit.utils.NetworkUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import butterknife.Bind;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import timber.log.Timber;

public class MainActivity extends BaseActivity implements RadioGroup.OnCheckedChangeListener, Toolbar.OnMenuItemClickListener {

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.query_field)
    EditText queryET;
    @Bind(R.id.options_text)
    TextView optionsTV;
    @Bind(R.id.no_database_button)
    Button noDatabaseB;
    @Bind(R.id.search)
    Button searchB;
    @Bind(R.id.search_types)
    RadioGroup searchTypesRG;
    @Bind(R.id.case_sensitive)
    CheckBox caseSensitiveCB;
    @Bind(R.id.each_word_search)
    CheckBox eachWordSearchCB;
    @Bind(R.id.spinner)
    Spinner spinner;
    @Bind(R.id.progress)
    ProgressBar progressBar;

    private int checkedId = R.id.search_type_word;

    private BroadcastReceiver sliceRequestReceiver;
    private ArrayList<Exploit> results;
    private BroadcastReceiver downloadFinishedReceiver;
    private long downloadId;

    public static Intent createIntent(Context context) {
        return new Intent(context, MainActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.inflateMenu(R.menu.menu_main);
        setSupportActionBar(toolbar);
        toolbar.setOnMenuItemClickListener(this);

        Boolean configured = HawkUtil.get(HawkUtil.DATABASE_CONFIGURED_KEY);
        if (!configured) {
            noDatabaseB.setVisibility(View.VISIBLE);
            searchB.setEnabled(false);
        } else {
            sliceRequestReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    int sliceIndex = intent.getIntExtra(IntentStrings.EXTRA_RESULTS_SLICE_INDEX, 0);
                    ArrayList<Exploit> slice = new ArrayList<>(results.subList(500 * sliceIndex, 500 * (sliceIndex + 1) > results.size() ? results.size() : 500 * (sliceIndex + 1)));
                    Intent resultsIntent = new Intent(IntentStrings.ACTION_NEW_RESULTS);
                    resultsIntent.putParcelableArrayListExtra(IntentStrings.EXTRA_SLICE, slice);
                    sendBroadcast(resultsIntent);
                }
            };

            downloadFinishedReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(downloadId);
                    Cursor c = ((DownloadManager) getSystemService(DOWNLOAD_SERVICE)).query(query);
                    int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    if (c.moveToFirst()) {
                        if (c.getInt(columnIndex) == DownloadManager.STATUS_SUCCESSFUL) {
                            searchB.setEnabled(true);
                            Toast.makeText(context, "Done!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            };

            registerReceiver(sliceRequestReceiver, new IntentFilter(IntentStrings.ACTION_NEW_RESULTS_REQUEST));
            registerReceiver(downloadFinishedReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

            checkForDatabaseUpdates();
        }

        searchTypesRG.setOnCheckedChangeListener(this);
    }

    private void checkForDatabaseUpdates() {
        if (NetworkUtils.isOnline(this)) {
            new AsyncTask<Object, Object, String>() {
                @Override
                protected String doInBackground(Object... params) {
                    try {
                        Document document = Jsoup.connect(getString(R.string.text_url_repo))
                                .userAgent("Searchsploit")
                                .get();
                        Elements messages = document.body().select(".message");
                        return messages.get(1).text();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return "not available";
                }

                @Override
                protected void onPostExecute(String s) {
                    super.onPostExecute(s);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    if (s.contains("DB:")) {
                        try {
                            final Date date = dateFormat.parse(s.split(" ")[1]);
                            if (((Long) HawkUtil.get(HawkUtil.LAST_DATABASE_UPDATE_KEY)) < date.getTime()) {
                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("Database update")
                                        .setMessage("A newer version of database found, do You want to download it now?")
                                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                if (NetworkUtils.isOnline(MainActivity.this)) {
                                                    downloadId = NetworkUtils.downloadFile(MainActivity.this, NetworkUtils.DownloadOptions.databaseOptions(MainActivity.this));
                                                    HawkUtil.set(HawkUtil.LAST_DATABASE_UPDATE_KEY, date.getTime());
                                                } else {
                                                    showNoInternetDialog();
                                                }
                                            }
                                        }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                })
                                        .setCancelable(false)
                                        .show();
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }.execute();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        queryET.setFilters(new InputFilter[]{new InputFilter.LengthFilter(40)});
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        int menuRes = NetworkUtils.isOnline(this) & (Boolean) HawkUtil.get(HawkUtil.DATABASE_CONFIGURED_KEY) ? R.menu.menu_main : R.menu.menu_main_no_update;
        getMenuInflater().inflate(menuRes, menu);
        return true;
    }

    @OnCheckedChanged(R.id.dark_theme)
    public void onDarkThemeCheckedChanged(boolean checked) {
        if (checked) {
            animateBackground(Color.WHITE, ContextCompat.getColor(this, R.color.blueGrey900));
            setUIColors(Color.WHITE);
        } else {
            animateBackground(ContextCompat.getColor(this, R.color.blueGrey900), Color.WHITE);
            setUIColors(ContextCompat.getColor(this, R.color.blueGrey900));
        }
    }

    @OnClick(R.id.no_database_button)
    public void onConfigureClick(View view) {
        startActivity(ConfigureDatabaseActivity.createIntent(this));
        finish();
    }

    @OnClick(R.id.search)
    public void onSearchClick(View view) {
        if (validateOptionsAndInput()) {
            animateSearchButton(true);
            final String query = queryET.getVisibility() == View.VISIBLE ? queryET.getText().toString().split("\n")[0] : ((TextView) spinner.getChildAt(0)).getText().toString();
            final boolean eachWordSearch = eachWordSearchCB.isChecked();
            new AsyncTask<Void, Void, ArrayList<Exploit>>() {
                @Override
                protected ArrayList<Exploit> doInBackground(Void... params) {
                    try {
                        if (!eachWordSearch) {
                            return getResultsForQuery(query);
                        } else {
                            String[] words = query.split(" ");
                            ArrayList<String> trueWords = new ArrayList<>();
                            for (String word : words) {
                                if (!TextUtils.isEmpty(word)) {
                                    trueWords.add(word);
                                }
                            }

                            ArrayList<ArrayList<Exploit>> resultsForEachWord = new ArrayList<>();

                            for (String word : trueWords) {
                                resultsForEachWord.add(getResultsForQuery(word));
                            }

                            return joinResults(resultsForEachWord);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        return new ArrayList<>();
                    }
                }

                @Override
                protected void onPostExecute(ArrayList<Exploit> exploits) {
                    super.onPostExecute(exploits);
                    Timber.d(String.valueOf(exploits.size()));
                    if (!exploits.isEmpty()) {
                        if (exploits.size() == 1) {
                            startActivity(ExploitActivity.createIntent(MainActivity.this, exploits.get(0)));
                        } else {
                            results = exploits;

                            startActivity(ResultsActivity.createIntent(
                                    MainActivity.this,
                                    new ArrayList<>(exploits.subList(0, exploits.size() > 500 ? 500 : exploits.size())),
                                    exploits.size() > 500,
                                    exploits.size() / 500 + 1));
                        }
                    } else {
                        new AlertDialog.Builder(MainActivity.this)
                                .setMessage("No results!")
                                .setCancelable(false)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                }).show();
                    }
                    animateSearchButton(false);
                }
            }.execute();
        }
    }

    private ArrayList<Exploit> joinResults(ArrayList<ArrayList<Exploit>> results) {
        ArrayList<Exploit> joined = new ArrayList<>();
        for (ArrayList<Exploit> result : results) {
            joined.removeAll(result);
            joined.addAll(result);
        }
        return joined;
    }

    private ArrayList<Exploit> getResultsForQuery(String query) throws IOException {
        String command = buildCommand(query);
        Process sh = Runtime.getRuntime().exec(new String[]{"/system/bin/sh", "-c", command});
        InputStream inputStream = sh.getInputStream();
        String output = streamToString(inputStream, true);
        return parseOutput(output, query);
    }

    private ArrayList<Exploit> parseOutput(String output, String query) {
        if (output.isEmpty()) {
            return new ArrayList<>();
        } else {
            List<String> results = Arrays.asList(output.split(System.lineSeparator()));
            ArrayList<Exploit> exploits = convertOutputToExploits(results);
            ArrayList<Exploit> trash = getInvalidResults(exploits, query);
            exploits.removeAll(trash);

            trash.clear();

            System.gc();

            return exploits;
        }
    }

    private ArrayList<Exploit> convertOutputToExploits(List<String> results) {
        ArrayList<Exploit> exploits = new ArrayList<>();
        for (String result : results) {
            String[] items = result.split(",");

            Exploit exploit = new Exploit(
                    Integer.parseInt(items[0]),
                    items[1],
                    items[2].replace("\"", ""),
                    items[3],
                    items[4].replace("\"", ""),
                    items[5],
                    items[6],
                    Integer.parseInt(items[7]));

            exploits.add(exploit);
        }

        return exploits;
    }

    private ArrayList<Exploit> getInvalidResults(ArrayList<Exploit> exploits, String query) {
        ArrayList<Exploit> trash = new ArrayList<>();
        for (Exploit exploit : exploits) {
            switch (checkedId) {
                case R.id.search_type_id:
                    if (Integer.parseInt(query) != exploit.getId()) {
                        trash.add(exploit);
                    }
                    break;

                case R.id.search_type_word:
                    String lowTitle = exploit.getTitle();
                    String lowQuery = query;
                    if (!caseSensitiveCB.isChecked()) {
                        lowTitle = exploit.getTitle().toLowerCase();
                        lowQuery = query.toLowerCase();
                    }
                    if (!lowTitle.contains(lowQuery)) {
                        trash.add(exploit);
                    }
                    break;

                case R.id.search_type_platform:
                    if (!query.equals(exploit.getPlatform())) {
                        trash.add(exploit);
                    }
                    break;

                case R.id.search_type_type:
                    if (!query.equals(exploit.getType())) {
                        trash.add(exploit);
                    }
                    break;
            }
        }
        return trash;
    }

    private String streamToString(InputStream inputStream, boolean fast) {
        String output = "";
        if (fast) {
            try {
                output = new Scanner(inputStream).useDelimiter("\\A").next();
            } catch (Exception e) {
                //empty inputStream
                return output;
            }
        } else {
            //if bugs will happen
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line;
            try {
                while ((line = bufferedReader.readLine()) != null) {
                    output += line + "\n";
                }
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return output;
    }

    private void setUIColors(int color) {
        queryET.setTextColor(color);
        queryET.setHintTextColor(color);
        optionsTV.setTextColor(color);
        caseSensitiveCB.setTextColor(color);
        eachWordSearchCB.setTextColor(color);
        TextView textView = (TextView) spinner.getChildAt(0);
        if (textView != null) {
            textView.setTextColor(color);
        }
        for (int i = 0; i < searchTypesRG.getChildCount(); i++) {
            ((RadioButton) searchTypesRG.getChildAt(i)).setTextColor(color);
        }
    }

    private void animateBackground(int from, int to) {
        ValueAnimator animator = ValueAnimator.ofArgb(from, to);
        animator.setDuration(600);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int color = (int) animation.getAnimatedValue();
                ((ViewGroup) queryET.getParentForAccessibility()).setBackgroundColor(color);
            }
        });
        animator.start();
    }

    private void animateSearchButton(final boolean hide) {
        ValueAnimator buttonAnimator1;
        int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics());
        if (hide) {
            buttonAnimator1 = ValueAnimator.ofInt(searchB.getLayoutParams().height, 10);
        } else {
            buttonAnimator1 = ValueAnimator.ofInt(10, height);
            searchB.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.INVISIBLE);
        }
        buttonAnimator1.setDuration(700);
        buttonAnimator1.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                ViewGroup.LayoutParams layoutParams = searchB.getLayoutParams();
                int animatedValue = (int) animation.getAnimatedValue();
                layoutParams.height = animatedValue;
                searchB.setLayoutParams(layoutParams);
                if (animatedValue == progressBar.getLayoutParams().height) {
                    if (hide) {
                        searchB.setVisibility(View.INVISIBLE);
                        progressBar.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        ValueAnimator buttonAnimator2;
        if (hide) {
            buttonAnimator2 = ValueAnimator.ofInt(searchB.getLayoutParams().width, progressBar.getLayoutParams().width);
        } else {
            buttonAnimator2 = ValueAnimator.ofInt(progressBar.getLayoutParams().width, height * 2);
        }
        buttonAnimator2.setDuration(700);
        buttonAnimator2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                ViewGroup.LayoutParams layoutParams = searchB.getLayoutParams();
                layoutParams.width = (int) animation.getAnimatedValue();
                searchB.setLayoutParams(layoutParams);
            }
        });

        buttonAnimator1.start();
        buttonAnimator2.start();
    }

    private String buildCommand(String query) {
        String command = "cat " + HawkUtil.get(HawkUtil.DATABASE_PATH_KEY) + " | grep ";
        if (!caseSensitiveCB.isChecked()) {
            command += "-i '";
        } else {
            command += "'";
        }

        command += query;
        if (searchTypesRG.getCheckedRadioButtonId() == R.id.search_type_id) {
            command += ",";
        }
        command += "'";
        return command;
    }

    private boolean validateOptionsAndInput() {
        if (queryET.getVisibility() == View.VISIBLE) {
            if (TextUtils.isEmpty(queryET.getText().toString())) {
                queryET.setError(getString(R.string.text_error_empty_field));
                return false;
            } else {
                queryET.setError(null);
            }
        }

        return handleCheckedRadioButton(false);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        this.checkedId = checkedId;
        handleCheckedRadioButton(true);
    }

    private boolean handleCheckedRadioButton(boolean state) {
        String query;

        switch (checkedId) {
            case R.id.search_type_id:
                if (state) {
                    spinner.setVisibility(View.GONE);
                    queryET.setVisibility(View.VISIBLE);
                    caseSensitiveCB.setEnabled(false);
                    caseSensitiveCB.setChecked(false);
                    eachWordSearchCB.setEnabled(false);
                    eachWordSearchCB.setChecked(false);
                    queryET.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
                    queryET.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5)});
                }
                query = queryET.getText().toString();
                if (query.isEmpty() || !isInt(query)) {
                    queryET.setError(getString(R.string.text_error_not_int));
                    return false;
                }
                break;

            case R.id.search_type_word:
                if (state) {
                    spinner.setVisibility(View.GONE);
                    queryET.setVisibility(View.VISIBLE);
                    caseSensitiveCB.setEnabled(true);
                    eachWordSearchCB.setEnabled(true);
                    queryET.setInputType(EditorInfo.TYPE_CLASS_TEXT);
                    queryET.setFilters(new InputFilter[]{new InputFilter.LengthFilter(40)});
                }
                query = queryET.getText().toString();
                if (query.isEmpty()) {
                    queryET.setError(getString(R.string.text_error_empty_field));
                    return false;
                }
                break;

            case R.id.search_type_platform:
                if (state) {
                    spinner.setVisibility(View.VISIBLE);
                    queryET.setVisibility(View.GONE);
                    caseSensitiveCB.setEnabled(false);
                    caseSensitiveCB.setChecked(true);
                    eachWordSearchCB.setEnabled(false);
                    eachWordSearchCB.setChecked(false);
                    spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.platforms)));
                }
                break;

            case R.id.search_type_type:
                if (state) {
                    spinner.setVisibility(View.VISIBLE);
                    queryET.setVisibility(View.GONE);
                    caseSensitiveCB.setEnabled(false);
                    caseSensitiveCB.setChecked(true);
                    eachWordSearchCB.setEnabled(false);
                    eachWordSearchCB.setChecked(false);
                    spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.types)));
                }
                break;
        }
        queryET.setError(null);

        return true;
    }

    private boolean isInt(String query) {
        try {
            //noinspection ResultOfMethodCallIgnored
            Integer.parseInt(query);
            return true;
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sliceRequestReceiver != null) {
            unregisterReceiver(sliceRequestReceiver);
            unregisterReceiver(downloadFinishedReceiver);
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.update_db:
                if (HawkUtil.get(HawkUtil.DATABASE_CONFIGURED_KEY)) {
                    if (NetworkUtils.isOnline(this)) {
                        searchB.setEnabled(false);
                        try {
                            Runtime.getRuntime().exec(new String[]{"/system/bin/sh", "-c", "rm " + HawkUtil.get(HawkUtil.DATABASE_PATH_KEY)});
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        downloadId = NetworkUtils.downloadFile(this, NetworkUtils.DownloadOptions.databaseOptions(this));

                        HawkUtil.set(HawkUtil.LAST_DATABASE_UPDATE_KEY, Calendar.getInstance().getTimeInMillis());
                    } else {
                        showNoInternetDialog();
                    }
                } else {
                    Toast.makeText(this, "Nothing to update!", Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.show_bookmarks:
                startActivity(BookmarksActivity.createIntent(this));
                break;
        }
        return false;
    }

    private void showNoInternetDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.text_normal_no_network)
                .setMessage(R.string.text_normal_connect_to_internet)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }
}