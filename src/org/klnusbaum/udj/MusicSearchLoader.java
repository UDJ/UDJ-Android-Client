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

import android.content.Context;
import android.util.Log;

import java.util.List;
import java.io.IOException;

import org.json.JSONException;

import org.apache.http.auth.AuthenticationException;
import org.apache.http.ParseException;

import org.klnusbaum.udj.auth.UDJAccount;
import org.klnusbaum.udj.containers.LibraryEntry;
import org.klnusbaum.udj.exceptions.NoLongerInPlayerException;
import org.klnusbaum.udj.exceptions.PlayerInactiveException;
import org.klnusbaum.udj.exceptions.KickedException;

public abstract class MusicSearchLoader 
  extends AsyncTaskLoader<MusicSearchLoader.MusicSearchResult>
{

  public enum MusicSearchError{
    NO_ERROR,
    PLAYER_INACTIVE_ERROR,
    NO_SEARCH_ERROR,
    SERVER_ERROR,
    AUTHENTICATION_ERROR,
    NO_LONGER_IN_PLAYER_ERROR,
    KICKED_ERROR
  };
  private static final String TAG = "MusicSearchLoader";

  public static class MusicSearchResult{
    private List<LibraryEntry> res;
    private MusicSearchError error;

    public MusicSearchResult(List<LibraryEntry> res){
      this.res = res;
      this.error = MusicSearchError.NO_ERROR;
    }

    public MusicSearchResult(List<LibraryEntry> res, MusicSearchError error){
      this.res = res;
      this.error = error;
    }

    public List<LibraryEntry> getResults(){
      return res;
    }

    public MusicSearchError getError(){
      return error;
    }
  }

  private UDJAccount account;

  public MusicSearchLoader(Context context, UDJAccount account){
    super(context);
    this.account = account;
  }

  public MusicSearchResult loadInBackground(){
    return attemptSearch(true);
  }

  private MusicSearchResult attemptSearch(boolean attemptReauth){
    String ticketHash = account.getTicketHash();
    String playerId = account.getUserData(getContext(), Constants.LAST_PLAYER_ID_DATA);

    try{
      return doSearch(playerId, ticketHash);
    }
    catch(JSONException e){
      return new MusicSearchResult(null, 
        MusicSearchError.SERVER_ERROR);
    }
    catch(ParseException e){
      return new MusicSearchResult(null, MusicSearchError.SERVER_ERROR);
    }
    catch(IOException e){
      return new MusicSearchResult(null, MusicSearchError.SERVER_ERROR);
    }
    catch(AuthenticationException e){
      Log.d(TAG, "hard auth failure");
      return new MusicSearchResult(null, MusicSearchError.AUTHENTICATION_ERROR);
    }
    catch(PlayerInactiveException e){
      return new MusicSearchResult(null, MusicSearchError.PLAYER_INACTIVE_ERROR);
    }
    catch (NoLongerInPlayerException e) {
      return new MusicSearchResult(null, MusicSearchError.NO_LONGER_IN_PLAYER_ERROR);
    }
    catch (KickedException e){
      return new MusicSearchResult(null, MusicSearchError.KICKED_ERROR);
    }
  }

  @Override
  protected void onStartLoading(){
    forceLoad();
  }

  protected abstract MusicSearchResult doSearch(String playerId, String authToken) throws
    JSONException, ParseException, IOException, AuthenticationException,
    PlayerInactiveException, NoLongerInPlayerException, KickedException;
}
