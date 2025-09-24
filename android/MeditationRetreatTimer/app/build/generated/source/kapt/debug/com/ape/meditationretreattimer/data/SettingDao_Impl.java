package com.ape.meditationretreattimer.data;

import android.database.Cursor;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.EntityUpsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.ape.meditationretreattimer.model.Setting;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"unchecked", "deprecation"})
public final class SettingDao_Impl implements SettingDao {
  private final RoomDatabase __db;

  private final EntityUpsertionAdapter<Setting> __upsertionAdapterOfSetting;

  public SettingDao_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__upsertionAdapterOfSetting = new EntityUpsertionAdapter<Setting>(new EntityInsertionAdapter<Setting>(__db) {
      @Override
      public String createQuery() {
        return "INSERT INTO `Setting` (`key`,`value`) VALUES (?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, Setting value) {
        if (value.getKey() == null) {
          stmt.bindNull(1);
        } else {
          stmt.bindString(1, value.getKey());
        }
        if (value.getValue() == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.getValue());
        }
      }
    }, new EntityDeletionOrUpdateAdapter<Setting>(__db) {
      @Override
      public String createQuery() {
        return "UPDATE `Setting` SET `key` = ?,`value` = ? WHERE `key` = ?";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, Setting value) {
        if (value.getKey() == null) {
          stmt.bindNull(1);
        } else {
          stmt.bindString(1, value.getKey());
        }
        if (value.getValue() == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.getValue());
        }
        if (value.getKey() == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.getKey());
        }
      }
    });
  }

  @Override
  public void upsert(final Setting setting) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __upsertionAdapterOfSetting.upsert(setting);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public List<Setting> getAllQuery() {
    final String _sql = "SELECT key, value FROM Setting ORDER BY key ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfKey = 0;
      final int _cursorIndexOfValue = 1;
      final List<Setting> _result = new ArrayList<Setting>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final Setting _item;
        final String _tmpKey;
        if (_cursor.isNull(_cursorIndexOfKey)) {
          _tmpKey = null;
        } else {
          _tmpKey = _cursor.getString(_cursorIndexOfKey);
        }
        final String _tmpValue;
        if (_cursor.isNull(_cursorIndexOfValue)) {
          _tmpValue = null;
        } else {
          _tmpValue = _cursor.getString(_cursorIndexOfValue);
        }
        _item = new Setting(_tmpKey,_tmpValue);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public String get(final String key) {
    final String _sql = "SELECT value FROM Setting WHERE key = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (key == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, key);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final String _result;
      if(_cursor.moveToFirst()) {
        if (_cursor.isNull(0)) {
          _result = null;
        } else {
          _result = _cursor.getString(0);
        }
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public Map<String, String> getAll() {
    return SettingDao.DefaultImpls.getAll(SettingDao_Impl.this);
  }

  @Override
  public void set(final String key, final String value) {
    SettingDao.DefaultImpls.set(SettingDao_Impl.this, key, value);
  }

  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
