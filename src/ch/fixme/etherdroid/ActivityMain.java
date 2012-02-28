package ch.fixme.etherdroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class ActivityMain extends Activity {

    private static final int DIALOG_LOADING = 1;
    private static final int DIALOG_ADD = 2;
    private static final String QUERY_READ = "SELECT _id,host,port from hosts";
    private static final String QUERY_ADD = "INSERT INTO hosts (host, port, apikey) values (?,?,?)";
    private SimpleCursorAdapter mAdapter;
    private Context mContext;
    private SQLiteDatabase mDb;
    private Cursor mCursor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mContext = getApplicationContext();
        mAdapter = new SimpleCursorAdapter(mContext, R.layout.list_host, null,
                new String[] { "host" }, new int[] { R.id.list_title });
        ListView list = (ListView) findViewById(R.id.list_hosts);
        list.setAdapter(mAdapter);
        list.setEmptyView(findViewById(R.id.empty));
        list.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // List saved pads from this host
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.putExtra("hostid", id);
                i.setClass(ActivityMain.this, ActivityPads.class);
                startActivity(i);
            }
        });

        mDb = new Database(mContext).getWritableDatabase();
        new ListTask().execute();
        // FIXME: Just for dev -> open reader
        // Intent i = new Intent(Intent.ACTION_VIEW);
        // i.setData(Uri.parse("pad://62.220.136.218:9001/BFrMshLVWcrG4B6BsFeDRk1Iritq2Dfz/GADC2012"));
        // startActivity(i);
        // finish();
    }

    @Override
    public void onDestroy() {
        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
        if (mDb != null && mDb.isOpen()) {
            mDb.close();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_add:
            showDialog(DIALOG_ADD);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        final AlertDialog dialog;
        switch (id) {
        case DIALOG_LOADING:
            dialog = new ProgressDialog(this);
            dialog.setCancelable(true);
            dialog.setMessage("Loading...");
            ((ProgressDialog) dialog).setIndeterminate(true);
            break;
        case DIALOG_ADD:
            AlertDialog.Builder b1 = new AlertDialog.Builder(this);
            b1.setTitle("Add host");
            // Custom view
            View layout = getLayoutInflater().inflate(R.layout.dialog_add_host, null);
            b1.setView(layout);
            // Buttons actions
            final EditText host = (EditText) layout.findViewById(R.id.add_host);
            final EditText port = (EditText) layout.findViewById(R.id.add_port);
            final EditText key = (EditText) layout.findViewById(R.id.add_apikey);
            b1.setPositiveButton("Add", null);
            b1.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            host.addTextChangedListener(new TextWatcher() {
                public void afterTextChanged(Editable s) {
                }

                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    validateAdd(host, null, null);
                }
            });
            port.addTextChangedListener(new TextWatcher() {
                public void afterTextChanged(Editable s) {
                }

                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    validateAdd(null, port, null);
                }
            });
            key.addTextChangedListener(new TextWatcher() {
                public void afterTextChanged(Editable s) {
                }

                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    validateAdd(null, null, key);
                }
            });
            // Creation and Listeners
            dialog = b1.create();
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    host.setText("");
                    port.setText("");
                    key.setText("");
                    host.setError(null);
                    port.setError(null);
                    key.setError(null);
                }
            });
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                public void onShow(DialogInterface d) {
                    Button button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                    button.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            if (validateAdd(host, port, key)) {
                                new AddHost().execute(host.getText().toString(), port.getText()
                                        .toString(), key.getText().toString());
                                dialog.cancel();
                            }
                        }
                    });
                }
            });
            break;
        default:
            dialog = null;
        }
        return dialog;
    }

    private boolean validateAdd(EditText host, EditText port, EditText key) {
        boolean ok = true;
        if (host != null && host.getText().toString().length() == 0) {
            host.setError("You must Provide an host address");
            ok = false;
        }
        if (port != null && port.getText().toString().length() == 0) {
            port.setError("You must Provide a port");
            ok = false;
        }
        if (key != null && key.getText().toString().length() == 0) {
            key.setError("You must Provide an API key");
            ok = false;
        }
        return ok;
    }

    private class ListTask extends AsyncTask<Void, Void, Cursor> {

        @Override
        protected Cursor doInBackground(Void... unused) {
            return mDb.rawQuery(QUERY_READ, null);
        }

        @Override
        protected void onPostExecute(Cursor c) {
            mCursor = c;
            mAdapter.changeCursor(mCursor);
        }
    }

    private class AddHost extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... data) {
            mDb.execSQL(QUERY_ADD, data);
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            mCursor.requery();
            mAdapter.notifyDataSetChanged();
            Toast.makeText(mContext, "Host successfully added!", Toast.LENGTH_SHORT).show();
        }
    }
}
