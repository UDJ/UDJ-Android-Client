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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.support.v4.app.*;
import android.support.v4.content.Loader;
import android.widget.EditText;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.os.Bundle;

import org.klnusbaum.udj.PlayerSelectorActivity;
import org.klnusbaum.udj.R;

public class AuthActivity extends SherlockFragmentActivity{

  private static final String PROGRESS_FRAGMENT_TAG = "progressdialog";
  private static final String ERROR_FRAGMENT_TAG = "errorfragment";

  @Override
  protected void onCreate(Bundle icicle){
    super.onCreate(icicle);
    setContentView(R.layout.login_activity);
    setResult(RESULT_CANCELED);

    if(icicle == null){
      FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
      UDJLoginFragment udjLoginFragment = new UDJLoginFragment();
      ft.add(R.id.content, udjLoginFragment);
      ft.commit();
    }
  }

  public static class UDJLoginFragment extends Fragment implements
    LoaderManager.LoaderCallbacks<UDJLoginLoader.LoginResult>
  {

    private static final String PROG_DIALOG_TAG = "prog_dialog";
    private static final int LOGIN_LOADER_ID = 0;
    private EditText usernameEdit, passwordEdit;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle icicle){
      View toReturn = inflater.inflate(R.layout.udj_login_fragment, container);
      usernameEdit = (EditText)toReturn.findViewById(R.id.username_edit);
      passwordEdit = (EditText)toReturn.findViewById(R.id.password_edit);
      return toReturn;
    }


    public void preformLogin(View view){
      getLoaderManager().restartLoader(LOGIN_LOADER_ID, null, this);
      showLoginProgress();
      //call loader manager restart 
    }


    public Loader<UDJLoginLoader.LoginResult> onCreateLoader(int id, Bundle args){
      switch (id) {
      case LOGIN_LOADER_ID:
        return new UDJLoginLoader(getActivity(), usernameEdit.getText(), passwordEdit.getText());
      default:
        return null;
      }
    }


    public void onLoadFinished(Loader<UDJLoginLoader.LoginResult> loader,
                               UDJLoginLoader.LoginResult data)
    {
      dismissLoginProgress();
      if(data.error == UDJLoginLoader.LoginLoadError.NO_ERROR){
        getActivity().setResult(Activity.RESULT_OK);
        if(getActivity().getIntent().getAction() == Intent.ACTION_MAIN){
          final Intent playerSelectorIntent = new Intent(getActivity(), PlayerSelectorActivity.class);
          getActivity().startActivity(playerSelectorIntent);
        }
        else{
          getActivity().finish();
        }
      }
      else{
        alertLoginError(data.error);
      }
    }

    public void onLoaderReset(Loader<UDJLoginLoader.LoginResult> loader){
      // we don't need to do anything here.
    }

    private void alertLoginError(UDJLoginLoader.LoginLoadError error){
      Bundle args = new Bundle();
      args.putString(LoginErrorDialog.LOGIN_LOAD_ERROR_EXTRA, error.toString());
      LoginErrorDialog led = new LoginErrorDialog();
      led.setArguments(args);
      led.show(getActivity().getSupportFragmentManager(), ERROR_FRAGMENT_TAG);
    }

    private void showLoginProgress(){
      ProgressFragment progFragment = new ProgressFragment();
      progFragment.show(getActivity().getSupportFragmentManager(), PROGRESS_FRAGMENT_TAG);
    }

    private void dismissLoginProgress(){
      ProgressFragment pd = (ProgressFragment)getActivity().getSupportFragmentManager().findFragmentByTag(PROG_DIALOG_TAG);
      pd.dismiss();
    }

  }


  public static class ProgressFragment extends DialogFragment{

    public Dialog onCreateDialog(Bundle icicle){
      //Stop gap for now. This should be cancelable
      setCancelable(false);
      final ProgressDialog dialog = new ProgressDialog(getActivity());
      dialog.setMessage(getActivity().getString(R.string.authenticating));
      dialog.setIndeterminate(true);
      return dialog;
    }

  }

  public static class LoginErrorDialog extends DialogFragment{
    public static final String LOGIN_LOAD_ERROR_EXTRA = "LoginLoadError";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
      Bundle args = getArguments();
      final UDJLoginLoader.LoginLoadError loginError =
        UDJLoginLoader.LoginLoadError.valueOf(args.getString(LOGIN_LOAD_ERROR_EXTRA));
      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
        .setTitle(R.string.login_error);
      String message;
      switch(loginError){
      case NO_NETWORK_ERROR:
        message = getString(R.string.login_error_no_network);
        break;
      case SERVER_ERROR:
        message = getString(R.string.login_error_server);
        break;
      case AUTHENTICATION_ERROR:
        message = getString(R.string.login_error_auth);
        break;
      case API_VERSION_ERROR:
        message = getString(R.string.login_error_auth);
        break;
      default:
        message = getString(R.string.unknown_error_message);
      }
      return builder
        .setMessage(message)
        .setPositiveButton(
          android.R.string.ok,
          new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int whichButton){
              dismiss();
            }
          })
        .create();
    }
  }


}

