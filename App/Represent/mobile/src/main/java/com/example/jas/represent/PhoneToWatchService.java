package com.example.jas.represent;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;


public class PhoneToWatchService extends Service {
    private GoogleApiClient mApiClient;

    @Override
    public void onCreate() {
        super.onCreate();
        //initialize the googleAPIClient for message passing
        mApiClient = new GoogleApiClient.Builder( this )
                .addApi( Wearable.API )
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                    }
                })
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mApiClient.disconnect();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Which cat do we want to feed? Grab this info from INTENT
        // which was passed over when we called startService
        if (intent != null) {
            Bundle extras = intent.getExtras();
            final int numPols = extras.getInt("ZIP");

            String county = extras.getString("COUNTY");
            String state = extras.getString("STATE");
            double obamaNum = extras.getDouble("OBAMA");
            double romneyNum = extras.getDouble("ROMNEY");


//        Service _this = this;

            String msg = county + ":" + state + ":" + obamaNum + ":" + romneyNum + ":" + numPols + ":";
            //need to pass getName, getPosition, getParty
            for (int i = 0; i < numPols; i++) {
                Politician pol = CongressionalActivity.politicians.get(i);
                msg += pol.getName()+ ":" + pol.getPosition() + ":" + pol.getParty()+":";
            }
            Log.d("Phone2Watch", "Msg = "+msg);

            final String message = msg;

            // Send the message with the cat name
            new Thread(new Runnable() {
                @Override
                public void run() {
                    //first, connect to the apiclient
                    mApiClient.connect();
                    //now that you're connected, send a massage with the cat name
                    Log.d("phone2watch", "Sending int " + numPols);
//                    sendMessage("/zip", message);
                    sendMessage("/zip", message);
                }
            }).start();
//        _this.stopSelf();
            return START_STICKY;
        } else {
            Log.d("Phone2Watch", "this shouldnt be happening");
            return 0;
        }
    }

    @Override //remember, all services need to implement an IBiner
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void sendMessage( final String path, final String text ) {
        //one way to send message: start a new thread and call .await()
        //see watchtophoneservice for another way to send a message
        new Thread( new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes( mApiClient ).await();
                for(Node node : nodes.getNodes()) {
                    //we find 'nodes', which are nearby bluetooth devices (aka emulators)
                    //send a message for each of these nodes (just one, for an emulator)
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mApiClient, node.getId(), path, text.getBytes() ).await();
                    //4 arguments: api client, the node ID, the path (for the listener to parse),
                    //and the message itself (you need to convert it to bytes.)
                }
            }
        }).start();
    }

}
