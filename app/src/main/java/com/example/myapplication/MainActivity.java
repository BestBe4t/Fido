package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class MainActivity extends AppCompatActivity {
    String s_name = "";
    String s_code = "";
    String token = "";          //로그인 성공시 보존할 JWT 토큰
    String user = "";           //로그인 성공시 보존할 사용자 이름
    EditText name = null;
    EditText code_edit = null;
    Button login_button;
    IntentIntegrator qrScan;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        name = (EditText) findViewById(R.id.user_id);
        code_edit = (EditText) findViewById(R.id.code_edit);

        prefs = getSharedPreferences("finger486", MODE_PRIVATE);
        s_name = prefs.getString("name", "");
        s_code = prefs.getString("code", "");

        name.setText(s_name);
        code_edit.setText(s_code);
        Log.d("auto login" ,"id : " + s_name + " code :" + s_code );
        login_button = (Button) findViewById(R.id.login_button);

        login_button.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                new login_data_paser().execute();
            }
        });

        Button scan_button = (Button) findViewById(R.id.scan);

        scan_button.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                qrScan = new IntentIntegrator(MainActivity.this);
                qrScan.setPrompt("Scanning...");
                qrScan.initiateScan();
            }
        });
    }

    @Override
    protected void onResume() {
        name= (EditText) findViewById(R.id.user_id);
        s_name = prefs.getString("name", "");
        s_code = prefs.getString("code", "");
        if (s_name != null)
            name.setText(s_name);
        super.onResume();
    }

    public class login_data_paser extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
        }

        @Override
        protected String doInBackground(String... strings) {
            InputStream is = null;
            String parse_data = "";
            String json_data = "";
            StringBuffer buffer;
            String name_str = null;
            String code_str = null;
             try {
                 // Load CAs from an InputStream
                 // (could be from a resource or ByteArrayInputStream or ...)
                 CertificateFactory cf = CertificateFactory.getInstance("X.509");
                 // From https://www.washington.edu/itconnect/security/ca/load-der.crt

                 AssetManager am = getResources().getAssets();
                 InputStream caInput = am.open("a.pem",AssetManager.ACCESS_BUFFER);

                 Certificate ca;
                 try {
                     ca = cf.generateCertificate(caInput);
                     System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
                 } finally {
                     caInput.close();
                 }

                 // Create a KeyStore containing our trusted CAs
                 String keyStoreType = KeyStore.getDefaultType();
                 KeyStore keyStore = KeyStore.getInstance(keyStoreType);
                 keyStore.load(null, null);
                 keyStore.setCertificateEntry("ca", ca);

                 // Create a TrustManager that trusts the CAs in our KeyStore
                 String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
                 TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
                 tmf.init(keyStore);

                 trustAllHosts();
                 // Tell the URLConnection to use a SocketFactory from our SSLContext
                 String server_url = "https://fidochallenge486.tk:9001/useraccount/program_login/";
                 //String server_url = "https://fidochallenge486.tk:9001/useraccount/app_test/";

                 URL url = new URL(server_url);

                 if(TextUtils.isEmpty(s_name) && TextUtils.isEmpty(s_code)) {
                     name = (EditText) findViewById(R.id.user_id);
                     code_edit = (EditText) findViewById(R.id.code_edit);

                     name_str = name.getText().toString();
                     code_str = code_edit.getText().toString();
                 }else{
                     name_str = s_name;
                     code_str = s_code;
                 }

                 JSONObject jsonObject = new JSONObject();

                 jsonObject.accumulate("name", name_str);
                 jsonObject.accumulate("code", code_str);

                 Log.d("Login", name_str + ":" + code_str);

                 String json = jsonObject.toString();

                 HttpsURLConnection urlcon = (HttpsURLConnection) url.openConnection();
                 urlcon.setHostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String s, SSLSession sslSession) {
                        return true;
                     }
                });

                 urlcon.setDoOutput(true);
                 urlcon.setDoInput(true);

                 urlcon.setConnectTimeout(8000);
                 urlcon.setReadTimeout(8000);
                 urlcon.setRequestProperty("Accept", "application/json");
                 urlcon.setRequestProperty("X-Environment", "android");
                 OutputStream os = urlcon.getOutputStream();
                 os.write(json.getBytes("euc-kr"));
                 os.flush();

                 is = urlcon.getInputStream();   // 데이터 전송

                 BufferedReader rd = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                 buffer = new StringBuffer();
                 while ((parse_data = rd.readLine()) != null) {
                     buffer.append(parse_data);
                 }
                 json_data = buffer.toString();
                 JSONObject response = new JSONObject(json_data);

                 if (response.get("name").toString().equals(s_name)) {
                     SharedPreferences.Editor edit = prefs.edit();
                     token = response.get("token").toString();
                     edit.putString("token", token);
                     edit.apply();

                     Log.d("MainActivity.java", "Login Success" + user + ":" + token);
                     return "succ";
                 } else {
                     Log.d("MainActivity.java", "Login Fail" + s_name + ":" + s_code);
                     //Toast.makeText(MainActivity.this, "Login Fail", Toast.LENGTH_SHORT).show();
                 }

             }catch (IOException e){
                    e.printStackTrace();
                    Log.i("MainActivity.java", "IO Exception");
                 }
                 catch (JSONException e) {
                    e.printStackTrace();
                    Log.i("MainActivity.java", "jSON Exception");
                 } catch (Exception e) {
                 e.printStackTrace();
                 Log.i("MainActivity.java", "Sever is deny");
             }

             return "fail";
        }

        @Override
        protected void onPostExecute(String res) {
            if (res.equals("succ"))
            {
                Intent intent = new Intent(getApplicationContext(), authview.class);
                startActivity(intent);
                finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        Log.d("Mainactivity", "activityresult start");

        if(result != null){
            if (result.getContents() == null){
                Toast.makeText(MainActivity.this, "스캔 취소", Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(MainActivity.this, "스캔 완료", Toast.LENGTH_SHORT).show();
            }
            try {
                String obj = result.getContents();
                String[] contents = obj.split("/");
                String user_name = contents[0];
                String user_code = contents[1];
                String user_id = contents[2];
                String user_company = contents[3];

                SharedPreferences.Editor edit = prefs.edit();

                edit.putString("name", user_name);
                edit.putString("code", user_code);
                edit.putString("id", user_id);
                edit.putString("company", user_company);
                edit.apply();

                Log.d("Mainactivity", "name: "+user_name+", code: "+user_code+", id: "+user_id+", company: "+user_company);

                sign_upmove(null);
            }catch (Exception e){
                Log.i("Mainactivity", "Fail");
            }
        }else {
            Log.d("Mainactivity", "fail");
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
    private static void trustAllHosts() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {

            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[]{};
            }
            @Override
            public void checkClientTrusted(
                    java.security.cert.X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
                // TODO Auto-generated method stub
            }
            @Override
            public void checkServerTrusted(
                    java.security.cert.X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
                // TODO Auto-generated method stub
            }
        }};
        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sign_upmove(View view)
    {
        Intent intent = new Intent(this, sign.class);
        startActivity(intent);
        finish();
    }
}



