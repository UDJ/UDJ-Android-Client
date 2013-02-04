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

import android.util.Log;
import android.location.Location;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.io.IOException;
import java.util.ArrayList;
import java.net.URI;
import java.net.URISyntaxException;


import org.apache.http.params.BasicHttpParams;
import org.apache.http.message.BasicHeader;
import org.apache.http.HttpVersion;
import org.apache.http.Header;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.http.protocol.HTTP;
import org.apache.http.conn.ssl.SSLSocketFactory;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;


import org.klnusbaum.udj.Constants;
import org.klnusbaum.udj.containers.LibraryEntry;
import org.klnusbaum.udj.containers.Player;
import org.klnusbaum.udj.exceptions.KickedException;
import org.klnusbaum.udj.exceptions.PlayerFullException;
import org.klnusbaum.udj.exceptions.NoLongerInPlayerException;
import org.klnusbaum.udj.exceptions.PlayerInactiveException;
import org.klnusbaum.udj.exceptions.APIVersionException;
import org.klnusbaum.udj.exceptions.PlayerPasswordException;
import org.klnusbaum.udj.exceptions.BannedException;



/**
 * A connection to the UDJ server
 */
public class ServerConnection{

  private static final String API_URL_PREFIX = "/udj/0_6/";

  private static final String TAG = "ServerConnection";
  private static final String PARAM_USERNAME = "username";

  private static final String PARAM_PASSWORD = "password";

  private static final String PARAM_PLAYER_NAME = "name";
  /**
   * This port number is a memorial to Keith Nusbaum, my father. I loved him
   * deeply and he was taken from this world far too soon. Never-the-less 
   * we all continue to benefit from his good deeds. Without him, I wouldn't 
   * be here, and there would be no UDJ. Please, don't change this port 
   * number. Keep the memory of my father alive.
   * K = 10 % 10 = 0
   * e = 4  % 10 = 4
   * i = 8  % 10 = 8
   * t = 19 % 10 = 9
   * h = 7  % 10 = 7
   * Port 4897, the Keith Nusbaum Memorial Port
   */
  private static final int SERVER_PORT = 4897;

  private static final String NETWORK_PROTOCOL = "https";

  private static final String SERVER_HOST = "udjplayer.com";


  private static final String TICKET_HASH_HEADER = "X-Udj-Ticket-Hash";
  private static final String MISSING_RESOURCE_HEADER = "X-Udj-Missing-Resource";
  private static final String MISSING_RESOURCE_REASON_HEADER = "X-Udj-Missing-Reason";
  private static final String WWW_AUTH_HEADER = "WWW-Authenticate";
  private static final String FORBIDDEN_REASON_HEADER = "X-Udj-Forbidden-Reason";

  private static final String PLAYER_PASSWORD_HEADER = "X-Udj-Player-Password";


  //private static final int REGISTRATION_TIMEOUT = 30 * 1000; // ms


  private static DefaultHttpClient httpClient;

  public static DefaultHttpClient getHttpClient() throws IOException{
    if(httpClient == null){
      SchemeRegistry schemeReg = new SchemeRegistry();
      schemeReg.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), SERVER_PORT));
      BasicHttpParams params = new BasicHttpParams();
      ConnManagerParams.setMaxTotalConnections(params, 100);
      HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
      HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
      HttpProtocolParams.setUseExpectContinue(params, true);

      ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(
        params, schemeReg);
      httpClient = new DefaultHttpClient(cm, params);
    }
    return httpClient;
  }


  public static class AuthResult{
    public String ticketHash;
    public String userId;

    public AuthResult(String ticketHash, String userId){
      this.ticketHash = ticketHash;
      this.userId = userId;
    }
  }

  public static AuthResult authenticate(String username, String password)
    throws AuthenticationException, IOException, APIVersionException, JSONException
  {
    URI AUTH_URI = null;
    try{
      AUTH_URI = new URI(
        NETWORK_PROTOCOL, null,
        SERVER_HOST, SERVER_PORT, API_URL_PREFIX + "auth", null, null);
    }
    catch(URISyntaxException e){
      //TODO should never get here but I should do something if it does.
    }
    final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
    params.add(new BasicNameValuePair(PARAM_USERNAME, username));
    params.add(new BasicNameValuePair(PARAM_PASSWORD, password));
    HttpEntity entity = null;
    entity = new UrlEncodedFormEntity(params);
    final HttpPost post = new HttpPost(AUTH_URI);
    post.addHeader(entity.getContentType());
    post.setEntity(entity);
    final HttpResponse resp = getHttpClient().execute(post);
    Log.d(TAG, "Auth Status code was " + resp.getStatusLine().getStatusCode());
    final String response = EntityUtils.toString(resp.getEntity());
    Log.d(TAG, "Auth Response was " + response);
    if(resp.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_IMPLEMENTED){
      throw new APIVersionException();
    }
    else if(resp.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED){
      throw new AuthenticationException();
    }
    else{
      JSONObject authResponse = new JSONObject(response);
      return new AuthResult(
        authResponse.getString("ticket_hash"),
        authResponse.getString("user_id"));
    }
  }

  private static void basicResponseErrorCheck(
    HttpResponse resp, 
    String response
  )
    throws AuthenticationException, IOException
  {
    if(resp.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED){
      throw new AuthenticationException();
    }
    else if(
      resp.getStatusLine().getStatusCode() == HttpStatus.SC_BAD_REQUEST || 
      resp.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND || 
      resp.getStatusLine().getStatusCode() >= 500)
    {
      //TODO this should just be "General server error"
      Log.e(TAG, "Basic Response Error Check got an error code: " + resp.getStatusLine().getStatusCode());
      throw new IOException(response);
    }
  }

  private static void playerInactiveErrorCheck(HttpResponse resp)
    throws PlayerInactiveException
  {
    if(resp.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND
      && resp.containsHeader(MISSING_RESOURCE_HEADER)
      && resp.getFirstHeader(MISSING_RESOURCE_HEADER).getValue().equals("player")
      && resp.containsHeader(MISSING_RESOURCE_REASON_HEADER)
      && resp.getFirstHeader(MISSING_RESOURCE_REASON_HEADER).getValue().equals("inactive"))
    {
        throw new PlayerInactiveException();
    }
  }

  private static void noLongerInPlayerErrorCheck(HttpResponse resp)
    throws NoLongerInPlayerException
  {
    if(resp.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED
      && resp.containsHeader(WWW_AUTH_HEADER)
      && resp.getFirstHeader(WWW_AUTH_HEADER).getValue().equals("begin-participating"))
    {
        throw new NoLongerInPlayerException();
    }

  }

  private static void kickedFromPlayerCheck(HttpResponse resp)
    throws KickedException
  {
    if(resp.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED
      && resp.containsHeader(WWW_AUTH_HEADER)
      && resp.getFirstHeader(WWW_AUTH_HEADER).getValue().equals("kicked"))
    {
      throw new KickedException();
    }
  }


  public static HttpResponse doGet(URI uri, String ticketHash)
    throws IOException
  {
    Log.d(TAG, "Doing get with uri: " + uri);
    final HttpGet get = new HttpGet(uri);
    get.addHeader(TICKET_HASH_HEADER, ticketHash);
    return getHttpClient().execute(get);
  }

  public static String doSimpleGet(URI uri, String ticketHash)
    throws AuthenticationException, IOException
  {
    final HttpResponse resp = doGet(uri, ticketHash);
    final String response = EntityUtils.toString(resp.getEntity());
    Log.d(TAG, "Simple get response: " + resp.getStatusLine().getStatusCode() +
        "\"" + response +"\"");
    basicResponseErrorCheck(resp, response);
    return response;
  }

  public static String doPlayerRelatedGet(URI uri, String ticketHash)
    throws AuthenticationException, IOException, PlayerInactiveException, NoLongerInPlayerException,
    KickedException
  {
    final HttpResponse resp = doGet(uri, ticketHash);
    Log.d(TAG, "Doing player related get");
    final String response = EntityUtils.toString(resp.getEntity());
    playerInactiveErrorCheck(resp);
    noLongerInPlayerErrorCheck(resp);
    kickedFromPlayerCheck(resp);
    basicResponseErrorCheck(resp, response);
    return response;
  }

  public static HttpResponse doPut(URI uri, String ticketHash, String payload)
    throws IOException
  {
    return doPut(uri, ticketHash, payload, new HashSet<Header>());
  }

  public static HttpResponse doPut(URI uri, String ticketHash, String payload, Set<Header> headers)
    throws IOException
  {
    Log.d(TAG, "Doing put to uri: " + uri);
    Log.d(TAG, "Put payload is: "+ (payload != null ? payload : "no payload"));
    final HttpPut put = new HttpPut(uri);
    put.addHeader(TICKET_HASH_HEADER, ticketHash);
    for(Header h: headers){
      put.addHeader(h);
    }
    if(payload != null){
      StringEntity entity = new StringEntity(payload);
      entity.setContentType("text/json");
      put.addHeader(entity.getContentType());
      put.setEntity(entity);
    }
    return getHttpClient().execute(put);
  }

  public static String doSimplePut(URI uri, String ticketHash, String payload)
    throws AuthenticationException, IOException
  {
    final HttpResponse resp = doPut(uri, ticketHash, payload);
    final String response = EntityUtils.toString(resp.getEntity());
    Log.d(TAG, "Simple put response: " + resp.getStatusLine().getStatusCode() +
        "\"" + response +"\"");
    basicResponseErrorCheck(resp, response);
    return response;
  }

  public static String doPlayerRelatedPut( 
    URI uri, String ticketHash, String payload)
    throws AuthenticationException, IOException, PlayerInactiveException, 
    NoLongerInPlayerException, KickedException
  {
    final HttpResponse resp = doPut(uri, ticketHash, payload);
    final String response = EntityUtils.toString(resp.getEntity());
    Log.d(TAG, "Player related Put response: \"" + response +"\"");
    Log.d(TAG, "Player status code: \"" + resp.getStatusLine().getStatusCode());
    playerInactiveErrorCheck(resp);
    noLongerInPlayerErrorCheck(resp);
    kickedFromPlayerCheck(resp);
    basicResponseErrorCheck(resp, response);
    return response;
  }

  public static HttpResponse doPost(URI uri, String authToken, String payload, boolean isJSON)
    throws AuthenticationException, IOException
  {
    Log.d(TAG, "Doing post to uri: " + uri);
    Log.d(TAG, "Post payload is: "+ (payload != null ? payload : "no payload"));
    final HttpPost post = new HttpPost(uri);
    if(payload != null){
      StringEntity entity = new StringEntity(payload);
      if(isJSON){
        entity.setContentType("text/json");
      }
      post.setEntity(entity);
    }
    post.addHeader(TICKET_HASH_HEADER, authToken);
    return getHttpClient().execute(post);
  }

  public static String doSimplePost(URI uri, String authToken, String payload, boolean isJSON)
    throws AuthenticationException, IOException
  {
    final HttpResponse resp = doPost(uri, authToken, payload, isJSON);
    final String response = EntityUtils.toString(resp.getEntity());
    Log.d(TAG, "Simple post response: " + resp.getStatusLine().getStatusCode() +
        "\"" + response +"\"");
    basicResponseErrorCheck(resp, response);
    return response;
  }

  public static String doPlayerRelatedPost(
    URI uri, String authToken, String payload, boolean isJSON)
    throws AuthenticationException, IOException, PlayerInactiveException, NoLongerInPlayerException,
    KickedException
  {
    final HttpResponse resp = doPost(uri, authToken, payload, isJSON);
    final String response = EntityUtils.toString(resp.getEntity());
    Log.d(TAG, "Player post response: " + resp.getStatusLine().getStatusCode() +
        "\"" + response +"\"");
    playerInactiveErrorCheck(resp);
    kickedFromPlayerCheck(resp);
    basicResponseErrorCheck(resp, response);
    noLongerInPlayerErrorCheck(resp);
    return response;

  }
   
  public static HttpResponse doDelete(URI uri, String ticketHash)
    throws IOException, AuthenticationException
  {
    Log.d(TAG, "Doing delete to uri: " + uri);
    final HttpDelete delete = new HttpDelete(uri);
    delete.addHeader(TICKET_HASH_HEADER, ticketHash);
    return getHttpClient().execute(delete);
  }

  public static void doSimpleDelete(URI uri, String ticketHash)
    throws IOException, AuthenticationException
  {
    final HttpResponse resp = doDelete(uri, ticketHash);
    final String response = EntityUtils.toString(resp.getEntity());
    Log.d(TAG, "Delete response: " + resp.getStatusLine().getStatusCode() +
        "\"" + response +"\"");
    basicResponseErrorCheck(resp, response);
  }

  public static void doPlayerRelatedDelete(URI uri, String ticketHash)
    throws IOException, AuthenticationException, PlayerInactiveException, NoLongerInPlayerException,
    KickedException
  {
    final HttpResponse resp = doDelete(uri, ticketHash);
    final String response = EntityUtils.toString(resp.getEntity());
    Log.d(TAG, "Player related Delete response: " + resp.getStatusLine().getStatusCode() +
        "\"" + response +"\"");
    playerInactiveErrorCheck(resp);
    noLongerInPlayerErrorCheck(resp);
    kickedFromPlayerCheck(resp);
    basicResponseErrorCheck(resp, response);
  }

  public static List<Player> getNearbyPlayers(
    Location location, String ticketHash)
    throws
    JSONException, ParseException, IOException, AuthenticationException
  {
    if(location == null) return null;
    try{
      URI playersQuery = new URI(
        NETWORK_PROTOCOL, null, SERVER_HOST, SERVER_PORT, 
        API_URL_PREFIX + "players/" + location.getLatitude() + "/" + location.getLongitude(),
        null, null);
      JSONArray players = new JSONArray(doSimpleGet(playersQuery, ticketHash));
      return Player.fromJSONArray(players);
    }
    catch(URISyntaxException e){
      return null;
      //TDOD inform caller that their query is bad 
    }
  }

  public static List<Player> searchForPlayers(
    String query, String ticketHash)
    throws
    JSONException, ParseException, IOException, AuthenticationException
  {
    try{
      URI playersQuery = new URI(
        NETWORK_PROTOCOL, null, SERVER_HOST, SERVER_PORT, 
        API_URL_PREFIX + "players",
        PARAM_PLAYER_NAME+"="+query, null);
      JSONArray players = new JSONArray(doSimpleGet(playersQuery, ticketHash));
      return Player.fromJSONArray(players);
    }
    catch(URISyntaxException e){
      return null;
      //TDOD inform caller that their query is bad 
    }
  }

  public static void joinPlayer(String playerId, String ticketHash)
    throws IOException, AuthenticationException, PlayerInactiveException, 
    JSONException, ParseException, PlayerPasswordException, PlayerFullException,
    BannedException
  {
    joinPlayer(playerId, "", ticketHash);
  }


  public static void joinPlayer(String playerId, String password, String ticketHash)
    throws IOException, AuthenticationException, PlayerInactiveException,
    JSONException, ParseException, PlayerPasswordException, PlayerFullException,
    BannedException
  {
    try{
      URI uri  = new URI(
        NETWORK_PROTOCOL, null, SERVER_HOST, SERVER_PORT, 
        API_URL_PREFIX + "players/" + playerId + "/users/user",
        null, null);
      final HttpResponse resp;
      if(password == null || password.equals("")){
        resp = doPut(uri, ticketHash, "");
      }
      else{
        final HashSet<Header> headers = new HashSet<Header>();
        headers.add(new BasicHeader(PLAYER_PASSWORD_HEADER, password));
        resp = doPut(uri, ticketHash, "", headers);
      }
      final String response = EntityUtils.toString(resp.getEntity());
      Log.d(TAG, "Player join Put response: \"" + response +"\"");
      playerInactiveErrorCheck(resp);
      if(
        resp.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED
        && resp.containsHeader(WWW_AUTH_HEADER)
        && resp.getFirstHeader(WWW_AUTH_HEADER).getValue().equals("player-password"))
      {
        throw new PlayerPasswordException();
      }
      if(
        resp.getStatusLine().getStatusCode() == HttpStatus.SC_FORBIDDEN
        && resp.containsHeader(FORBIDDEN_REASON_HEADER)
        && resp.getFirstHeader(FORBIDDEN_REASON_HEADER).getValue().equals("player-full"))
      {
        throw new PlayerFullException();
      }
      if(
        resp.getStatusLine().getStatusCode() == HttpStatus.SC_FORBIDDEN
        && resp.containsHeader(FORBIDDEN_REASON_HEADER)
        && resp.getFirstHeader(FORBIDDEN_REASON_HEADER).getValue().equals("banned"))
      {
        throw new BannedException();
      }
      basicResponseErrorCheck(resp, response);
    }
    catch(URISyntaxException e){
      Log.e(TAG, "URI syntax error in join player");
    }
  }

  public static JSONObject getActivePlaylist(String playerId, String authToken)
    throws JSONException, ParseException, IOException, AuthenticationException,
    PlayerInactiveException, NoLongerInPlayerException, KickedException
  {
    try{
      URI uri = new URI(
        NETWORK_PROTOCOL, null, SERVER_HOST, SERVER_PORT, 
        API_URL_PREFIX + "players/"+playerId+"/active_playlist",
        null, null);
      return new JSONObject(doPlayerRelatedGet(uri, authToken));
    }
    catch(URISyntaxException e){
      return null;
      //TODO inform caller that their query is bad 
    }
  }


  public static List<LibraryEntry> availableMusicQuery(
    String query, String playerId, String authToken)
    throws JSONException, ParseException, IOException, AuthenticationException,
    PlayerInactiveException, NoLongerInPlayerException, KickedException
  {
    try{
      URI uri = new URI(
        NETWORK_PROTOCOL, null, SERVER_HOST, SERVER_PORT,
        API_URL_PREFIX + "players/"+playerId+"/available_music",
        "query="+query, null);
      JSONArray libEntries = new JSONArray(doPlayerRelatedGet(uri, authToken));
      return LibraryEntry.fromJSONArray(libEntries);
    }
    catch(URISyntaxException e){
      //TODO inform caller that their query is bad 
    }
    return null;
  }

  public static List<String> getArtists(String playerId, String authToken)
    throws JSONException, ParseException, IOException, AuthenticationException,
    PlayerInactiveException, NoLongerInPlayerException, KickedException
  {
    try{
      URI uri = new URI(
        NETWORK_PROTOCOL, null, SERVER_HOST, SERVER_PORT,
        API_URL_PREFIX + "players/"+playerId+"/available_music/artists",
        null, null);
      JSONArray artists = new JSONArray(doPlayerRelatedGet(uri, authToken));
      return toStringList(artists);
    }
    catch(URISyntaxException e){
      //TODO inform caller that their query is bad 
    }
    return null;
  }

  public static List<LibraryEntry> getSongsByArtists(
    String artistQuery, String playerId, String authToken)
    throws JSONException, ParseException, IOException, AuthenticationException,
    PlayerInactiveException, NoLongerInPlayerException, KickedException
  {
    try{
      URI uri = new URI(
        NETWORK_PROTOCOL, null, SERVER_HOST, SERVER_PORT,
        API_URL_PREFIX + "players/"+playerId+"/available_music/artists/"+artistQuery,
        null, null);
      JSONArray libEntries = new JSONArray(doPlayerRelatedGet(uri, authToken));
      return LibraryEntry.fromJSONArray(libEntries);
    }
    catch(URISyntaxException e){
      //TODO inform caller that their query is bad 
    }
    return null;
  }


  public static List<LibraryEntry> getRandomMusic(int max, String playerId, String authToken)
    throws JSONException, ParseException, IOException, AuthenticationException,
    PlayerInactiveException, NoLongerInPlayerException, KickedException
  {
    try{
      URI uri = new URI(
        NETWORK_PROTOCOL, null, SERVER_HOST, SERVER_PORT,
        API_URL_PREFIX + "players/"+playerId+"/available_music/random_songs",
        "max_randoms="+String.valueOf(max), null);
      JSONArray libEntries = new JSONArray(doPlayerRelatedGet(uri, authToken));
      return LibraryEntry.fromJSONArray(libEntries);
    }
    catch(URISyntaxException e){
      //TODO inform caller that their query is bad 
    }
    return null;
  }

  public static List<LibraryEntry> getRecentlyPlayedLibEntries(int max, String playerId, String authToken)
    throws JSONException, ParseException, IOException, AuthenticationException,
    PlayerInactiveException, NoLongerInPlayerException, KickedException
  {
    try{
      URI uri = new URI(
        NETWORK_PROTOCOL, null, SERVER_HOST, SERVER_PORT,
        API_URL_PREFIX + "players/"+playerId+"/recently_played",
        "max_songs="+String.valueOf(max), null);
      JSONArray recentlyPlayedEntries = new JSONArray(doPlayerRelatedGet(uri, authToken));
      return LibraryEntry.fromRecentlyPlayedJSONArray(recentlyPlayedEntries);
    }
    catch(URISyntaxException e){
      //TODO inform caller that their query is bad 
    }
    return null;
  }



  public static void addSongToActivePlaylist(
    String playerId, String libId, String authToken)
    throws JSONException, ParseException, IOException, AuthenticationException,
    PlayerInactiveException, NoLongerInPlayerException, KickedException
  {
    try{
      URI uri = new URI(
        NETWORK_PROTOCOL, null, SERVER_HOST, SERVER_PORT,
        API_URL_PREFIX + "players/"+playerId+"/active_playlist/songs/"+libId,
        null, null);
      Log.d(TAG, "Add song to active playlist: " + libId);
      doPlayerRelatedPut(uri, authToken, ""); 
    }
    catch(URISyntaxException e){
      //TODO inform caller that their query is bad 
    }
  }

  public static void removeSongFromActivePlaylist(String playerId, String libId, String authToken)
    throws IOException, AuthenticationException, PlayerInactiveException, NoLongerInPlayerException,
    KickedException
  {

    try{
      URI uri = new URI(
          NETWORK_PROTOCOL, null, SERVER_HOST, SERVER_PORT,
          API_URL_PREFIX + "players/"+playerId+"/active_playlist/songs/"+libId,
          null, null);
      Log.d(TAG, "Add remove song from active playlist: " + libId);
      doPlayerRelatedDelete(uri, authToken);
    }
    catch(URISyntaxException e){
      //TODO inform caller that their query is bad 
    }
  }

  public static void setCurrentSong(String playerId, String libId, String authToken)
    throws IOException, AuthenticationException, PlayerInactiveException, NoLongerInPlayerException,
    KickedException
  {
    try{
      URI uri = new URI(
          NETWORK_PROTOCOL, null, SERVER_HOST, SERVER_PORT,
          API_URL_PREFIX + "players/"+playerId+"/current_song",
          null, null);
      Log.d(TAG, "Set current song to: " + libId);
      doPlayerRelatedPost(uri, authToken, "lib_id="+libId, false);
    }
    catch(URISyntaxException e){
      //TODO inform caller that their query is bad 
    }
  }

  public static void setPlaybackState(String playerId, int desiredPlaybackState,
      String authToken)
    throws IOException, AuthenticationException, PlayerInactiveException, NoLongerInPlayerException,
    KickedException
  {
    try{
      URI uri = new URI(
          NETWORK_PROTOCOL, null, SERVER_HOST, SERVER_PORT,
          API_URL_PREFIX + "players/"+playerId+"/state",
          null, null);
      Log.d(TAG, "Setting playback state: " + desiredPlaybackState);
      String plState = "";
      if(desiredPlaybackState == Constants.PAUSED_STATE){
        plState = "paused";
      }
      else if(desiredPlaybackState == Constants.PLAYING_STATE){
        plState = "playing";
      }
      doPlayerRelatedPost(uri, authToken, "state="+plState, false);
    }
    catch(URISyntaxException e){
      //TODO inform caller that their query is bad 
    }
  }

  public static void setPlayerVolume(String playerId, int desiredVolume,
      String authToken)
    throws IOException, AuthenticationException, PlayerInactiveException, NoLongerInPlayerException,
    KickedException
  {
    try{
      URI uri = new URI(
          NETWORK_PROTOCOL, null, SERVER_HOST, SERVER_PORT,
          API_URL_PREFIX + "players/"+playerId+"/volume",
          null, null);
      Log.d(TAG, "Setting player volume: " + desiredVolume);
      doPlayerRelatedPost(uri, authToken, "volume="+String.valueOf(desiredVolume), false);
    }
    catch(URISyntaxException e){
      //TODO inform caller that their query is bad 
    }
  }



  public static void voteOnSong(
    String playerId, String libId, int voteType, String authToken)
    throws IOException, AuthenticationException, PlayerInactiveException, NoLongerInPlayerException,
    KickedException
  {
    String voteString = null;
    if(voteType == 1){
      voteString = "upvote";
    }
    else if(voteType == -1){
      voteString = "downvote";
    }
    else{
      throw new IllegalArgumentException("Vote type must be either 1 or -1");
    }
    try{
      URI uri = new URI(
        NETWORK_PROTOCOL, null, SERVER_HOST, SERVER_PORT,
        API_URL_PREFIX + "players/"+playerId+"/active_playlist/songs/" + libId +"/" + voteString,
        null, null);
      doPlayerRelatedPost(uri, authToken, null, false);
    }
    catch(URISyntaxException e){
      //TODO inform caller that their query is bad 
    }
  }

  private static List<String> toStringList(final JSONArray array)
    throws JSONException
  {
    ArrayList<String> toReturn = new ArrayList();
    for(int i=0; i<array.length(); ++i){
      toReturn.add(array.getString(i));
    }
    return toReturn;
  }
}
