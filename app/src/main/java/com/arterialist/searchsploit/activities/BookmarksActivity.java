package com.arterialist.searchsploit.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.arterialist.searchsploit.R;
import com.arterialist.searchsploit.adapters.ExploitAdapter;
import com.arterialist.searchsploit.models.Exploit;
import com.arterialist.searchsploit.utils.HawkUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Scanner;

import butterknife.Bind;
import ir.sohreco.androidfilechooser.ExternalStorageNotAvailableException;
import ir.sohreco.androidfilechooser.FileChooserDialog;

public class BookmarksActivity extends BaseActivity implements ExploitAdapter.OnItemClickListener, Toolbar.OnMenuItemClickListener, FileChooserDialog.ChooserListener {

    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.list)
    RecyclerView recyclerView;

    public static Intent createIntent(Context context) {
        return new Intent(context, BookmarksActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmarks);

        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.inflateMenu(R.menu.menu_bookmarks);
        setSupportActionBar(toolbar);
        toolbar.setOnMenuItemClickListener(this);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_bookmarks, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshBookmarks();
    }

    public void refreshBookmarks() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        //noinspection unchecked
        ExploitAdapter adapter = new ExploitAdapter((ArrayList<Exploit>) HawkUtil.get(HawkUtil.BOOKMARKED_EXPLOITS_KEY));
        adapter.setOnItemClickListener(this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onItemClick(int index, Exploit item) {
        startActivity(ExploitActivity.createIntent(this, item));
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.export_bookmarks:
                Gson gson = new GsonBuilder().create();
                String exported = gson.toJson(HawkUtil.get(HawkUtil.BOOKMARKED_EXPLOITS_KEY));
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_kk-mm", Locale.getDefault());
                String fileName = String.format("bookmarks_%s.sbe", dateFormat.format(Calendar.getInstance().getTime()));
                String path = String.format("/sdcard/%s/Searchsploit/%s", Environment.DIRECTORY_DOCUMENTS, fileName);
                try (PrintWriter printWriter = new PrintWriter(path, "UTF-8")) {
                    printWriter.write(exported);
                    printWriter.close();
                } catch (FileNotFoundException | UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                Toast.makeText(this, String.format("Bookmarks exported to %s", path), Toast.LENGTH_LONG).show();
                break;

            case R.id.import_bookmarks:
                FileChooserDialog.Builder builder = new FileChooserDialog.Builder(FileChooserDialog.ChooserType.FILE_CHOOSER, this)
                        .setFileFormats(new String[]{".sbe"})
                        .setInitialDirectory(new File("/sdcard/Documents/"));
                try {
                    builder.build().show(getSupportFragmentManager(), null);
                } catch (ExternalStorageNotAvailableException e) {
                    Toast.makeText(this, "Storage not available!", Toast.LENGTH_LONG).show();
                }
                break;
        }
        return false;
    }

    @Override
    public void onSelect(String path) {
        try {
            String imported = new Scanner(new File(path)).useDelimiter("\\A").next();
            Gson gson = new GsonBuilder().create();
            HawkUtil.set(HawkUtil.BOOKMARKED_EXPLOITS_KEY, gson.fromJson(imported, new TypeToken<ArrayList<Exploit>>() {
            }.getType()));
            refreshBookmarks();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to import bookmarks", Toast.LENGTH_SHORT).show();
        }
    }
}