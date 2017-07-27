package com.ardic.android.iotignite.greenhouse.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.ardic.android.iotignite.greenhouse.Constants;
import com.ardic.android.iotignite.greenhouse.CustomCardViewClickListener;
import com.ardic.android.iotignite.greenhouse.GatewayViewModel;
import com.ardic.android.iotignite.greenhouse.R;
import com.ardic.android.iotignite.greenhouse.RecyclerGatewayAdapter;
import com.ardic.android.iotignite.greenhouse.controllers.DROMController;
import com.ardic.android.iotignite.greenhouse.controllers.DeviceController;
import com.ardic.android.iotignite.lib.restclient.model.Device;
import com.ardic.android.iotignite.lib.restclient.model.DeviceContent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class GatewayDashboardActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener, CustomCardViewClickListener, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = GatewayDashboardActivity.class.getSimpleName();
    private FloatingActionButton fabAddGateway;
    private Toolbar toolbar;

    private RecyclerView recyclerView;
    private List<GatewayViewModel> gatewayList;
    private DrawerLayout drawer;
    private ActionBarDrawerToggle toggle;
    private NavigationView navigationView;
    private LinearLayoutManager layoutManager;
    private RecyclerGatewayAdapter recyclerGatewayAdapter;
    private SwipeRefreshLayout gatewaySwipeRefreshLayout;

    private String activeUser;
    private String activeUserPassword;
    private DROMController mDromController;
    private DeviceController mDeviceController;
    private Device devices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gateway_dashboard);

        getLoginInfo();

        initUI();

        // get previous configured gateways..
        updateDashboard();

    }

    private void initUI() {

        toolbar = (Toolbar) findViewById(R.id.app_bar_gateway_dashboard_toolbar);
        setSupportActionBar(toolbar);

        fabAddGateway = (FloatingActionButton) findViewById(R.id.fab);
        fabAddGateway.setOnClickListener(this);

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        drawer = (DrawerLayout) findViewById(R.id.activity_gateway_dashboard_drawer_layout);

        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.scrollToPosition(0);

        recyclerView = (RecyclerView) findViewById(R.id.content_gateway_dashboard_recycler_view);
        recyclerView.setLayoutManager(layoutManager);

        gatewayList = new ArrayList<>();
        recyclerGatewayAdapter = new RecyclerGatewayAdapter(gatewayList, this);

        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(recyclerGatewayAdapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        gatewaySwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.app_bar_gateway_swipe_refresh_layout);
        gatewaySwipeRefreshLayout.setOnRefreshListener(this);
    }

    @Override
    public void onBackPressed() {
        drawer = (DrawerLayout) findViewById(R.id.activity_gateway_dashboard_drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        drawer = (DrawerLayout) findViewById(R.id.activity_gateway_dashboard_drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onClick(View view) {

        /**
         * Start QR Scan Activity here.
         */
        if (view.equals(fabAddGateway)) {

            Snackbar.make(view, "Scan your gateway", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();

            //TODO : Check camera permission here. - RunTime and Manifest.
            Intent intent = new Intent(GatewayDashboardActivity.this, QRScanActivity.class);
            startActivityForResult(intent, Constants.READ_QR_CODE);

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK) {

            String qr = data.getStringExtra(Constants.Extra.EXTRA_GW_QR_CODE);
            Log.i(TAG, "QR Code Received !" + qr);
            Toast.makeText(this, "QR Code Received !" + qr, Toast.LENGTH_LONG).show();

            mDromController = new DROMController(this, activeUser, activeUserPassword, qr);

            mDromController.execute();

            //TODO: LOADING....
            try {
                if (mDromController.get()) {

                    GatewayDashboardActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //TODO End of LOADING
                            Toast.makeText(GatewayDashboardActivity.this, "AWESOME ! DEVICE LICENCED SUCCESSFULLY. ", Toast.LENGTH_LONG).show();
                            // refreshContent();
                        }
                    });
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException on onActivityResult function:" + e);
            } catch (ExecutionException e) {
                Log.e(TAG, "ExecutionException on onActivityResult function:" + e);
            }
        }
    }

    private void getLoginInfo() {

        Intent mIntent = getIntent();

        if (mIntent != null && mIntent.hasExtra(Constants.Extra.EXTRA_USERNAME)) {
            activeUser = mIntent.getStringExtra(Constants.Extra.EXTRA_USERNAME);
        }

        if (mIntent != null && mIntent.hasExtra(Constants.Extra.EXTRA_PASSWORD)) {
            activeUserPassword = mIntent.getStringExtra(Constants.Extra.EXTRA_PASSWORD);
        }
    }

    private void getDeviceInfo() {
        mDeviceController = new DeviceController(this, activeUser, activeUserPassword);
        mDeviceController.execute();

        // TODO : LOADING - Getting devices...
        try {
            devices = mDeviceController.get();

            for (DeviceContent cnt : devices.getDeviceContents()) {
                Log.i(TAG, "DEVICES: " + cnt.getDeviceId());
                GatewayViewModel model = new GatewayViewModel(cnt.getLabeL(), cnt.getDeviceId(), true);
                if (!gatewayList.contains(model)) {
                    gatewayList.add(model);
                }
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "getDeviceInfo(): " + e);
        } catch (ExecutionException e) {
            Log.e(TAG, "getDeviceInfo(): " + e);
        }
        // TODO : LOADING - Getting device end...
    }

    @Override
    public void onItemClick(View v, int position) {
        Log.i(TAG, "Position on recycler view:" + position);
        GatewayViewModel gateway = gatewayList.get(position);
        Toast.makeText(getApplicationContext(), " Position: " + position + " Gateway ID: " + gateway.getGatewayId(), Toast.LENGTH_SHORT).show();
        // startActivity(new Intent(GatewayDashboardActivity.this, SensorDashboardActivity.class));
    }

    /**
     * Called when a swipe gesture triggers a refresh.
     */
    @Override
    public void onRefresh() {
        updateDashboard();
    }


    private void updateDashboard() {


        getDeviceInfo();

        recyclerGatewayAdapter.notifyDataSetChanged();
        gatewaySwipeRefreshLayout.setRefreshing(false);
    }

}

