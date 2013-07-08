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

import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;

import java.util.Scanner;

import android.content.Context;
import android.os.Parcelable;
import android.os.Parcel;

import org.klnusbaum.udj.exceptions.NoAccountException;

public class UDJAccount implements Parcelable{

  private String userId;
  private String ticketHash;
  private static final String ACCOUNT_FILE_NAME="account_info.json";
  private static final String USER_ID_FIELD_NAME="user_id";
  private static final String TICKET_HASH_FIELD_NAME="ticket_hash";
  private static final String DATA_FILE_PREFIX="data_";
  private static UDJAccount theAccount = null;


  public int describeContents(){
    return 0;
  }

  public void writeToParcel(Parcel dest, int flags){
    dest.writeString(userId);
    dest.writeString(ticketHash);
  }

  public static final Parcelable.Creator<UDJAccount> CREATOR
    = new Parcelable.Creator<UDJAccount>() {
      public UDJAccount createFromParcel(Parcel in) {
        return new UDJAccount(in);
      }

      public UDJAccount[] newArray(int size) {
        return new UDJAccount[size];
      }
    };


  private UDJAccount(Parcel in){
    this.userId = in.readString();
    this.ticketHash = in.readString();
  }

  private UDJAccount(String userId, String ticketHash){
    this.userId = userId;
    this.ticketHash = ticketHash;
  }


  public String getUserId(){
    return userId;
  }

  public String getTicketHash(){
    return ticketHash;
  }

  public static void createUDJAccount(Context context, String userId, String ticketHash){
    JSONObject accountObject = new JSONObject();
    accountObject.put(USER_ID_FIELD_NAME, userId);
    accountObject.put(TICKET_HASH_FIELD_NAME, ticketHash);
    FileOutputStream accountFile = context.openFileOutput(ACCOUNT_FILE_NAME,
                                                                  Context.MODE_PRIVATE);
    accountFile.write(accountObject.toString().getBytes());
    accountFile.close();



    theAccount = new UDJAccount(userId, ticketHash);
  }

  public static UDJAccount getUDJAccount(Context context) throws NoAccountException{
    if(theAccount == null){
      JSONObject accountJSON = getAccountJSON(context);
      theAccount = new UDJAccount(accountJSON.getString(USER_ID_FIELD_NAME),
                                  accountJSON.getString(TICKET_HASH_FIELD_NAME));
    }
    return theAccount;
  }

  private static JSONObject getAccountJSON(Context context) throws NoAccountException{
    try{
      InputStream accountFile = new BufferedInputStream(context.openFileInput(ACCOUNT_FILE_NAME));
      Scanner sc = new Scanner(accountFile);
      StringBuffer inputText = new StringBuffer();
      while(sc.hasNextLine()){
        inputText.append(sc.nextLine());
      }
      sc.close();
      accountFile.close();
      return new JSONObject(inputText.toString());
    }
    catch(FileNotFoundException e){
      throw new NoAccountException();
    }
  }

  public String getUserData(Context context, String key){
    try{
      InputStream accountFile = new BufferedInputStream(context.openFileInput(DATA_FILE_PREFIX + key));
      Scanner sc = new Scanner(accountFile);
      String value = sc.next();
      sc.close();
      accountFile.close();
      return value;
    }
    catch(FileNotFoundException e){
      return null;
    }
  }

  public void setUserData(Context context, String key, String value){
    FileOutputStream fos = context.openFileOutput(DATA_FILE_PREFIX + key, Context.MODE_PRIVATE);
    fos.write(value.getBytes());
    fos.close();
  }

}
