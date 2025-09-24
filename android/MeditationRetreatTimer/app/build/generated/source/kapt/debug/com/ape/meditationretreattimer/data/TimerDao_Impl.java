package com.ape.meditationretreattimer.data;

import android.database.Cursor;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.ape.meditationretreattimer.model.Timer;
import com.ape.meditationretreattimer.model.TimerData;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings({"unchecked", "deprecation"})
public final class TimerDao_Impl implements TimerDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Timer> __insertionAdapterOfTimer;

  private final Converters __converters = new Converters();

  private final EntityDeletionOrUpdateAdapter<Timer> __deletionAdapterOfTimer;

  private final EntityDeletionOrUpdateAdapter<Timer> __updateAdapterOfTimer;

  public TimerDao_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTimer = new EntityInsertionAdapter<Timer>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR ABORT INTO `Timer` (`name`,`timer_data`,`id`) VALUES (?,?,nullif(?, 0))";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, Timer value) {
        if (value.getName() == null) {
          stmt.bindNull(1);
        } else {
          stmt.bindString(1, value.getName());
        }
        final String _tmp = __converters.timerDataToString(value.getTimerData());
        if (_tmp == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, _tmp);
        }
        stmt.bindLong(3, value.getId());
      }
    };
    this.__deletionAdapterOfTimer = new EntityDeletionOrUpdateAdapter<Timer>(__db) {
      @Override
      public String createQuery() {
        return "DELETE FROM `Timer` WHERE `id` = ?";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, Timer value) {
        stmt.bindLong(1, value.getId());
      }
    };
    this.__updateAdapterOfTimer = new EntityDeletionOrUpdateAdapter<Timer>(__db) {
      @Override
      public String createQuery() {
        return "UPDATE OR ABORT `Timer` SET `name` = ?,`timer_data` = ?,`id` = ? WHERE `id` = ?";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, Timer value) {
        if (value.getName() == null) {
          stmt.bindNull(1);
        } else {
          stmt.bindString(1, value.getName());
        }
        final String _tmp = __converters.timerDataToString(value.getTimerData());
        if (_tmp == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, _tmp);
        }
        stmt.bindLong(3, value.getId());
        stmt.bindLong(4, value.getId());
      }
    };
  }

  @Override
  public void insert(final Timer timer) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfTimer.insert(timer);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void delete(final Timer timer) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __deletionAdapterOfTimer.handle(timer);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void update(final Timer timer) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfTimer.handle(timer);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public List<Timer> getAll() {
    final String _sql = "SELECT * FROM timer ORDER BY id ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
      final int _cursorIndexOfTimerData = CursorUtil.getColumnIndexOrThrow(_cursor, "timer_data");
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final List<Timer> _result = new ArrayList<Timer>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final Timer _item;
        final String _tmpName;
        if (_cursor.isNull(_cursorIndexOfName)) {
          _tmpName = null;
        } else {
          _tmpName = _cursor.getString(_cursorIndexOfName);
        }
        final TimerData _tmpTimerData;
        final String _tmp;
        if (_cursor.isNull(_cursorIndexOfTimerData)) {
          _tmp = null;
        } else {
          _tmp = _cursor.getString(_cursorIndexOfTimerData);
        }
        _tmpTimerData = __converters.timerDataFromString(_tmp);
        _item = new Timer(_tmpName,_tmpTimerData);
        final int _tmpId;
        _tmpId = _cursor.getInt(_cursorIndexOfId);
        _item.setId(_tmpId);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<Timer> getById(final int id) {
    final String _sql = "SELECT * FROM timer WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
      final int _cursorIndexOfTimerData = CursorUtil.getColumnIndexOrThrow(_cursor, "timer_data");
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final List<Timer> _result = new ArrayList<Timer>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final Timer _item;
        final String _tmpName;
        if (_cursor.isNull(_cursorIndexOfName)) {
          _tmpName = null;
        } else {
          _tmpName = _cursor.getString(_cursorIndexOfName);
        }
        final TimerData _tmpTimerData;
        final String _tmp;
        if (_cursor.isNull(_cursorIndexOfTimerData)) {
          _tmp = null;
        } else {
          _tmp = _cursor.getString(_cursorIndexOfTimerData);
        }
        _tmpTimerData = __converters.timerDataFromString(_tmp);
        _item = new Timer(_tmpName,_tmpTimerData);
        final int _tmpId;
        _tmpId = _cursor.getInt(_cursorIndexOfId);
        _item.setId(_tmpId);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
