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

import org.klnusbaum.udj.auth.UDJAccount;
import org.klnusbaum.udj.exceptions.NoAccountException;

import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.widget.ListView;
import android.util.Log;

import com.handmark.pulltorefresh.extras.listfragment.PullToRefreshListFragment;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

public abstract class SearchFragment extends PullToRefreshListFragment
  implements LoaderManager.LoaderCallbacks<MusicSearchLoader.MusicSearchResult>,
  OnRefreshListener<ListView>
{
  public static final int LIB_SEARCH_LOADER_TAG = 0;
  public static final String TAG ="SearchFragment";

  /** Adapter used to help display the contents of the library. */
  private MusicSearchAdapter searchAdapter;
  private UDJAccount account;


  @Override
  public void onActivityCreated(Bundle savedInstanceState){
    super.onActivityCreated(savedInstanceState);

    try{
      account = UDJAccount.getUDJAccount();
    }
    catch(NoAccountException e){
      Log.wtf(TAG, "HOLY shit! We made a SearchFragment somehow with out " +
                   "having a UDJ account, pretty sure shits about to blow up.");
      account = null;
    }

    setEmptyText(getActivity().getString(R.string.no_library_songs));

    searchAdapter = new MusicSearchAdapter(getActivity(), account);
    getPullToRefreshListView().getRefreshableView().setAdapter(searchAdapter);
    getPullToRefreshListView().setOnRefreshListener(this);
    setListShown(false);
    getLoaderManager().initLoader(LIB_SEARCH_LOADER_TAG, null, this);
  }


  public Loader<MusicSearchLoader.MusicSearchResult> onCreateLoader(
    int id, Bundle args)
  {
    if(id == LIB_SEARCH_LOADER_TAG){
      return getLoader(account);
    }
    return null;
  }

  @Override
  public void onRefresh(PullToRefreshBase<ListView> listView){
    Log.i(TAG, "In on refresh for SearchFragment");
    getLoaderManager().restartLoader(LIB_SEARCH_LOADER_TAG, null, this);
  }

  public void onLoadFinished(
    Loader<MusicSearchLoader.MusicSearchResult> loader,
    MusicSearchLoader.MusicSearchResult data)
  {
    Log.d("SearchFragment", "In loader finished");
    getPullToRefreshListView().onRefreshComplete();
    if(data.getError() == MusicSearchLoader.MusicSearchError.NO_ERROR){
      searchAdapter.updateList(data.getResults());
    }
    else if(data.getError() ==
      MusicSearchLoader.MusicSearchError.PLAYER_INACTIVE_ERROR)
    {
      Utils.handleInactivePlayer(getActivity(), account);
    }
    else if(data.getError() == MusicSearchLoader.MusicSearchError.NO_LONGER_IN_PLAYER_ERROR){
      Utils.handleNoLongerInPlayer(getActivity(), account);
    }
    else if(data.getError() == MusicSearchLoader.MusicSearchError.KICKED_ERROR){
      Utils.handleKickedFromPlayer(getActivity(), account);
    }

    if(isResumed()){
      setListShown(true);
    }
    else if(isVisible()){
      setListShownNoAnimation(true);
    }
  }

  public void onLoaderReset(Loader<MusicSearchLoader.MusicSearchResult> loader){
    setListAdapter(null);
  }

  public abstract Loader<MusicSearchLoader.MusicSearchResult> getLoader(UDJAccount account);

}
