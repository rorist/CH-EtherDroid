package ch.fixme.etherdroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

public class ActivityReader extends Activity {

    private EtherAPI mApi;
    private static final int DIALOG_LOADING = 1;
    private static final int DIALOG_ERROR = 2;

    private Response mLastResponse;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reader);
        // Vertical scroll
        ((TextView) findViewById(R.id.padText)).setMovementMethod(ScrollingMovementMethod
                .getInstance());
        // Get pad
        mApi = new EtherAPI("http://62.220.136.218:9001", "BFrMshLVWcrG4B6BsFeDRk1Iritq2Dfz");
        new GetTextTask().execute("GADC2012");
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
            case R.id.menu_refresh:
                new GetTextTask().execute("GADC2012");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog dialog = null;
        switch (id) {
            case DIALOG_LOADING:
                dialog = new ProgressDialog(this);
                dialog.setCancelable(false);
                dialog.setMessage("Loading...");
                ((ProgressDialog) dialog).setIndeterminate(true);
                break;
            case DIALOG_ERROR:
                if (mLastResponse != null) {
                    dialog = new AlertDialog.Builder(this).setMessage(mLastResponse.message)
                            .setNeutralButton("Ok", null).create();
                }
                break;
        }
        return dialog;
    }

    private void handleResponse(Response res) {
        mLastResponse = res;
        TextView txt = (TextView) findViewById(R.id.padText);
        switch (res.code) {
            case Response.CODE_OK:
                if (res.data.containsKey("text")) {
                    txt.setText(res.data.get("text"));
                }
                break;
            default:
                showDialog(DIALOG_ERROR);
                break;
        }
    }

    private class GetTextTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            showDialog(DIALOG_LOADING);
        }

        @Override
        protected String doInBackground(String... ids) {
            return mApi.getText(ids[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            handleResponse(new Response(result));
            dismissDialog(DIALOG_LOADING);
        }

    }
}