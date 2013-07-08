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


import android.content.Context;

import java.util.List;
import java.io.IOException;

import org.json.JSONException;

import org.apache.http.auth.AuthenticationException;
import org.apache.http.ParseException;

import org.klnusbaum.udj.network.ServerConnection;
import org.klnusbaum.udj.containers.LibraryEntry;
import org.klnusbaum.udj.exceptions.NoLongerInPlayerException;
import org.klnusbaum.udj.exceptions.PlayerInactiveException;
import org.klnusbaum.udj.exceptions.KickedException;
import org.klnusbaum.udj.auth.UDJAccount;

public class RegularSearchLoader extends MusicSearchLoader{

  private String query;

  public RegularSearchLoader(
    Context context, String query, UDJAccount account)
  {
    super(context, account);
    this.query = query;
  }

  @Override
  protected MusicSearchResult doSearch(String playerId, String authToken) throws
    JSONException, ParseException, IOException, AuthenticationException, PlayerInactiveException,
    NoLongerInPlayerException, KickedException
  {
    if(query != null && query != ""){
        List<LibraryEntry> list = 
          ServerConnection.availableMusicQuery(query, playerId, authToken);
        return new MusicSearchResult(list);
    }
    return new MusicSearchResult(null, MusicSearchError.NO_SEARCH_ERROR);
  }
}
