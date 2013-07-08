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

import com.actionbarsherlock.app.SherlockFragmentActivity;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.os.Bundle;

public class AuthActivity extends SherlockFragmentActivity{

  @Override
  protected void onCreate(Bundle icicle){
    super.onCreate(icicle);
    setContentView(R.layout.login_activity);

    if(icicle == null){
      FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
      UDJLoginFragment udjLoginFragment = new UDJLoginFragment();
      ft.add(R.id.content, udjLoginFragment)
      ft.commit();
    }
  }

  public static class UDJLoginFragment extends Fragment{

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle icicle){
      View toReturn = inflater.inflate(R.layout.udj_login_fragment, container);
      return toReturn;
    }

  }

}

