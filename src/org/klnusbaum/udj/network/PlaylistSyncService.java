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


import android.content.Context;
import android.os.RemoteException;
import android.content.OperationApplicationException;
import android.util.Log;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.app.Notification;
import android.app.NotificationManager;
import android.graphics.drawable.BitmapDrawable;


import java.io.IOException;

import org.json.JSONObject;
import org.json.JSONException;

import org.apache.http.auth.AuthenticationException;
import org.apache.http.ParseException;

import org.klnusbaum.udj.Constants;
import org.klnusbaum.udj.R;
import org.klnusbaum.udj.exceptions.NoLongerInPlayerException;
import org.klnusbaum.udj.exceptions.PlayerInactiveException;
import org.klnusbaum.udj.exceptions.KickedException;
import org.klnusbaum.udj.Utils;


/**
 * Adapter used to sync up with the UDJ server.
 */
public class PlaylistSyncService extends IntentService{

  private static final int SONG_ADD_EXCEPTION_ID = 1;
  private static final int SONG_REMOVE_EXCEPTION_ID = 2;
  private static final int SONG_SET_EXCEPTION_ID = 3;
  private static final int PLAYBACK_STATE_SET_EXCEPTION_ID = 4;
  private static final int PLAYER_VOLUME_SET_EXCEPTION_ID = 5;

  private static final String TAG = "PlaylistSyncService";

  public PlaylistSyncService(){
    super("PlaylistSyncService");
  }

  @Override
  public void onHandleIntent(Intent intent){
    final UDJAccount account =
      (UDJAccount)intent.getParcelableExtra(Constants.ACCOUNT_EXTRA);
    final String playerId = account.getUserData(context, Constants.LAST_PLAYER_ID_DATA);
    //TODO handle error if playerId is bad
    if(intent.getAction().equals(Intent.ACTION_INSERT)){
      if(intent.getData().equals(Constants.PLAYLIST_URI)){
        String libId = intent.getStringExtra(Constants.LIB_ID_EXTRA);
        addSongToPlaylist(account, playerId, libId, true, intent);
      }
      else if(intent.getData().equals(Constants.VOTES_URI)){
        //TODO handle if lib id is bad
        String libId = intent.getStringExtra(Constants.LIB_ID_EXTRA);
        //TODO handle if votetype is bad
        int voteWeight = intent.getIntExtra(Constants.VOTE_WEIGHT_EXTRA,0); 
        voteOnSong(account, playerId, libId, voteWeight, true);
      }
    }
    else if(intent.getAction().equals(Intent.ACTION_DELETE)){
      Log.d(TAG, "Handling delete");
      if(intent.getData().equals(Constants.PLAYLIST_URI)){
        Log.d(TAG, "In plalist syncservice, about to insert song into remove requests");
        //TODO handle if Playlist id is bad.
        String libId = intent.getStringExtra(Constants.LIB_ID_EXTRA);
        removeSongFromPlaylist(account, playerId, libId, true, intent);
      }
    }
    else if(intent.getAction().equals(Constants.ACTION_SET_CURRENT_SONG)){
      Log.d(TAG, "Handling setting current song");
      String libId = intent.getStringExtra(Constants.LIB_ID_EXTRA);
      setCurrentSong(account, playerId, libId, true, intent);
    }
    else if(intent.getAction().equals(Constants.ACTION_SET_PLAYBACK)){
      setPlaybackState(intent, account, playerId, true);
    }
    else if(intent.getAction().equals(Constants.ACTION_SET_VOLUME)){
      setPlayerVolume(intent, account, playerId, true);
    }
  }

  private void setCurrentSong(
    Account account,
    String playerId,
    String libId,
    Intent originalIntent)
  {
    String ticketHash = account.getTicketHash();

    try{
      ServerConnection.setCurrentSong(playerId, libId, ticketHash);
      Intent setCurrentComplete = new Intent(Constants.BROADCAST_SET_CURRENT_COMPLETE);
      this.sendBroadcast(setCurrentComplete);
    }
    catch(IOException e){
      alertSetSongException(account, originalIntent);
      Log.e(TAG, "IO exception when setting song");
      Log.e(TAG, e.getMessage());
    }
    catch(AuthenticationException e){
      alertSetSongException(account, originalIntent);
      Log.e(TAG, "Hard Authentication exception when setting song");
    }
    catch(PlayerInactiveException e){
      Log.e(TAG, "Event over exceptoin when setting song");
      Utils.handleInactivePlayer(this, account);
    } catch (NoLongerInPlayerException e) {
      Utils.handleNoLongerInPlayer(this, account);
    }
    catch(KickedException e){
      Utils.handleKickedFromPlayer(this, account);
    }
  }



  private void addSongToPlaylist(
    Account account,
    String playerId,
    String libId,
    boolean attemptReauth,
    Intent originalIntent)
  {
    String ticketHash = account.getTicketHash();

    try{
      ServerConnection.addSongToActivePlaylist(
          playerId, libId, ticketHash);
    }
    catch(JSONException e){
      alertAddSongException(account, originalIntent);
      Log.e(TAG, "JSON exception when adding to playist");
    }
    catch(ParseException e){
      alertAddSongException(account, originalIntent);
      Log.e(TAG, "Parse exception when adding to playist");
    }
    catch(IOException e){
      alertAddSongException(account, originalIntent);
      Log.e(TAG, "IO exception when adding to playist");
      Log.e(TAG, e.getMessage());
    }
    catch(AuthenticationException e){
      alertAddSongException(account, originalIntent);
      Log.e(TAG, "Hard Authentication exception when adding to playist");
    }
    catch(PlayerInactiveException e){
      Log.e(TAG, "Event over exceptoin when retreiving playlist");
      Utils.handleInactivePlayer(this, account);
    }
    catch (NoLongerInPlayerException e) {
      Utils.handleNoLongerInPlayer(this, account);
    }
    catch(KickedException e){
      Utils.handleKickedFromPlayer(this, account);
    }


  }

  private void removeSongFromPlaylist(Account account, String playerId, String libId, Intent originalIntent)
  {
    String ticketHash = account.getTicketHash();

    try{
      Log.d(TAG, "Actually removing song");
      ServerConnection.removeSongFromActivePlaylist(playerId, libId, ticketHash);
      Intent removeSongComplete = new Intent(Constants.BROADCAST_REMOVE_SONG_COMPLETE);
      this.sendBroadcast(removeSongComplete);
    }
    catch(ParseException e){
      alertRemoveSongException(account, originalIntent);
      Log.e(TAG, "Parse exception when removing from playist");
    }
    catch(IOException e){
      alertRemoveSongException(account, originalIntent);
      Log.e(TAG, "IO exception when removing from playist");
      Log.e(TAG, e.getMessage());
    }
    catch(AuthenticationException e){
      alertRemoveSongException(account, originalIntent);
      Log.e(TAG, "Hard Authentication exception when removing from playist");
    }
    catch(PlayerInactiveException e){
      Log.e(TAG, "Event over exceptoin when removing from playlist");
      Utils.handleInactivePlayer(this, account);
    }
    catch(NoLongerInPlayerException e){
      Utils.handleNoLongerInPlayer(this, account);
    }
    catch(KickedException e){
      Utils.handleKickedFromPlayer(this, account);
    }
  }

  private void voteOnSong(Account account, String playerId, String libId, int voteWeight){
    String ticketHash = account.getTicketHash();

    try{
      ServerConnection.voteOnSong(playerId, libId, voteWeight, authToken);
      Intent voteCompleteBroadcast = new Intent(Constants.BROADCAST_VOTE_COMPLETED);
      this.sendBroadcast(voteCompleteBroadcast);
    }
    catch(ParseException e){
      Log.e(TAG, "Parse exception when voting on song");
    }
    catch(IOException e){
      Log.e(TAG, "IO exception when voting on song");
    }
    catch(AuthenticationException e){
      Log.e(TAG, "Hard Authentication exception when voting on song");
    }
    catch(PlayerInactiveException e){
      Log.e(TAG, "Event over exception when retreiving playlist");
      Utils.handleInactivePlayer(this, account);
    }
    catch(NoLongerInPlayerException e){
      Utils.handleNoLongerInPlayer(this, account);
    }
    catch(KickedException e){
      Utils.handleKickedFromPlayer(this, account);
    }
  }

  private void setPlayerVolume(Intent intent, UDJAccount account, String playerId){
    int desiredVolume = intent.getIntExtra(Constants.PLAYER_VOLUME_EXTRA, 0);
    Log.d(TAG, "proceeding to set volume of player to: " + String.valueOf(desiredVolume) + 
        " on server");

    String ticketHash = account.getTicketHash();

    try{
      ServerConnection.setPlayerVolume(playerId, desiredVolume, ticketHash);
    }
    catch(IOException e){
      Log.e(TAG, "IO exception in set volume" );
      alertSetVolumeException(account, intent);
      return;
    }
    catch(AuthenticationException e){
      Log.e(TAG, "Hard Authentication exception when setting volume");
      alertSetVolumeException(account, intent);
      return;
    }
    catch(PlayerInactiveException e){
      Log.e(TAG, "Player inactive exception in set volume" );
      Utils.handleInactivePlayer(this, account);
      return;
    }
    catch(NoLongerInPlayerException e){
      Utils.handleNoLongerInPlayer(this, account);
      return;
    }
    catch(KickedException e){
      Utils.handleKickedFromPlayer(this, account);
    }
  }


  private void setPlaybackState(Intent intent, Account account, String playerId)
  {
    String authToken = account.getTicketHash();

    int desiredPlaybackState = intent.getIntExtra(Constants.PLAYBACK_STATE_EXTRA, 0);
    try{
      ServerConnection.setPlaybackState(playerId, desiredPlaybackState, authToken);
    }
    catch(IOException e){
      Log.e(TAG, "IO exception in set playback" );
      alertSetPlaybackException(account, intent);
      return;
    }
    catch(AuthenticationException e){
      alertSetPlaybackException(account, intent);
      Log.e(TAG, "Hard Authentication exception when setting playback state");
    }
    catch(PlayerInactiveException e){
      Log.e(TAG, "Player inactive exception in set playback" );
      Utils.handleInactivePlayer(this, account);
      return;
    }
    catch(NoLongerInPlayerException e){
      Utils.handleNoLongerInPlayer(this, account);
      return;
    }
    catch(KickedException e){
      Utils.handleKickedFromPlayer(this, account);
    }
  }

  private void alertSetVolumeException(Account account, Intent originalIntent){
    alertException(
      account,
      originalIntent,
      R.string.set_volume_failed_title,
      R.string.set_volume_failed_content,
      PLAYBACK_STATE_SET_EXCEPTION_ID
    );
  }


  private void alertSetPlaybackException(Account account, Intent originalIntent){
    alertException(
      account,
      originalIntent,
      R.string.set_playback_failed_title,
      R.string.set_playback_failed_content,
      PLAYBACK_STATE_SET_EXCEPTION_ID
    );
  }


  private void alertAddSongException(Account account, Intent originalIntent){
    alertException(
      account,
      originalIntent,
      R.string.song_add_failed_title,
      R.string.song_add_failed_content,
      SONG_ADD_EXCEPTION_ID
    );
  }

  private void alertRemoveSongException(Account account, Intent originalIntent){
    alertException(
      account,
      originalIntent,
      R.string.song_remove_failed_title,
      R.string.song_remove_failed_content,
      SONG_REMOVE_EXCEPTION_ID
    );
  }

  private void alertSetSongException(Account account, Intent originalIntent){
    alertException(
      account,
      originalIntent,
      R.string.song_set_failed_title,
      R.string.song_set_failed_content,
      SONG_SET_EXCEPTION_ID);
  }

  private void alertException(Account account, Intent originalIntent,
    int titleRes, int contentRes, int notificationId)
  {

    PendingIntent pe = PendingIntent.getService(
      this, 0, originalIntent, 0);
    Notification notification = 
      new Notification(R.drawable.udjlauncher, "", System.currentTimeMillis());
    notification.setLatestEventInfo(
        this,
        getString(titleRes),
        getString(contentRes),
        pe);
    notification.flags |= Notification.FLAG_AUTO_CANCEL;

    NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
    nm.notify(notificationId, notification);
  }





}
