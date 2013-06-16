import org.json.JSONObject;
import org.io.InputStream;
import org.io.FileInputStream;
import org.io.BufferedInputStream;

import java.util.Scanner;

import android.content.Context;

public class UDJAccount{

  private String userId;
  private String ticketHash;
  private static final String ACCOUNT_FILE_NAME="account_info.json";
  private static final String USER_ID_FIELD_NAME="user_id";
  private static final String TICKET_HASH_FIELD_NAME="ticket_hash";
  private static UDJAccount theAccount = null;

  public String getUserId(){
    return userId;
  }

  public String getTicketHash(){
    return ticketHash;
  }

  private UDJAccount(String userId, String ticketHash){
    this.userId = userId;
    this.ticketHash = ticketHash;
  }

  public static void createUDJAccount(Context context, String userId, String ticketHash){
    JSONObject accountObject = new JSONObject();
    accountObject.put(USER_ID_FIELD_NAME, userId);
    accountObject.put(TICKET_HASH_FIELD_NAME, ticketHash);
    FileOutputStream accountFile = context.openFileOutputStream(ACCOUNT_FILE_NAME,
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
      return new JSONObject(inputText.toString());
    }
    catch(FileNotFoundException e){
      throw new NoAccountException();
    }
    finally{
      sc.close();
      accountFile.close();
    }
  }

}
