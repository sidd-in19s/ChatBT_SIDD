package com.ChatBT_SIDD.PHCET.Service;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.ChatBT_SIDD.PHCET.Activity.MainActivity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BluetoothChatService {

    private static final String TAG = "BluetoothChatService";


    private static final String NAME_SECURE = "BluetoothChatSecure";

    private static final UUID MY_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private int mState;
    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_TRANSFER = 3;

    private static Handler uiHandler;
    private BluetoothAdapter bluetoothAdapter;
    private AcceptThread mAcceptThread;
    private TransferThread mTransferThread;
    private ConnectThread mConnectThread;
    private boolean isTransferError = false;


    private static final String FILE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/ChatBT/";
    private static final int FLAG_MSG = 0;
    private static final int FLAG_FILE = 1;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    public static volatile BluetoothChatService instance = null;

    public static BluetoothChatService getInstance(Handler handler) {
        uiHandler = handler;
        if (instance == null) {
            synchronized (BluetoothChatService.class) {
                if (instance == null) {
                    instance = new BluetoothChatService();
                }
            }
        }
        return instance;
    }

    public BluetoothChatService() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
    }

    public synchronized void start() {
        if (mTransferThread != null) {
            mTransferThread.cancel();
            mTransferThread = null;
        }

        setState(STATE_LISTEN);

        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
    }


    public synchronized void stop() {
        Log.e(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        if (mTransferThread != null) {
            mTransferThread.cancel();
            mTransferThread = null;
        }

        setState(STATE_NONE);
    }

    public void setState(int state) {
        this.mState = state;
    }

    public synchronized void connectDevice(BluetoothDevice device) {
        Log.e(TAG, "connectDevice: ");

        if (mState == STATE_CONNECTING) {
            if (mTransferThread != null) {
                mTransferThread.cancel();
                mTransferThread = null;
            }
        }

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        sendMessageToUi(MainActivity.BLUE_TOOTH_DIALOG, "Working with"+" " + device.getName() +" "+ "to establish connection.");
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    class AcceptThread extends Thread {
        private final BluetoothServerSocket serverSocket;

        public AcceptThread() {

            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, MY_UUID_SECURE);
            } catch (IOException e) {
                e.printStackTrace();
            }
            serverSocket = tmp;
        }

        @Override
        public void run() {
            super.run();

            BluetoothSocket socket = null;
            while (mState != STATE_TRANSFER) {
                try {
                    Log.e(TAG, "run: AcceptThread blocking call，waiting for connection");
                    socket = serverSocket.accept();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "run: ActivityThread fail");
                    break;
                }
                if (socket != null) {
                    synchronized (BluetoothChatService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:

                                Log.e(TAG, "run: Server AcceptThread transport");
                                sendMessageToUi(MainActivity.BLUE_TOOTH_DIALOG, "is working with" + socket.getRemoteDevice().getName() + "connect");
                                dataTransfer(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_TRANSFER:

                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket" + e);
                                }
                                break;
                        }
                    }
                }
            }
        }

        public void cancel() {
            Log.e(TAG, "close: activity Thread");
            try {
                if (serverSocket != null)
                    serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "close: activity Thread fail");
            }
        }
    }

    private void sendMessageToUi(int what, Object s) {
        Message message = uiHandler.obtainMessage();
        message.what = what;
        message.obj = s;
        uiHandler.sendMessage(message);
    }

    private void dataTransfer(BluetoothSocket socket, final BluetoothDevice remoteDevice) {
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        mTransferThread = new TransferThread(socket);
        mTransferThread.start();

        setState(STATE_TRANSFER);
        uiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isTransferError) {
                    sendMessageToUi(MainActivity.BLUE_TOOTH_SUCCESS, remoteDevice);
                }
            }
        }, 300);
    }


    public void sendData(String msg) {
        TransferThread r;
        synchronized (this) {
            if (mState != STATE_TRANSFER) return;
            r = mTransferThread;
        }
        r.write(msg);
    }


    public void sendFile(String filePath) {
        TransferThread r;
        synchronized (this) {
            if (mState != STATE_TRANSFER) return;
            r = mTransferThread;
        }
        r.writeFile(filePath);
    }



    class TransferThread extends Thread {
        private final BluetoothSocket socket;
        private final OutputStream out;
        private final DataOutputStream OutData;
        private final InputStream in;
        private final DataInputStream inData;


        public TransferThread(BluetoothSocket mBluetoothSocket) {
            socket = mBluetoothSocket;
            OutputStream mOutputStream = null;
            InputStream mInputStream = null;
            try {
                if (socket != null) {

                    mOutputStream = socket.getOutputStream();
                    mInputStream = socket.getInputStream();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            out = mOutputStream;
            OutData = new DataOutputStream(out);
            in = mInputStream;
            inData = new DataInputStream(in);
            isTransferError = false;
        }

        @Override
        public void run() {
            super.run();

            while (true) {
                try {
                    switch (inData.readInt()) {
                        case FLAG_MSG:
                            String msg = inData.readUTF();
                            sendMessageToUi(MainActivity.BLUE_TOOTH_READ, msg);
                            break;
                        case FLAG_FILE:
                            File destDir = new File(FILE_PATH);
                            if (!destDir.exists())
                                destDir.mkdirs();
                            String fileName = inData.readUTF();
                            long fileLen = inData.readLong();
                            sendMessageToUi(MainActivity.BLUE_TOOTH_READ_FILE_NOW, "receiving file(" + fileName + ")");

                            long len = 0;
                            int r;
                            byte[] b = new byte[4 * 1024];
                            FileOutputStream out = new FileOutputStream(FILE_PATH + fileName);
                            while ((r = in.read(b)) != -1) {
                                out.write(b, 0, r);
                                len += r;
                                if (len >= fileLen)
                                    break;
                            }
                            sendMessageToUi(MainActivity.BLUE_TOOTH_READ_FILE, FILE_PATH + fileName);
                            break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "run: Transform error" + e.toString());
                    BluetoothChatService.this.start();

                    sendMessageToUi(MainActivity.BLUE_TOOTH_TOAST, "Device connection failed/transfer off");
                    isTransferError = true;
                    break;
                }
            }
        }

        public void write(final String msg) {
            executorService.execute(new Runnable() {
                @SuppressLint("LongLogTag")
                public void run() {
                    try {
                        OutData.writeInt(FLAG_MSG);
                        OutData.writeUTF(msg);
                    } catch (Throwable e) {
                        Log.i("ChatBT_SIDD bluetooth message transmission", "Failed to send");
                    }
                    sendMessageToUi(MainActivity.BLUE_TOOTH_WRAITE, msg);
                }
            });
        }

        public void writeFile(final String filePath) {
            executorService.execute(new Runnable() {
                public void run() {
                    try {
                        sendMessageToUi(MainActivity.BLUE_TOOTH_WRITE_FILE_NOW, "sending file(" + filePath + ")");
                        FileInputStream in = new FileInputStream(filePath);
                        File file = new File(filePath);
                        OutData.writeInt(FLAG_FILE);
                        OutData.writeUTF(file.getName());
                        OutData.writeLong(file.length());
                        int r;
                        byte[] b = new byte[4 * 1024];
                        while ((r = in.read(b)) != -1) {
                            OutData.write(b, 0, r);
                        }
                        sendMessageToUi(MainActivity.BLUE_TOOTH_WRITE_FILE, filePath);
                    } catch (Throwable e) {
                        sendMessageToUi(MainActivity.BLUE_TOOTH_WRITE_FILE_NOW, "File sending failed");
                    }
                }
            });
        }


        public void cancel() {
            try {
                if (socket != null)
                    socket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed" + e);
            }
        }
    }

    class ConnectThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        public ConnectThread(BluetoothDevice device) {
            this.device = device;
            BluetoothSocket mSocket = null;
            try {

                mSocket = device.createRfcommSocketToServiceRecord(
                        MY_UUID_SECURE);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "ConnectThread: fail");
                sendMessageToUi(MainActivity.BLUE_TOOTH_TOAST, "Connection failed，please reconnect");
            }
            socket = mSocket;
        }

        @Override
        public void run() {
            super.run();
            bluetoothAdapter.cancelDiscovery();

            try {
                Log.e(TAG, "run: connectThread wait");
                socket.connect();
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                    Log.e(TAG, "run: unable to close");
                }

                sendMessageToUi(MainActivity.BLUE_TOOTH_TOAST, "Connection failed，please reconnect");
                BluetoothChatService.this.start();
            }

            synchronized (BluetoothChatService.this) {
                mConnectThread = null;
            }

            Log.e(TAG, "run: connectThread connected,ready to transfer");
            dataTransfer(socket, device);
        }

        public void cancel() {
            try {
                if (socket != null)
                    socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
