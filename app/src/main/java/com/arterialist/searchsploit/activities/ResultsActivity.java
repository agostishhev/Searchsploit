package com.arterialist.searchsploit.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.arterialist.searchsploit.R;
import com.arterialist.searchsploit.adapters.ExploitAdapter;
import com.arterialist.searchsploit.common.IntentStrings;
import com.arterialist.searchsploit.models.Exploit;

import java.util.ArrayList;
import java.util.Collections;

import butterknife.BindView;

public class ResultsActivity extends BaseActivity implements ExploitAdapter.OnItemClickListener, Toolbar.OnMenuItemClickListener, TabLayout.OnTabSelectedListener {

    public static final String EXTRA_EXPLOITS = "EXTRA_EXPLOITS";
    public static final String EXTRA_TABS = "EXTRA_TABS";
    public static final String EXTRA_SLICES_COUNT = "EXTRA_SLICES_COUNT";

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.tabLayout)
    TabLayout tabLayout;
    @BindView(R.id.list)
    RecyclerView recyclerView;

    private ArrayList<Exploit> exploits;
    private BroadcastReceiver receiver;

    public static Intent createIntent(Context context, ArrayList<Exploit> exploits, boolean needsTabs, int slicesCount) {
        Intent intent = new Intent(context, ResultsActivity.class);
        intent.putExtra(EXTRA_EXPLOITS, exploits);
        intent.putExtra(EXTRA_TABS, needsTabs);
        intent.putExtra(EXTRA_SLICES_COUNT, slicesCount);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        Intent intent = getIntent();
        exploits = intent.getParcelableArrayListExtra(EXTRA_EXPLOITS);

        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.inflateMenu(R.menu.menu_results);
        setSupportActionBar(toolbar);
        toolbar.setOnMenuItemClickListener(this);

        boolean needsTabs = intent.getBooleanExtra(EXTRA_TABS, false);
        if (needsTabs) {
            int slicesCount = intent.getIntExtra(EXTRA_SLICES_COUNT, 0);
            tabLayout.setVisibility(View.VISIBLE);
            for (int index = 0; index < slicesCount; index++) {
                tabLayout.addTab(tabLayout.newTab().setText(String.valueOf(index + 1)));
            }
            tabLayout.addOnTabSelectedListener(this);

            receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (IntentStrings.ACTION_NEW_RESULTS.equals(action)) {
                        exploits = intent.getParcelableArrayListExtra(IntentStrings.EXTRA_SLICE);
                        refreshList();
                    }
                }
            };

            registerReceiver(receiver, new IntentFilter(IntentStrings.ACTION_NEW_RESULTS));
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        refreshList();
    }

    private void refreshList() {
        ExploitAdapter adapter = new ExploitAdapter(new ArrayList<Exploit>());
        adapter.setOnItemClickListener(this);
        recyclerView.setAdapter(adapter);
        adapter.setItems(exploits);
        getSupportActionBar().setTitle(String.format(getString(R.string.text_normal_results_title), exploits.size()));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_results, menu);
        for (int index = 0; index < toolbar.getChildCount(); index++) {
            View child = toolbar.getChildAt(index);
            if (child instanceof ActionMenuView) {
                ((ActionMenuView) child).setOverflowIcon(getDrawable(R.drawable.ic_sort));
            }
        }
        return true;
    }

    @Override
    public void onItemClick(int index, Exploit item) {
        startActivity(ExploitActivity.createIntent(this, item));
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.asc_desc:
                item.setChecked(!item.isChecked());
                break;

            case R.id.sort_by_title:
                sortExploits(Exploit.COMPARING_PARAMETER_TITLE);
                break;

            case R.id.sort_by_date:
                sortExploits(Exploit.COMPARING_PARAMETER_DATE);
                break;

            case R.id.sort_by_platform:
                sortExploits(Exploit.COMPARING_PARAMETER_PLATFORM);
                break;
        }
        return false;
    }

    private void sortExploits(int parameter) {
        for (Exploit exploit : exploits) {
            exploit.setComparingParameter(parameter);
        }
        Collections.sort(exploits);
        if (toolbar.getMenu().getItem(0).isChecked()) {
            Collections.reverse(exploits);
        }
        refreshList();
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        Intent intent = new Intent(IntentStrings.ACTION_NEW_RESULTS_REQUEST);
        intent.putExtra(IntentStrings.EXTRA_RESULTS_SLICE_INDEX, tab.getPosition());
        sendBroadcast(intent);
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
        //pass
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
        //pass
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
    }
}