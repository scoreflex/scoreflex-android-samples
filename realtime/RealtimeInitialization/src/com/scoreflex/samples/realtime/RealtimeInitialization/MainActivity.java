/*
 * Licensed to Scoreflex (www.scoreflex.com) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Scoreflex licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.scoreflex.samples.realtime.RealtimeInitialization;

import java.util.Date;
import java.text.SimpleDateFormat;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.widget.TextView;
import android.widget.Button;
import android.content.BroadcastReceiver;
import android.support.v4.content.LocalBroadcastManager;

import com.scoreflex.Scoreflex;
import com.scoreflex.realtime.Session;
import com.scoreflex.realtime.SessionInitializedListener;
import com.scoreflex.model.JSONParcelable;


public class MainActivity extends Activity {
  /**********************************************************************/
  /*********************** Scoreflex identifiers ************************/
  /**********************************************************************/
  // FILL THESE VARIABLES
  private final String  APP_CLIENT_ID     = "...";
  private final String  APP_CLIENT_SECRET = "...";
  private final boolean APP_USE_SANDBOX   = true;

  /**********************************************************************/
  /************************ BroadcastReceivers **************************/
  /**********************************************************************/
  // Create a receiver to be notified when the Scoreflex SDK will be initialized
  private BroadcastReceiver onInitializedReceiver = new BroadcastReceiver() {
    public void onReceive(Context context, Intent intent) {
      if (intent.getAction().equals(Scoreflex.INTENT_SCOREFLEX_INTIALIZED)) {
        onScoreflexSDKInitialized();
      }
      else {
        JSONParcelable reason = intent.getParcelableExtra(
          Scoreflex.INTENT_SCOREFLEX_INTIALIZE_FAILED_EXTRA_REASON
          );
        onScoreflexSDKInitializationFailed(reason);
      }
    }
  };

  // Create a receiver to monitor the application connectivity
  private BroadcastReceiver onConnectivityReceiver = new BroadcastReceiver() {
    public void onReceive(Context context, Intent intent) {
      boolean state = intent.getBooleanExtra(
        Scoreflex.INTENT_CONNECTIVITY_EXTRA_CONNECTIVITY, false
        );
      if (state)
        logInfo("Application connected");
      else
        logInfo("Application disconnected");
    }
  };

  // Create a receiver to be notified of the player changes
  private BroadcastReceiver onUserLoggedInReceiver = new BroadcastReceiver() {
    public void onReceive(Context context, Intent intent) {
      logInfo("User "+Scoreflex.getPlayerId()+" logged in");
      if (Session.isInitialized()) {
        // The realtime session was already initialized, so this event was
        // launched because the current player has changed. In this situation,
        // we destroy the current session and start a new one.
        logInfo("Current player has changed. Renew the realtime session");
        stopRealtimeSession();
        startRealtimeSession();
      }
    }
  };

  /**********************************************************************/
  /************************* Activity definition ************************/
  /**********************************************************************/
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    findViewById(R.id.restart_button).setOnClickListener(
      new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          stopRealtimeSession();
          if (!Scoreflex.isInitialized()) {
            initializeSDK();
          }
          else {
            startRealtimeSession();
          }
        }
      });
    findViewById(R.id.stop_button).setOnClickListener(
      new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          stopRealtimeSession();
        }
      });
    findViewById(R.id.profile_button).setOnClickListener(
      new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          Scoreflex.showPlayerProfile(MainActivity.this, null, null);
        }
      });
    findViewById(R.id.clear_button).setOnClickListener(
      new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          resetInfo();
        }
      });
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
  }

  @Override
  protected void onPause() {
    super.onPause();
    LocalBroadcastManager.getInstance(this).unregisterReceiver(onInitializedReceiver);
    LocalBroadcastManager.getInstance(this).unregisterReceiver(onConnectivityReceiver);
    LocalBroadcastManager.getInstance(this).unregisterReceiver(onUserLoggedInReceiver);
    Scoreflex.unregisterNetworkReceiver(this);
  }

  @Override
  protected void onResume() {
    super.onResume();

    // Register the previous receiver on the Intents:
    //   - Scoreflex.INTENT_SCOREFLEX_INTIALIZED
    //   - Scoreflex.INTENT_SCOREFLEX_INTIALIZE_FAILED
    LocalBroadcastManager.getInstance(this).registerReceiver(
      onInitializedReceiver,
      new IntentFilter(Scoreflex.INTENT_SCOREFLEX_INTIALIZED)
    );
    LocalBroadcastManager.getInstance(this).registerReceiver(
      onInitializedReceiver,
      new IntentFilter(Scoreflex.INTENT_SCOREFLEX_INTIALIZE_FAILED)
    );

    // Register the previous receiver on the Intents:
    //   - Scoreflex.INTENT_CONNECTIVITY_CHANGED
    LocalBroadcastManager.getInstance(this).registerReceiver(
      onConnectivityReceiver,
      new IntentFilter(Scoreflex.INTENT_CONNECTIVITY_CHANGED)
    );

    // Register the previous receiver on the Intents:
    //   - Scoreflex.INTENT_USER_LOGED_IN
    LocalBroadcastManager.getInstance(this).registerReceiver(
      onUserLoggedInReceiver,
      new IntentFilter(Scoreflex.INTENT_USER_LOGED_IN)
    );

    // Initialize the Scoreflex SDK
    if (!Scoreflex.isInitialized()) {
      initializeSDK();
    }
    else if (!Session.isInitialized()) {
      logInfo("Scoreflex SDK already initialized");
      startRealtimeSession();
    }
    else {
      logInfo("Scoreflex SDK already initialized");
      logInfo("Realtime Session already initialized");
    }
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
  }


  /**********************************************************************/
  /************************ Init/Deinit functions ***********************/
  /**********************************************************************/

  // Function to initialize the scoreflex SDK
  private void initializeSDK() {
    logInfo("Scoreflex SDK initializing...");
    ((Button)findViewById(R.id.restart_button)).setEnabled(false);
    ((Button)findViewById(R.id.stop_button)).setEnabled(false);
    ((Button)findViewById(R.id.profile_button)).setEnabled(false);

    Scoreflex.initialize(this,
                         APP_CLIENT_ID,
                         APP_CLIENT_SECRET,
                         APP_USE_SANDBOX);
    Scoreflex.registerNetworkReceiver(this);
  }

  // Function to initialize the realtime session
  private void startRealtimeSession() {
      logInfo("Realtime session initializing...");
      Session.initialize(new SessionInitializedListener() {
        public void onSuccess() {
          ((Button)findViewById(R.id.restart_button)).setEnabled(true);
          ((Button)findViewById(R.id.stop_button)).setEnabled(true);
          onRealtimeSessionInitialized();
        }
        public void onFailure(int status_code) {
          ((Button)findViewById(R.id.restart_button)).setEnabled(true);
          ((Button)findViewById(R.id.stop_button)).setEnabled(true);
          onRealtimeSessionError(status_code);
        }
      });
  }

  // Function to deinitialize the realtime session, if needed
  private void stopRealtimeSession() {
    ((Button)findViewById(R.id.stop_button)).setEnabled(false);
    if (Session.isInitialized()) {
      logInfo("Realtime session deinitializing...");
      Session.deinitialize();
      logInfo("Realtime session deinitialized");
    }
  }

  /**********************************************************************/
  /************************* Callback functions *************************/
  /**********************************************************************/
  // Callback function called when the SDK is initialized
  private void onScoreflexSDKInitialized() {
    logInfo("Scoreflex SDK initialized");
    ((Button)findViewById(R.id.profile_button)).setEnabled(true);
    startRealtimeSession();
  }

  // Callback function called when the SDK initialization failed
  private void onScoreflexSDKInitializationFailed(JSONParcelable reason) {
    ((Button)findViewById(R.id.restart_button)).setEnabled(true);
    ((Button)findViewById(R.id.profile_button)).setEnabled(false);

    if (reason != null)
      logInfo("Scorefles SDK initialization failed\n"
              + " (reason: "+reason.getJSONObject()+")");
    else
      logInfo("Scorefles SDK initialization failed\n"
              +" (reason: Network error)");
  }

  // Callback function called when the realtime session is initializated
  private void onRealtimeSessionInitialized() {
    logInfo("Realtime session initialized\n"
            + "  * Server address:   " + Session.getServerAddr() + "\n"
            + "  * Server port:      " + Session.getServerPort() + "\n"
            + "  * Player connected: " + Session.isConnected()   + "\n"
            + "  * Session options:\n"
            + "      - Auto reconnection flag: "
            + Session.getReconnectFlag() + "\n"
            + "      - Max retries:            "
            + Session.getMaxRetries() + "\n"
            + "      - Reconnection timeout:   "
            + Session.getReconnectTimeout() + " msecs\n"
            + "      - TCP heartbeat timeout:  "
            + Session.getTcpHeartbeatTimeout() + " msecs\n"
            + "      - UDP heartbeat timeout:  "
            + Session.getUdpHeartbeatTimeout() + " msecs");

    // ################################################################
    // THIS SAMPLE ENDS HERE. CHECKOUT 'RealtimeConnectionManagement'
    // SAMPLE FOR THE NEXT STEPS.
    // ################################################################
  }


  // Callback function called when the realtime session initialization failed
  private void onRealtimeSessionError(int error) {
    switch (error) {
      case Session.STATUS_NETWORK_ERROR:
        logInfo("Realtime session initialization failed\n"
                + "  (reason: Network error)");
        break;
      case Session.STATUS_PERMISSION_DENIED:
        logInfo("Realtime session initialization failed\n"
                + "  (reason: Permission denied)");
        break;
      case Session.STATUS_INTERNAL_ERROR:
        logInfo("Realtime session initialization failed\n"
                + "  (reason: Internal error)");
        break;
      default:
        logInfo("Realtime session initialization failed\n"
                + "  (reason: Unexpected error)");
        break;
    }
  }

  /**********************************************************************/
  /************************** Helper functions **************************/
  /**********************************************************************/
  // Helper function that returns the current time. Used to log messages
  private String getTime() {
    Date now = new Date();
    SimpleDateFormat ft =  new SimpleDateFormat("hh:mm:ss.SSS");
    return ft.format(now);
  }

  // Helper function that clears the log TextView
  private void resetInfo() {
    TextView info = (TextView)findViewById(R.id.scoreflex_info);
    info.setText("");
  }

  // Helper function that appends a log message in the info box, prefixed by the
  // current time
  private void logInfo(String text) {
    TextView info = (TextView)findViewById(R.id.scoreflex_info);
    info.append("[" + getTime() + "] " + text + "\n");
  }
}
