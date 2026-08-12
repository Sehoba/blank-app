package com.sehoba.antigravitymobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final String PREFS = "antigravity_mobile_prefs";
    private static final String PREF_HISTORY = "history";
    private static final int BG = Color.rgb(11,16,32), SURFACE = Color.rgb(21,27,46), SURFACE_2 = Color.rgb(32,41,66), TEXT = Color.rgb(246,247,251), MUTED = Color.rgb(185,194,216), ACCENT = Color.rgb(145,167,255);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private SharedPreferences prefs;
    private EditText apiKeyEdit, endpointIdEdit, systemEdit, messageEdit;
    private Spinner backendSpinner;
    private TextView statusView, chatView;
    private ScrollView chatScroll;
    private Button sendButton;
    private boolean requestRunning;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        setContentView(buildUi()); restoreState();
    }
    @Override protected void onDestroy() { executor.shutdownNow(); super.onDestroy(); }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG); root.setPadding(dp(16),dp(14),dp(16),dp(14));
        TextView title = text("✦ ANTIGRAVITY MOBILE",24,TEXT,true); title.setLetterSpacing(.04f); root.addView(title, matchWrap());
        TextView subtitle = text("Native Android Client • Gemini Interactions API",13,MUTED,false); subtitle.setPadding(0,dp(2),0,dp(10)); root.addView(subtitle,matchWrap());
        statusView = text("Initialisierung …",13,ACCENT,true); statusView.setPadding(dp(12),dp(9),dp(12),dp(9)); statusView.setBackgroundColor(SURFACE_2); root.addView(statusView,matchWrapWithBottom(dp(10)));
        LinearLayout settings = new LinearLayout(this); settings.setOrientation(LinearLayout.VERTICAL); settings.setPadding(dp(12),dp(12),dp(12),dp(12)); settings.setBackgroundColor(SURFACE);
        settings.addView(label("API-Key (Android Keystore-verschlüsselt)")); apiKeyEdit = edit("Gemini API-Key",false); apiKeyEdit.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD); apiKeyEdit.setTransformationMethod(PasswordTransformationMethod.getInstance()); settings.addView(apiKeyEdit,matchWrapWithBottom(dp(8)));
        settings.addView(label("Backend")); backendSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,new String[]{"Gemini 3.6 Flash","Antigravity Managed Agent"}) {
            @Override public View getView(int p, View c, ViewGroup parent) { View v=super.getView(p,c,parent); if(v instanceof TextView)((TextView)v).setTextColor(TEXT); return v; }
            @Override public View getDropDownView(int p, View c, ViewGroup parent) { View v=super.getDropDownView(p,c,parent); if(v instanceof TextView){((TextView)v).setTextColor(Color.BLACK);((TextView)v).setPadding(dp(12),dp(12),dp(12),dp(12));} return v; }
        }; adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); backendSpinner.setAdapter(adapter); settings.addView(backendSpinner,matchWrapWithBottom(dp(8)));
        settings.addView(label("Modell / Agent-ID")); endpointIdEdit=edit("gemini-3.6-flash",false); endpointIdEdit.setText("gemini-3.6-flash"); settings.addView(endpointIdEdit,matchWrapWithBottom(dp(8)));
        backendSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){ public void onItemSelected(AdapterView<?> p,View v,int pos,long id){endpointIdEdit.setText(pos==1?"antigravity-preview-05-2026":"gemini-3.6-flash");} public void onNothingSelected(AdapterView<?> p){} });
        settings.addView(label("System-Instruktion")); systemEdit=edit("Verhalten des Agenten",true); systemEdit.setText("Du bist Antigravity Mobile, ein präziser technischer Assistent. Antworte auf Deutsch, arbeite nachvollziehbar und erfinde keine ausgeführten Aktionen."); systemEdit.setMinLines(2); systemEdit.setMaxLines(4); settings.addView(systemEdit,matchWrapWithBottom(dp(8)));
        LinearLayout r1=new LinearLayout(this);r1.setOrientation(LinearLayout.HORIZONTAL); Button save=button("KEY SPEICHERN"),copy=button("CHAT KOPIEREN"); r1.addView(save,weightWrap(1));r1.addView(copy,weightWrap(1));settings.addView(r1,matchWrap());
        LinearLayout r2=new LinearLayout(this);r2.setOrientation(LinearLayout.HORIZONTAL); Button clear=button("LÖSCHEN"),licenses=button("LIZENZEN");r2.addView(clear,weightWrap(1));r2.addView(licenses,weightWrap(1));settings.addView(r2,matchWrap());
        save.setOnClickListener(v->saveKey());copy.setOnClickListener(v->copyChat());clear.setOnClickListener(v->clearChat());licenses.setOnClickListener(v->showLicenses()); root.addView(settings,matchWrapWithBottom(dp(10)));
        chatScroll=new ScrollView(this);chatScroll.setFillViewport(true);chatScroll.setBackgroundColor(SURFACE);chatView=text("",15,TEXT,false);chatView.setTextIsSelectable(true);chatView.setPadding(dp(12),dp(12),dp(12),dp(12));chatView.setLineSpacing(0f,1.12f);chatScroll.addView(chatView,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT)); LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f);cp.bottomMargin=dp(10);root.addView(chatScroll,cp);
        LinearLayout composer=new LinearLayout(this);composer.setOrientation(LinearLayout.HORIZONTAL);composer.setGravity(Gravity.BOTTOM);messageEdit=edit("Nachricht an Antigravity …",true);messageEdit.setMinLines(2);messageEdit.setMaxLines(5);LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f);ip.rightMargin=dp(8);composer.addView(messageEdit,ip);sendButton=button("SENDEN");sendButton.setMinHeight(dp(52));sendButton.setOnClickListener(v->sendMessage());composer.addView(sendButton,new LinearLayout.LayoutParams(dp(100),ViewGroup.LayoutParams.WRAP_CONTENT));root.addView(composer,matchWrap()); return root;
    }

    private void restoreState(){String key=SecureStore.loadApiKey(this);apiKeyEdit.setText(key);String h=prefs.getString(PREF_HISTORY,"");if(h==null||h.trim().isEmpty())h="ANTIGRAVITY MOBILE 1.0\nBereit. Hinterlege deinen Gemini API-Key und starte den Chat.";chatView.setText(h);updateStatus(key.isEmpty()?"Bereit • API-Key fehlt":"Bereit • API-Key sicher geladen");scrollToBottom();}
    private void saveKey(){try{String key=apiKeyEdit.getText().toString().trim();SecureStore.saveApiKey(this,key);updateStatus(key.isEmpty()?"API-Key entfernt":"API-Key verschlüsselt gespeichert");toast(key.isEmpty()?"API-Key entfernt":"API-Key gespeichert");}catch(Exception e){updateStatus("Keystore-Fehler: "+safeMessage(e));}}
    private void sendMessage(){if(requestRunning)return;final String key=apiKeyEdit.getText().toString().trim(),prompt=messageEdit.getText().toString().trim(),id=endpointIdEdit.getText().toString().trim(),system=systemEdit.getText().toString().trim();final boolean agentMode=backendSpinner.getSelectedItemPosition()==1;if(key.isEmpty()){toast("Zuerst API-Key eintragen.");apiKeyEdit.requestFocus();return;}if(prompt.isEmpty())return;if(id.isEmpty()){toast("Modell-/Agent-ID fehlt.");return;}try{SecureStore.saveApiKey(this,key);}catch(Exception e){toast("API-Key konnte nicht sicher gespeichert werden.");return;}final String prior=chatView.getText().toString();final String input=prior.trim().isEmpty()?prompt:"Lokaler Gesprächskontext:\n"+trimContext(prior)+"\n\nNeue Nutzernachricht:\n"+prompt;appendChat("\n\nDU:\n"+prompt+"\n\nAI:\n");messageEdit.setText("");setRunning(true);updateStatus((agentMode?"Antigravity Agent":id)+" • Anfrage läuft …");StringBuilder out=new StringBuilder();executor.execute(()->GeminiClient.streamInteraction(key,agentMode,id,system,input,new GeminiClient.Listener(){public void onDelta(String t){out.append(t);runOnUiThread(()->appendChat(t));}public void onComplete(){runOnUiThread(()->{if(out.length()==0)appendChat("[Keine Textausgabe]");appendChat("\n");persistHistory();setRunning(false);updateStatus("Bereit • letzte Anfrage erfolgreich");});}public void onError(String m){runOnUiThread(()->{appendChat("\n[FEHLER] "+m+"\n");persistHistory();setRunning(false);updateStatus("Fehler • "+shorten(m,100));});}}));}
    private void clearChat(){if(requestRunning){toast("Während einer laufenden Anfrage nicht löschen.");return;}chatView.setText("ANTIGRAVITY MOBILE 1.0\nNeuer lokaler Chat.");persistHistory();updateStatus("Chat lokal gelöscht");}
    private void copyChat(){ClipboardManager c=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);c.setPrimaryClip(ClipData.newPlainText("Antigravity Chat",chatView.getText()));toast("Chat kopiert");}
    private void showLicenses(){new AlertDialog.Builder(this).setTitle("Lizenzen & Architektur").setMessage("Antigravity Mobile 1.0\n\nNative Android-Neuimplementierung, inspiriert von google-antigravity 0.1.10 (Apache License 2.0).\n\nDiese APK bindet weder das Desktop-localharness-Binary noch eine Python-Runtime ein. Sie spricht die Gemini Interactions API direkt über HTTPS an.\n\nAPI-Key: AES/GCM-Schlüssel im Android Keystore; verschlüsselter Ciphertext in privaten App-Preferences.").setPositiveButton("OK",null).show();}
    private void appendChat(String t){chatView.append(t);scrollToBottom();} private void persistHistory(){prefs.edit().putString(PREF_HISTORY,chatView.getText().toString()).apply();} private void scrollToBottom(){chatScroll.post(()->chatScroll.fullScroll(View.FOCUS_DOWN));}
    private void setRunning(boolean r){requestRunning=r;sendButton.setEnabled(!r);sendButton.setText(r?"LÄUFT…":"SENDEN");messageEdit.setEnabled(!r);backendSpinner.setEnabled(!r);endpointIdEdit.setEnabled(!r);} private void updateStatus(String t){statusView.setText(t);} private TextView label(String v){TextView t=text(v,12,MUTED,true);t.setPadding(0,dp(3),0,dp(4));return t;} private TextView text(String v,int sp,int c,boolean b){TextView t=new TextView(this);t.setText(v);t.setTextSize(sp);t.setTextColor(c);if(b)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private EditText edit(String hint,boolean multi){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(MUTED);e.setTextColor(TEXT);e.setTextSize(14);e.setBackgroundColor(SURFACE_2);e.setPadding(dp(10),dp(9),dp(10),dp(9));if(multi){e.setSingleLine(false);e.setGravity(Gravity.TOP|Gravity.START);e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);}else e.setSingleLine(true);return e;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(11);b.setTextColor(Color.rgb(15,20,35));b.setAllCaps(false);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);if(b.getBackground()!=null)b.getBackground().setTint(ACCENT);return b;} private LinearLayout.LayoutParams matchWrap(){return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);} private LinearLayout.LayoutParams matchWrapWithBottom(int b){LinearLayout.LayoutParams p=matchWrap();p.bottomMargin=b;return p;} private LinearLayout.LayoutParams weightWrap(float w){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,w);p.setMargins(dp(2),dp(2),dp(2),dp(2));return p;} private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);} private void toast(String v){Toast.makeText(this,v,Toast.LENGTH_SHORT).show();}
    private static String trimContext(String v){int m=12000;return v.length()<=m?v:"[…]\n"+v.substring(v.length()-m);} private static String shorten(String v,int m){if(v==null)return "Unbekannter Fehler";return v.length()<=m?v:v.substring(0,m)+"…";} private static String safeMessage(Throwable t){String m=t.getMessage();return m==null?t.getClass().getSimpleName():m;}
}
