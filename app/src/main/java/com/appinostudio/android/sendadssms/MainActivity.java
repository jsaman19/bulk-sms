package com.appinostudio.android.sendadssms;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.Toast;

import com.appinostudio.android.sendadssms.models.User;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {

    private LayoutInflater inflater;
    private LinearLayout keyValuesParentLyt;
    private int counter = 0;
    private List<EditText> keyEditTexts = new ArrayList<>();
    private List<EditText> valueEditTexts = new ArrayList<>();
    private EditText apiUrlTxt, patternTxt;
    boolean is_dual_sim;
    private AsyncHttpClient client = new AsyncHttpClient();
    private List<User> users = new ArrayList<>();
    int selectedSim = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        is_dual_sim = getSubscriptionInfo();
        inflater = LayoutInflater.from(this);
        keyValuesParentLyt = findViewById(R.id.key_values_parent);
        Button sendSim1Btn = findViewById(R.id.send_btn_sim1);
        Button sendSim2Btn = findViewById(R.id.send_btn_sim2);
        Space sendBtnDivider = findViewById(R.id.send_btn_divider);
        apiUrlTxt = findViewById(R.id.url_txt);
        patternTxt = findViewById(R.id.pattern_txt);
        client.addHeader("Authorization", "");

        addNewKeyValueLayout();

        if (!is_dual_sim) {
            sendSim1Btn.setText("Send");
            sendSim2Btn.setVisibility(View.GONE);
            sendBtnDivider.setVisibility(View.GONE);
        }
    }

    public void addAnOtherKeyValueLyt(View view) {
        addNewKeyValueLayout();
    }

    private void addNewKeyValueLayout() {
        LinearLayout keyValueLyt = (LinearLayout) inflater.inflate(R.layout.key_value_lyt, null, false);
        EditText keyTxt = (EditText) keyValueLyt.findViewById(R.id.key_txt);
        EditText valueTxt = (EditText) keyValueLyt.findViewById(R.id.value_txt);

        keyTxt.setId(keyTxt.getId() + counter);
        valueTxt.setId(keyTxt.getId() + counter);

        keyEditTexts.add(keyTxt);
        valueEditTexts.add(valueTxt);

        keyValuesParentLyt.addView(keyValueLyt);
        counter++;
    }

    public void sendWithSim1(View view) {
        selectedSim = 0;
        fetchData();
    }

    public void sendWithSim2(View view) {
        selectedSim = 1;
        fetchData();
    }

    private String getMessage(User user) {
        String pattern = patternTxt.getText().toString();

        for (int i = 0; i < keyEditTexts.size(); i++) {
            EditText keyEditText = keyEditTexts.get(i);
            String key = keyEditText.getText().toString();
            String value = valueEditTexts.get(i).getText().toString();
            String old_txt = "%" + key + "%";
            String new_txt = user.getByValue(value);
            pattern = pattern.replace(old_txt.trim(), new_txt);
        }

        return pattern;
    }

    private void sendSMS() {
        if (patternTxt.getText().toString().isEmpty()) {
            Toast.makeText(MainActivity.this, "Please Enter Your Pattern", Toast.LENGTH_LONG).show();
            return;
        }
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setTitle("Sending Sms");
        dialog.setProgressStyle(dialog.STYLE_HORIZONTAL);
        dialog.setProgress(0);
        dialog.setMax(users.size());
        dialog.show();

        final int[] counter = {1};


        new Thread(new Runnable(){
            public void run(){
                try{
                    for (User user :
                            users) {
                        Thread.sleep(1000);
                        String smsText = getMessage(user);
                        sendSmsMsgFnc(user.getMobile(), smsText, selectedSim);
                        dialog.setProgress(counter[0]);
                        counter[0]++;
                    }
                    dialog.dismiss();
                }
                catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }

        }).start();
    }

    void sendSmsMsgFnc(String mblNumVar, String smsMsgVar, int sim_id) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                SmsManager.getSmsManagerForSubscriptionId(sim_id).sendTextMessage(mblNumVar, null, smsMsgVar, null, null);
                Toast.makeText(getApplicationContext(), "Message Sent",
                        Toast.LENGTH_LONG).show();
            } catch (Exception ErrVar) {
                Toast.makeText(getApplicationContext(), ErrVar.getMessage().toString(),
                        Toast.LENGTH_LONG).show();
                ErrVar.printStackTrace();
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.SEND_SMS}, 10);
            }
        }

    }

    private boolean getSubscriptionInfo() {
        SubscriptionManager subscriptionManager = (SubscriptionManager) getSystemService(this.TELEPHONY_SUBSCRIPTION_SERVICE);
        return subscriptionManager.getActiveSubscriptionInfoCountMax() > 1;
    }

    private void fetchData() {
        if (apiUrlTxt.getText().toString().isEmpty()) {
            Toast.makeText(MainActivity.this, "Please Enter Your Api Url", Toast.LENGTH_LONG).show();
            return;
        }
        ProgressDialog dialog = ProgressDialog.show(this, "FetchData", "Loading. Please Wait...");
        client.post(apiUrlTxt.getText().toString(), new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                dialog.dismiss();
                try {
                    JSONArray data = response.getJSONArray("data");
                    users.clear();
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject userObj = data.getJSONObject(i);
                        User user = new User();
                        user.setEmail(userObj.getString("email"));
                        user.setMobile(userObj.getString("mobile"));
                        user.setF_name(userObj.getString("f_name"));
                        user.setL_name(userObj.getString("l_name"));

                        users.add(user);
                    }

                    Toast.makeText(MainActivity.this, "Fetched " + String.valueOf(users.size()) + " Number for Send Sms", Toast.LENGTH_LONG).show();
                    sendSMS();
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Pars Response Failed", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                dialog.dismiss();
                Toast.makeText(MainActivity.this, "Connection Failed", Toast.LENGTH_LONG).show();
            }
        });
    }
}