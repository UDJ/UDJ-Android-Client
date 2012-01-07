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
#include "SongListView.hpp"
#include "DataStore.hpp"
#include <QSqlQueryModel>
#include <QSqlQuery>
#include <QAction>
#include <QMenu>
#include <QModelIndex>
#include <QSqlRecord>
#include <QHeaderView>


namespace UDJ{


SongListView::SongListView(DataStore *dataStore, QWidget *parent):
  QTableView(parent),
  dataStore(dataStore),
  currentSongListId(-1)
{
  setEditTriggers(QAbstractItemView::NoEditTriggers);
  songListEntryModel = new QSqlQueryModel(this);
  setModel(songListEntryModel);
  horizontalHeader()->setStretchLastSection(true);
  setSelectionBehavior(QAbstractItemView::SelectRows);
  setContextMenuPolicy(Qt::CustomContextMenu);
  connect(this, SIGNAL(customContextMenuRequested(const QPoint&)),
    this, SLOT(handleContextMenuRequest(const QPoint&)));
}

void SongListView::handleContextMenuRequest(const QPoint &pos){
/*  QMenu contextMenu(this);
  contextMenu.addAction(addToActivePlaylist);
  contextMenu.addAction(removeFromAvailableMusic);
  contextMenu.exec(QCursor::pos());*/
}

void SongListView::onSongListEntriesChanged(
  song_list_id_t updatedSongList)
{
  if(updatedSongList==currentSongListId){ 
    QSqlQuery query("SELECT * FROM " +
    DataStore::getSongListEntryTableName() + " where " + 
    DataStore::getSongListEntrySongListIdColName() + "=?;",
    dataStore->getDatabaseConnection());
    query.addBindValue(QVariant::fromValue(currentSongListId));
    songListEntryModel->setQuery(query);
  }
}

void SongListView::setSongListId(song_list_id_t songListId){
  currentSongListId = songListId;
  onSongListEntriesChanged(currentSongListId);
}


} //end namespace
