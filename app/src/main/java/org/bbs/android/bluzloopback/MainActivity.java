package org.bbs.android.bluzloopback;

import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final  java.lang.String TAG = MainActivity.class.getSimpleName();



    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private RecyclerView mBondedDevice;
    private RecyclerView mUnbondedDevice;
    private BroadcastReceiver mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBondedDevice = (RecyclerView)findViewById(R.id.bonded_devices);
        mBondedDevice.setLayoutManager(new LinearLayoutManager(this));
        mBondedDevice.setAdapter(new BondedAdapter(new ArrayList<BluetoothDevice>()));
        mUnbondedDevice = (RecyclerView)findViewById(R.id.unbonded_devices);
        mUnbondedDevice.setLayoutManager(new LinearLayoutManager(this));
        mUnbondedDevice.setAdapter(new BondedAdapter(new ArrayList<BluetoothDevice>()));

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        registerBR();
    }

    void registerBR(){
        // Create a BroadcastReceiver for ACTION_FOUND
        mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d(TAG, "onReceive. action: " + action);
                // When discovery finds a device
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    // Add the name and address to an array adapter to show in a ListView
                    ((BondedAdapter)mUnbondedDevice.getAdapter()).add(device);
                }
            }
        };
// Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
    }

    void unRegisterBR(){
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            finish();
        }

        checkBt();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        checkBt();
    }

    private void checkBt() {
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            addBoundeDevice();
            discoverDevice();
        }
    }

    private void discoverDevice() {
        boolean started = mBluetoothAdapter.startDiscovery();
    }

    private void addBoundeDevice() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        Log.d(TAG, "found " + pairedDevices.size() + " bounded devices.");
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices

            BondedAdapter adapter= new BondedAdapter(new ArrayList<BluetoothDevice>());
            for (BluetoothDevice d : pairedDevices){
                adapter.add(d);
            }
            mBondedDevice.setAdapter(adapter);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unRegisterBR();
    }

    class BondedAdapter extends RecyclerView.Adapter {

        private final List<BluetoothDevice> mDevices;

        public BondedAdapter(List<BluetoothDevice> devices){
            mDevices = devices;

        }

        public void add(BluetoothDevice device){
            mDevices.add(device);
            notifyDataSetChanged();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView t =  new Button(getApplicationContext());
            t.setTextColor(Color.BLACK);
            return new VH(t);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
            BluetoothDevice device = mDevices.get(position);
            ((TextView)holder.itemView).setText(device.getName() + "\n" + device.getAddress());
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent conversation = new Intent(MainActivity.this, ConversationActivity.class);
                    conversation.putExtra(ConversationActivity.ConversationFrag.KEP_MAC, mDevices.get(position).getAddress().toUpperCase());
                    startActivity(conversation);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mDevices.size();
        }

        class VH extends RecyclerView.ViewHolder {
            public VH(View itemView) {
                super(itemView);
            }
        }
    }

}
