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
package org.klnusbaum.udj.network;


import android.content.ContentResolver;
import android.util.Log;
import android.app.IntentService;
import android.content.Intent;

import java.io.IOException;

import org.json.JSONException;

import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;

import org.klnusbaum.udj.Constants;
import org.klnusbaum.udj.Utils;
import org.klnusbaum.udj.exceptions.PlayerInactiveException;
import org.klnusbaum.udj.exceptions.PlayerPasswordException;
import org.klnusbaum.udj.exceptions.PlayerFullException;
import org.klnusbaum.udj.exceptions.BannedException;


/**
 * Adapter used to sync up with the UDJ server.
 */
public class PlayerCommService extends IntentService{

  public enum PlayerJoinError{
    NO_ERROR,
    AUTHENTICATION_ERROR,
    SERVER_ERROR,
    PLAYER_INACTIVE_ERROR,
    NO_NETWORK_ERROR,
    PLAYER_PASSWORD_ERROR,
    PLAYER_FULL_ERROR,
    BANNED_ERROR,
    UNKNOWN_ERROR
  }

  private static final String TAG = "PlayerCommService";

  public PlayerCommService(){
    super("PlayerCommService");
  }

  @Override
  public void onHandleIntent(Intent intent){
    Log.d(TAG, "In Player Comm Service");
    final UDJAccount account = 
      (UDJAccount)intent.getParcelableExtra(Constants.ACCOUNT_EXTRA);
    if(intent.getAction().equals(Intent.ACTION_INSERT)){
      joinPlayer(intent, account);
    }
    else{
      Log.d(TAG, "Unrecognized action of, it was " + 
        intent.getAction());
    } 
  }


  private void joinPlayer(Intent intent, UDJAccount account){
    if(!Utils.isNetworkAvailable(this)){
      doLoginFail(account, PlayerJoinError.NO_NETWORK_ERROR);
      return;
    }

    String userId = account.getUserData(context, Constants.USER_ID_DATA);
    String playerId = intent.getStringExtra(Constants.PLAYER_ID_EXTRA);
    String ownerId = intent.getStringExtra(Constants.PLAYER_OWNER_ID_EXTRA);
    if(userId.equals(ownerId)){
      setLoggedInToPlayer(intent, account, playerId);
      return;
    }

    String authToken;
    String password = "";
    boolean hasPassword = false;
    //TODO hanle error if account isn't provided
    //TODO handle if player id isn't provided
    ticket_hash = account.getTicketHash();
    if(intent.hasExtra(Constants.PLAYER_PASSWORD_EXTRA)){
      Log.d(TAG, "password given for player");
      hasPassword = true;
      password = intent.getStringExtra(Constants.PLAYER_PASSWORD_EXTRA);
    }
    else{
      Log.d(TAG, "No password given for player");
    }

    try{
      if(!hasPassword){
        ServerConnection.joinPlayer(playerId, ticketHash);
      }
      else{
        ServerConnection.joinPlayer(playerId, password, ticketHash);
      }
      setLoggedInToPlayer(intent, account, playerId);
    }
    catch(IOException e){
      Log.e(TAG, "IO exception when joining player");
      Log.e(TAG, e.getMessage());
      doLoginFail(account, PlayerJoinError.SERVER_ERROR);
    }
    catch(JSONException e){
      Log.e(TAG, 
          "JSON exception when joining player");
      Log.e(TAG, e.getMessage());
      doLoginFail(account, PlayerJoinError.SERVER_ERROR);
    }
    catch(AuthenticationException e){
      Log.e(TAG, "Bad password when joining player");
      doLoginFail(account, PlayerJoinError.AUTHENTICATION_ERROR);
    }
    catch(PlayerInactiveException e){
      Log.e(TAG, "Player inactive Exception when joining player");
      doLoginFail(account, PlayerJoinError.PLAYER_INACTIVE_ERROR);
    } catch (ParseException e) {
      e.printStackTrace();
      doLoginFail(account, PlayerJoinError.SERVER_ERROR);
    } catch (PlayerPasswordException e) {
      Log.e(TAG, "Player Password Exception");
      e.printStackTrace();
      doLoginFail(account, PlayerJoinError.PLAYER_PASSWORD_ERROR);
    }
    catch(PlayerFullException e){
      Log.e(TAG, "Player Password Exception");
      e.printStackTrace();
      doLoginFail(account, PlayerJoinError.PLAYER_FULL_ERROR);
    }
    catch(BannedException e){
      Log.e(TAG, "Player Password Exception");
      e.printStackTrace();
      doLoginFail(account, PlayerJoinError.BANNED_ERROR);
    }
  }

  private void setLoggedInToPlayer(Intent joinPlayerIntent, UDJAccount account, String playerId){
    setPlayerData(joinPlayerIntent, account);
    account.setUserData(
        context, Constants.LAST_PLAYER_ID_DATA, playerId);
    account.setUserData(
        context, 
        Constants.PLAYER_STATE_DATA, 
        String.valueOf(Constants.IN_PLAYER));
    Log.d(TAG, "Sending joined player broadcast");
    Intent playerJoinedBroadcast = new Intent(Constants.JOINED_PLAYER_ACTION);
    sendBroadcast(playerJoinedBroadcast);
  }

  private void doLoginFail(
    UDJAccount account,
    PlayerJoinError error)
  {
    account.setUserData(
      context,
      Constants.PLAYER_STATE_DATA,
      String.valueOf(Constants.PLAYER_JOIN_FAILED));
    account.setUserData(
      context,
      Constants.PLAYER_JOIN_ERROR,
      error.toString());
    Intent playerJoinFailedIntent =
      new Intent(Constants.PLAYER_JOIN_FAILED_ACTION);
    Log.d(TAG, "Sending player join failure broadcast");
    sendBroadcast(playerJoinFailedIntent);
  }

  private void setPlayerData(Intent intent, UDJAccount account){
    account.setUserData(
      context,
      Constants.PLAYER_NaccountE_DATA,
      intent.getStringExtra(Constants.PLAYER_NaccountE_EXTRA));
    account.setUserData(
      context,
      Constants.PLAYER_HOSTNaccountE_DATA,
      intent.getStringExtra(Constants.PLAYER_OWNER_EXTRA));
    account.setUserData(
      context,
      Constants.PLAYER_HOST_ID_DATA,
      intent.getStringExtra(Constants.PLAYER_OWNER_ID_EXTRA));
    account.setUserData(
      context,
      Constants.PLAYER_LAT_DATA,
      String.valueOf(intent.getDoubleExtra(Constants.PLAYER_LAT_EXTRA, -100.0))
    );
    account.setUserData(
      context,
      Constants.PLAYER_LONG_DATA,
      String.valueOf(intent.getDoubleExtra(Constants.PLAYER_LONG_EXTRA, -100.0))
    );
  }
}
