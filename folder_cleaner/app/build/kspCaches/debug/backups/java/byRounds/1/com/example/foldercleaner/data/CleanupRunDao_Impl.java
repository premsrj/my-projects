package com.example.foldercleaner.data;

import android.database.Cursor;
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
public final class CleanupRunDao_Impl implements CleanupRunDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CleanupRunEntity> __insertionAdapterOfCleanupRunEntity;

  private final SharedSQLiteStatement __preparedStmtOfPruneToLatest;

  public CleanupRunDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCleanupRunEntity = new EntityInsertionAdapter<CleanupRunEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `cleanup_runs` (`id`,`executedAtMillis`,`trigger`,`scannedCount`,`deletedCount`,`skippedCount`,`failedCount`,`reclaimedBytes`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CleanupRunEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getExecutedAtMillis());
        statement.bindString(3, entity.getTrigger());
        statement.bindLong(4, entity.getScannedCount());
        statement.bindLong(5, entity.getDeletedCount());
        statement.bindLong(6, entity.getSkippedCount());
        statement.bindLong(7, entity.getFailedCount());
        statement.bindLong(8, entity.getReclaimedBytes());
      }
    };
    this.__preparedStmtOfPruneToLatest = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "\n"
                + "        DELETE FROM cleanup_runs\n"
                + "        WHERE id NOT IN (\n"
                + "            SELECT id FROM cleanup_runs\n"
                + "            ORDER BY executedAtMillis DESC, id DESC\n"
                + "            LIMIT ?\n"
                + "        )\n"
                + "        ";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final CleanupRunEntity run, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCleanupRunEntity.insert(run);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object pruneToLatest(final int maxEntries, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfPruneToLatest.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, maxEntries);
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
          __preparedStmtOfPruneToLatest.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<CleanupRunEntity>> observeAll() {
    final String _sql = "SELECT * FROM cleanup_runs ORDER BY executedAtMillis DESC, id DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"cleanup_runs"}, new Callable<List<CleanupRunEntity>>() {
      @Override
      @NonNull
      public List<CleanupRunEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfExecutedAtMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "executedAtMillis");
          final int _cursorIndexOfTrigger = CursorUtil.getColumnIndexOrThrow(_cursor, "trigger");
          final int _cursorIndexOfScannedCount = CursorUtil.getColumnIndexOrThrow(_cursor, "scannedCount");
          final int _cursorIndexOfDeletedCount = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedCount");
          final int _cursorIndexOfSkippedCount = CursorUtil.getColumnIndexOrThrow(_cursor, "skippedCount");
          final int _cursorIndexOfFailedCount = CursorUtil.getColumnIndexOrThrow(_cursor, "failedCount");
          final int _cursorIndexOfReclaimedBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "reclaimedBytes");
          final List<CleanupRunEntity> _result = new ArrayList<CleanupRunEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CleanupRunEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpExecutedAtMillis;
            _tmpExecutedAtMillis = _cursor.getLong(_cursorIndexOfExecutedAtMillis);
            final String _tmpTrigger;
            _tmpTrigger = _cursor.getString(_cursorIndexOfTrigger);
            final int _tmpScannedCount;
            _tmpScannedCount = _cursor.getInt(_cursorIndexOfScannedCount);
            final int _tmpDeletedCount;
            _tmpDeletedCount = _cursor.getInt(_cursorIndexOfDeletedCount);
            final int _tmpSkippedCount;
            _tmpSkippedCount = _cursor.getInt(_cursorIndexOfSkippedCount);
            final int _tmpFailedCount;
            _tmpFailedCount = _cursor.getInt(_cursorIndexOfFailedCount);
            final long _tmpReclaimedBytes;
            _tmpReclaimedBytes = _cursor.getLong(_cursorIndexOfReclaimedBytes);
            _item = new CleanupRunEntity(_tmpId,_tmpExecutedAtMillis,_tmpTrigger,_tmpScannedCount,_tmpDeletedCount,_tmpSkippedCount,_tmpFailedCount,_tmpReclaimedBytes);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
