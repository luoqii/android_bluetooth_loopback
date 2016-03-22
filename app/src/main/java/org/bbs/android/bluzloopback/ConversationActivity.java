package org.bbs.android.bluzloopback;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.support.annotation.Nullable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class ConversationActivity extends AppCompatActivity {
    private static final String TAG = ConversationActivity.class.getSimpleName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        android.app.Fragment frag = new ConversationFrag();
        Bundle args = new Bundle();
        args.putString(ConversationFrag.KEP_MAC, getIntent().getStringExtra(ConversationFrag.KEP_MAC));
        frag.setArguments(args);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, frag)
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_conversation, menu);
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



    public static class ConversationFrag extends android.app.Fragment {
        public static final java.lang.String KEP_MAC = "mac_address";

        //http://stackoverflow.com/questions/23963815/sending-data-from-android-to-arduino-with-hc-06-bluetooth-module
        // Well known SPP UUID
        private static final UUID MY_UUID =
                UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        private BluetoothAdapter mBluetoothAdapter;
        private BluetoothDevice mDevice;
        private RecyclerView mConversion;
        private Adapter mAdapter;
        private BZThread mThread;
        private View mButton;
        private EditText mMessage;
        private View mDebug;
        private TextView mSendM;
        private TextView mRcvdM;

        public ConversationFrag(){
            super();
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            String mac = getArguments().getString(KEP_MAC);
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            mDevice = mBluetoothAdapter.getRemoteDevice(mac);
            mThread = new BZThread(mac);
            mThread.start();
            mAdapter = new Adapter();
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.frag_conversation, null);
            return v;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            mConversion = (RecyclerView)view.findViewById(R.id.conversation);
            mConversion.setLayoutManager(new LinearLayoutManager(view.getContext()));
            mConversion.setAdapter(mAdapter);

            mMessage = (EditText)view.findViewById(R.id.message);
            mButton = view.findViewById(R.id.send);
            mButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendMessage();
                }
            });

            mDebug = view.findViewById(R.id.debug);
            mSendM = (TextView)view.findViewById(R.id.send_message);
            mRcvdM = (TextView)view.findViewById(R.id.rcvd_message);

        }

        private void sendMessage() {
            String message = mMessage.getText().toString();
            if (!TextUtils.isEmpty(message) && mThread.mOut != null){
                try {
                    mThread.mOut.write(message.getBytes());
                    Message m = new Message();
                    m.message = message;
                    m.direction = Message.Direction.DIRECTION_CLIENT_2_SERVER;
                    mAdapter.add(m);

                    mSendM.setText(message);
                    mRcvdM.setText("");
                    Log.d(TAG, "SND: " + message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            mMessage.setText("");
        }

        class Adapter extends RecyclerView.Adapter {
            private List<Message> mMessages;

            public Adapter(){
                mMessages = new ArrayList<>();
            }

            public void add(Message message){
                mMessages.add(message);
                notifyDataSetChanged();
            }

            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                TextView v = new TextView(getContext());
                return new VH(v);
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                TextView t = (TextView) holder.itemView;

                Message m = mMessages.get(position);
                String text = m.direction == Message.Direction.DIRECTION_CLIENT_2_SERVER ? "SND:" : "RCV:";
                text += m.message;
                t.setText(text);
            }

            @Override
            public int getItemCount() {
                return mMessages.size();
            }

            class VH extends RecyclerView.ViewHolder {

                public VH(View itemView) {
                    super(itemView);
                }
            }
        }

        static class Message {
            enum Direction {
                DIRECTION_SERVER_2_CLIENT,
                DIRECTION_CLIENT_2_SERVER
            }

            public Direction direction;
            public String message;
        }

        class BZThread extends ConnectThread {
            private InputStream mIn;
            private OutputStream mOut;

            public BZThread(String mac) {
                super(mac);
            }

            protected void manageConnectedSocket(BluetoothSocket mmSocket) {
                Log.d(TAG, "connect success.");
                mButton.post(new Runnable() {
                    @Override
                    public void run() {
                        mButton.setEnabled(true);
                    }
                });

                try {
                    mIn = mmSocket.getInputStream();
                    mOut = mmSocket.getOutputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                int count = -1;
                byte[] buffer = new byte[1024];
                try {
                    while ((count = mIn.read(buffer)) != -1 ){
                        String message = new String(buffer, 0, count);
                        Log.d(TAG, "RCV: " + message);
                        final Message m = new Message();
                        m.message = message;
                        m.direction = Message.Direction.DIRECTION_SERVER_2_CLIENT;
                        mButton.post(new Runnable() {
                            @Override
                            public void run() {
                                mAdapter.add(m);
                                mConversion.scrollToPosition(mConversion.getChildCount() - 1 );

                                mRcvdM.setText(m.message);
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        //http://developer.android.com/intl/zh-tw/guide/topics/connectivity/bluetooth.html#ConnectingDevices
        public static class ConnectThread extends Thread {
            private final BluetoothSocket mmSocket;
            private final BluetoothDevice mDevice;
            private BluetoothAdapter mBluetoothAdapter;

            public ConnectThread(String mac) {
                // Use a temporary object that is later assigned to mmSocket,
                // because mmSocket is final
                BluetoothSocket tmp = null;
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                mDevice = mBluetoothAdapter.getRemoteDevice(mac.toUpperCase());

                // Get a BluetoothSocket to connect with the given BluetoothDevice
                try {
                    // MY_UUID is the app's UUID string, also used by the server code
                    tmp = mDevice.createRfcommSocketToServiceRecord(MY_UUID);
                } catch (IOException e) { }
                mmSocket = tmp;
            }

            public void run() {
                // Cancel discovery because it will slow down the connection
                mBluetoothAdapter.cancelDiscovery();

                try {
                    // Connect the device through the socket. This will block
                    // until it succeeds or throws an exception
                    mmSocket.connect();
                } catch (IOException connectException) {
                    // Unable to connect; close the socket and get out
                    Log.d(TAG, "EXP", connectException);
                    try {
                        mmSocket.close();
                    } catch (IOException closeException) {
                        Log.d(TAG, "EXP", closeException);
                    }
                    return;
                }

                // Do work to manage the connection (in a separate thread)
                manageConnectedSocket(mmSocket);
            }

            protected void manageConnectedSocket(BluetoothSocket mmSocket) {
                Log.d(TAG, "connect success.");
            }

            /** Will cancel an in-progress connection, and close the socket */
            public void cancel() {
                try {
                    mmSocket.close();
                } catch (IOException e) { }
            }
        }
    }
}
