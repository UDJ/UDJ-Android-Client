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

package org.klnusbaum.udj.auth;

import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;
import android.content.Context;

import org.klnusbaum.udj.Utils;
import org.klnusbaum.udj.auth.UDJAccount;
import org.klnusbaum.udj.network.ServerConnection;
import org.klnusbaum.udj.network.RESTProcessor;
import org.klnusbaum.udj.containers.ActivePlaylistEntry;
import org.klnusbaum.udj.exceptions.KickedException;
import org.klnusbaum.udj.exceptions.NoLongerInPlayerException;
import org.klnusbaum.udj.exceptions.PlayerInactiveException;
import org.klnusbaum.udj.exceptions.APIVersionException;

import org.json.JSONObject;
import org.json.JSONException;

import org.apache.http.auth.AuthenticationException;

import java.io.IOException;
import java.util.List;


public class UDJLoginLoader extends AsyncTaskLoader<UDJLoginLoader.LoginResult>{
  private static final String TAG = "LoginLoader";

  public enum LoginLoadError{
    NO_ERROR,
    NO_NETWORK_ERROR,
    SERVER_ERROR,
    AUTHENTICATION_ERROR,
    API_VERSION_ERROR
  }

  public static class LoginResult{
    public String userId;
    public String ticketHash;
    public LoginLoadError error;

    public LoginResult(String userId, String ticketHash){
      this.userId = userId;
      this.ticketHash = ticketHash;
      this.error = LoginLoadError.NO_ERROR;
    }

    public LoginResult(String userId, String ticketHash, LoginLoadError error){
      this.userId = userId;
      this.ticketHash = ticketHash;
      this.error = error;
    }
  }

  private CharSequence username;
  private CharSequence password;

  public UDJLoginLoader(Context context, CharSequence username, CharSequence password){
    super(context);
    this.username = username;
    this.password = password;
  }

  public LoginResult loadInBackground(){
    if(!Utils.isNetworkAvailable(getContext())){
      return new LoginResult(null, null, LoginLoadError.NO_NETWORK_ERROR);
    }

    try{
      final ServerConnection.AuthResult result = ServerConnection.authenticate(
              this.username.toString(), this.password.toString());
      return new LoginResult(result.userId, result.ticketHash);
    }
    catch(AuthenticationException e){
      return new LoginResult(null, null, LoginLoadError.AUTHENTICATION_ERROR);
    }
    catch(IOException e){
      return new LoginResult(null, null, LoginLoadError.SERVER_ERROR);
    }
    catch(JSONException e){
      return new LoginResult(null, null, LoginLoadError.SERVER_ERROR);
    }
    catch(APIVersionException e){
      return new LoginResult(null, null, LoginLoadError.API_VERSION_ERROR);
    }
  }

  @Override
  protected void onStartLoading(){
    forceLoad();
  }

}
