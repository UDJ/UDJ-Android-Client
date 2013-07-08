/**
 * Copyright 2011 Kurtis L. Nusbaum
 *
 * This file is part of UDJ.
 *
 * UDJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * UDJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with UDJ.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.klnusbaum.udj;

import org.klnusbaum.udj.auth.UDJAccount;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.Intent;
import android.util.Log;

public class Utils{
  private static final String TAG = "Utils";

  public static boolean isNetworkAvailable(Context context){
    ConnectivityManager cm = ((ConnectivityManager)context.getSystemService(
      Context.CONNECTIVITY_SERVICE));
    NetworkInfo info = cm.getActiveNetworkInfo();
    if(info != null && info.isConnected()){
      return true;
    }
    return false;
  }

  public static int getPlayerState(Context context, UDJAccount account){
    try{
      return Integer.valueOf(account.getUserData(context, Constants.PLAYER_STATE_DATA));
    }
    catch(NumberFormatException e){
      account.setUserData(context, Constants.PLAYER_STATE_DATA, String.valueOf(Constants.NOT_IN_PLAYER));
      return Constants.NOT_IN_PLAYER;
    }
  }

  public static void handleInactivePlayer(Context context, UDJAccount account){
    Log.d(TAG, "Handling player inactive exception");
    account.setUserData(context, Constants.PLAYER_STATE_DATA,
      String.valueOf(Constants.PLAYER_ENDED));
    Intent playerInactiveBroadcast = new Intent(Constants.PLAYER_INACTIVE_ACTION);
    context.sendBroadcast(playerInactiveBroadcast);
  }

  public static void handleNoLongerInPlayer(Context context, UDJAccount account){
    Log.d(TAG, "Handling no longer in player exception");
    account.setUserData(context, Constants.PLAYER_STATE_DATA,
      String.valueOf(Constants.NO_LONGER_IN_PLAYER));
    Intent noLongerInPlayerBroadcast = new Intent(Constants.NO_LONGER_IN_PLAYER_ACTION);
    context.sendBroadcast(noLongerInPlayerBroadcast);
  }

  public static void handleKickedFromPlayer(Context context, UDJAccount account){
    Log.d(TAG, "Handling no longer in player exception");
    account.setUserData(context, Constants.PLAYER_STATE_DATA,
      String.valueOf(Constants.KICKED_FROM_PLAYER));
    Intent kickedFromPlayerBroadcast = new Intent(Constants.KICKED_FROM_PLAYER_ACTION);
    context.sendBroadcast(kickedFromPlayerBroadcast);
  }

  public static void leavePlayer(Context context, UDJAccount account){
    account.setUserData(context, Constants.LAST_PLAYER_ID_DATA,
      String.valueOf(Constants.NO_PLAYER_ID));
    account.setUserData(context, Constants.PLAYER_STATE_DATA,
      String.valueOf(Constants.NOT_IN_PLAYER));
  }


  public static boolean isCurrentPlayerOwner(Context context, UDJAccount account){
    long userId = Long.valueOf(account.getUserData(context, Constants.USER_ID_DATA));
    long ownerId = Long.valueOf(account.getUserData(context, Constants.PLAYER_HOST_ID_DATA));
    return userId==ownerId;
  }

  public static int getPlaybackState(Context context, UDJAccount account){
    String playbackState = account.getUserData(context, Constants.PLAYBACK_STATE_DATA);
    if(playbackState == null){
      return Constants.PLAYING_STATE;
    }
    else{
      return Integer.valueOf(playbackState);
    }
  }

  public static int getPlayerVolume(Context context, UDJAccount account){
    String volume = account.getUserData(context, Constants.PLAYER_VOLUME_DATA);
    if(volume == null){
      return 0;
    }
    else{
      return Integer.valueOf(volume);
    }
  }

}
