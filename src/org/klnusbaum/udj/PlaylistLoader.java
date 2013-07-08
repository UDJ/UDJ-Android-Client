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

import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;
import android.content.Context;

import org.klnusbaum.udj.auth.UDJAccount;
import org.klnusbaum.udj.network.ServerConnection;
import org.klnusbaum.udj.network.RESTProcessor;
import org.klnusbaum.udj.containers.ActivePlaylistEntry;
import org.klnusbaum.udj.exceptions.KickedException;
import org.klnusbaum.udj.exceptions.NoLongerInPlayerException;
import org.klnusbaum.udj.exceptions.PlayerInactiveException;

import org.json.JSONObject;
import org.json.JSONException;

import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;

import java.io.IOException;
import java.util.List;


public class PlaylistLoader extends AsyncTaskLoader<PlaylistLoader.PlaylistResult>{
  private static final String TAG = "PlaylistLoader";
  public enum PlaylistLoadError{
    NO_ERROR,
    PLAYER_INACTIVE_ERROR,
    SERVER_ERROR,
    AUTHENTICATION_ERROR,
    NO_LONGER_IN_PLAYER_ERROR,
    KICKED_ERROR
  }

  public static class PlaylistResult{
    public List<ActivePlaylistEntry> playlistEntries;
    public PlaylistLoadError error;

    public PlaylistResult(List<ActivePlaylistEntry> playlistEntries){
      this.playlistEntries = playlistEntries;
      this.error = PlaylistLoadError.NO_ERROR;
    }

    public PlaylistResult(List<ActivePlaylistEntry> playlistEntries, PlaylistLoadError error){
      this.playlistEntries = playlistEntries;
      this.error = error;
    }
  }

  private UDJAccount account;
  private Context context;

  public PlaylistLoader(Context context, UDJAccount account){
    super(context);
    this.account = account;
    this.context = context;
  }

  public PlaylistResult loadInBackground(){
    String ticketHash = account.getTicketHash();
    String playerId = account.getUserData(context, Constants.LAST_PLAYER_ID_DATA);

    try{
      JSONObject serverResult = ServerConnection.getActivePlaylist(playerId, ticketHash);
      List<ActivePlaylistEntry> toReturn = RESTProcessor.processActivePlaylist(serverResult, account, context);
      return new PlaylistResult(toReturn);
    }
    catch(JSONException e){
      return new PlaylistResult(null, PlaylistLoadError.SERVER_ERROR);
    }
    catch(ParseException e){
      return new PlaylistResult(null, PlaylistLoadError.SERVER_ERROR);
    }
    catch(IOException e){
      return new PlaylistResult(null, PlaylistLoadError.SERVER_ERROR);
    }
    catch(AuthenticationException e){
      Log.d(TAG, "hard auth failure");
      return new PlaylistResult(null, PlaylistLoadError.AUTHENTICATION_ERROR);
    }
    catch(PlayerInactiveException e){
      return new PlaylistResult(null, PlaylistLoadError.PLAYER_INACTIVE_ERROR);
    }
    catch (NoLongerInPlayerException e) {
      return new PlaylistResult(null, PlaylistLoadError.NO_LONGER_IN_PLAYER_ERROR);
    }
    catch (KickedException e){
      return new PlaylistResult(null, PlaylistLoadError.KICKED_ERROR);
    }
  }

  @Override
  protected void onStartLoading(){
    forceLoad();
  }

}
