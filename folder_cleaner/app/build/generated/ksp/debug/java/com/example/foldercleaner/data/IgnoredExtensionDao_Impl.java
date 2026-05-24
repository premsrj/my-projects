package com.example.foldercleaner.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class IgnoredExtensionDao_Impl implements IgnoredExtensionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<IgnoredExtensionEntity> __insertionAdapterOfIgnoredExtensionEntity;

  private final EntityInsertionAdapter<IgnoredExtensionEntity> __insertionAdapterOfIgnoredExtensionEntity_1;

  private final SharedSQLiteStatement __preparedStmtOfClearAll;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByExtension;

  public IgnoredExtensionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfIgnoredExtensionEntity = new EntityInsertionAdapter<IgnoredExtensionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `ignored_extensions` (`extension`) VALUES (?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final IgnoredExtensionEntity entity) {
        statement.bindString(1, entity.getExtension());
      }
    };
    this.__insertionAdapterOfIgnoredExtensionEntity_1 = new EntityInsertionAdapter<IgnoredExtensionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `ignored_extensions` (`extension`) VALUES (?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final IgnoredExtensionEntity entity) {
        statement.bindString(1, entity.getExtension());
      }
    };
    this.__preparedStmtOfClearAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM ignored_extensions";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteByExtension = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM ignored_extensions WHERE extension = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final IgnoredExtensionEntity extension,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfIgnoredExtensionEntity.insert(extension);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAll(final List<IgnoredExtensionEntity> extensions,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfIgnoredExtensionEntity_1.insert(extensions);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object clearAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteByExtension(final String extension,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByExtension.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, extension);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteByExtension.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<IgnoredExtensionEntity>> observeAll() {
    final String _sql = "SELECT * FROM ignored_extensions ORDER BY extension ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"ignored_extensions"}, new Callable<List<IgnoredExtensionEntity>>() {
      @Override
      @NonNull
      public List<IgnoredExtensionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfExtension = CursorUtil.getColumnIndexOrThrow(_cursor, "extension");
          final List<IgnoredExtensionEntity> _result = new ArrayList<IgnoredExtensionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final IgnoredExtensionEntity _item;
            final String _tmpExtension;
            _tmpExtension = _cursor.getString(_cursorIndexOfExtension);
            _item = new IgnoredExtensionEntity(_tmpExtension);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getAllOnce(final Continuation<? super List<IgnoredExtensionEntity>> $completion) {
    final String _sql = "SELECT * FROM ignored_extensions";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<IgnoredExtensionEntity>>() {
      @Override
      @NonNull
      public List<IgnoredExtensionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfExtension = CursorUtil.getColumnIndexOrThrow(_cursor, "extension");
          final List<IgnoredExtensionEntity> _result = new ArrayList<IgnoredExtensionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final IgnoredExtensionEntity _item;
            final String _tmpExtension;
            _tmpExtension = _cursor.getString(_cursorIndexOfExtension);
            _item = new IgnoredExtensionEntity(_tmpExtension);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
