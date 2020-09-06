package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
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

public class sign extends Activity {
    String names, company, ids, code;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        SharedPreferences prefs = getSharedPreferences("finger486", MODE_PRIVATE);

        // 회원가입 버튼 이벤트
        names = prefs.getString("name", "");
        ids = prefs.getString("id", "");
        code = prefs.getString("code", "");
        company = prefs.getString("company", "");

        try {
            new send_db().execute();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        back_login(null);
    }

    private class send_db extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            InputStream is = null;
            String parse_data = "";
            String json_data = "";
            StringBuffer buffer;
            try {
                // Load CAs from an InputStream
                // (could be from a resource or ByteArrayInputStream or ...)
                CertificateFactory cf = CertificateFactory.getInstance("X.509");

                AssetManager am = getResources().getAssets();
                InputStream caInput = am.open("a.pem", AssetManager.ACCESS_BUFFER);

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

                // Create an SSLContext that uses our TrustManager
                //SSLContext context = SSLContext.getInstance("TLS");
                //context.init(null, tmf.getTrustManagers(), null);

                trustAllHosts();
                // Tell the URLConnection to use a SocketFactory from our SSLContext
                String server_url = "https://fidochallenge486.tk:9001/useraccount/program_signup/";
                //String server_url = "https://fidochallenge486.tk:9001/useraccount/app_test/";

                URL url = new URL(server_url);

                JSONObject jsonObject = new JSONObject();

                jsonObject.accumulate("id", ids);
                jsonObject.accumulate("code", code);
                jsonObject.accumulate("name", names);
                jsonObject.accumulate("company", company);

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

                if (response.get("Check").toString().equals("1")) {
                    return "succ";
                }
                else
                {
                    return "fail";
                }
            } catch (JSONException e) {
                Log.i("sign activity", "Json ERROR");
            } catch (MalformedURLException e) {
                Log.i("sign activity", "MalformedURL");
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (CertificateException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (KeyStoreException e) {
                e.printStackTrace();
            }
            return "";
        }

        @Override
        protected void onPostExecute(String res) {

            if(res.equals("succ")) {
                String real = "https://fidochallenge486.tk:8080/test/" + ids + "&" + code;
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(real));
                startActivity(intent);
                finish();
            }
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

        public void back_login(View view) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }
