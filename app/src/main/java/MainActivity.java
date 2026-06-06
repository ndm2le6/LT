package com.example.ironcoach;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Base64;
import android.view.*;
import android.view.animation.*;
import android.widget.*;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import org.json.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    final int BG=Color.rgb(11,15,20), CARD=Color.rgb(21,26,33), GREEN=Color.rgb(0,200,90), TXT=Color.WHITE, MUTED=Color.rgb(160,167,180);
    LinearLayout root; SharedPreferences sp; Handler h=new Handler();
    String apiKey="", model="gemini-2.5-flash"; int dayCount=0; JSONArray sessions=new JSONArray();
    JSONObject currentPlan; int exIndex=0; CountDownTimer timer; long remaining=0; Uri pendingPhotoUri; String pendingPhotoPath="";

    @Override public void onCreate(Bundle b){ super.onCreate(b); sp=getSharedPreferences("iron",MODE_PRIVATE); load(); showLanding(); }
    void load(){ apiKey=sp.getString("apiKey",""); model=sp.getString("model","gemini-2.5-flash"); dayCount=sp.getInt("dayCount",0); try{sessions=new JSONArray(sp.getString("sessions","[]"));}catch(Exception e){sessions=new JSONArray();}}
    void save(){ sp.edit().putString("apiKey",apiKey).putString("model",model).putInt("dayCount",dayCount).putString("sessions",sessions.toString()).apply(); }
    TextView tv(String t,int spSize,int color,int style){ TextView v=new TextView(this); v.setText(t); v.setTextSize(spSize); v.setTextColor(color); v.setTypeface(Typeface.DEFAULT,style); v.setPadding(dp(12),dp(8),dp(12),dp(8)); return v; }
    Button btn(String t,int color){ Button b=new Button(this); b.setText(t); b.setTextColor(Color.WHITE); b.setTextSize(16); b.setTypeface(Typeface.DEFAULT,Typeface.BOLD); b.setBackgroundColor(color); b.setAllCaps(false); b.setPadding(dp(12),dp(10),dp(12),dp(10)); return b; }
    EditText input(String hint){ EditText e=new EditText(this); e.setHint(hint); e.setHintTextColor(MUTED); e.setTextColor(TXT); e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL); e.setBackgroundColor(CARD); e.setPadding(dp(14),dp(12),dp(14),dp(12)); return e; }
    void base(){ root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER); root.setBackgroundColor(BG); setContentView(root); }
    int dp(int v){ return (int)(v*getResources().getDisplayMetrics().density+0.5f); }

    void showLanding(){ base(); root.setPadding(dp(24),0,dp(24),0); Space top=new Space(this); root.addView(top,new LinearLayout.LayoutParams(1,0,1));
        TextView cal=tv("▦\n"+dayCount+" Day",54,TXT,Typeface.BOLD); cal.setGravity(Gravity.CENTER); root.addView(cal,new LinearLayout.LayoutParams(-1,-2));
        Button start=btn("Bắt đầu",GREEN); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(150),dp(150)); lp.topMargin=dp(38); start.setTextSize(22); root.addView(start,lp);
        start.setOnClickListener(v->showBodyWizard()); Space bot=new Space(this); root.addView(bot,new LinearLayout.LayoutParams(1,0,1)); }

    void showBodyWizard(){ base(); root.setGravity(Gravity.TOP); root.setPadding(dp(20),dp(40),dp(20),dp(20)); root.addView(tv("AI Coach cần chỉ số cơ bản",25,TXT,Typeface.BOLD));
        TextView sub=tv("Nhập cân nặng và chiều cao để AI phân tích bài tập thiết thực nhất cho hôm nay.",15,MUTED,Typeface.NORMAL); root.addView(sub);
        EditText weight=input("Cân nặng kg, ví dụ 58"); EditText height=input("Chiều cao cm, ví dụ 170"); root.addView(weight,new LinearLayout.LayoutParams(-1,-2)); addGap(10); root.addView(height,new LinearLayout.LayoutParams(-1,-2)); addGap(20);
        Button next=btn("Tiếp tục",GREEN); root.addView(next,new LinearLayout.LayoutParams(-1,dp(56))); next.setOnClickListener(v->{ if(weight.getText().length()==0||height.getText().length()==0){toast("Nhập đủ cân nặng và chiều cao"); return;} sp.edit().putString("weight",weight.getText().toString()).putString("height",height.getText().toString()).apply(); showMinuteWizard(); }); }

    void showMinuteWizard(){ base(); root.setGravity(Gravity.TOP); root.setPadding(dp(20),dp(40),dp(20),dp(20)); root.addView(tv("Bạn tập được bao nhiêu phút?",25,TXT,Typeface.BOLD));
        TextView sub=tv("Chọn nhanh hoặc nhập số phút. AI sẽ chia thời gian theo bài tập phù hợp.",15,MUTED,Typeface.NORMAL); root.addView(sub);
        GridLayout grid=new GridLayout(this); grid.setColumnCount(3); int[] mins={5,10,15,20,30,60}; for(int m:mins){ Button b=btn(m+" phút",CARD); b.setOnClickListener(v->showLoadingAndPlan(m)); GridLayout.LayoutParams glp=new GridLayout.LayoutParams(); glp.width=dp(105); glp.height=dp(58); glp.setMargins(dp(4),dp(6),dp(4),dp(6)); grid.addView(b,glp);} root.addView(grid);
        EditText custom=input("Nhập số phút khác"); root.addView(custom,new LinearLayout.LayoutParams(-1,-2)); addGap(14); Button go=btn("AI phân tích",GREEN); root.addView(go,new LinearLayout.LayoutParams(-1,dp(56))); go.setOnClickListener(v->{int m=parseInt(custom.getText().toString(),0); if(m<3){toast("Tối thiểu 3 phút");return;} showLoadingAndPlan(m);}); }

    void showLoadingAndPlan(int minutes){ base(); root.setGravity(Gravity.CENTER); root.setPadding(dp(22),dp(22),dp(22),dp(22)); ProgressBar pb=new ProgressBar(this); root.addView(pb); TextView lines=tv("AI đang phân tích như một chuyên gia tập luyện...",19,TXT,Typeface.BOLD); lines.setGravity(Gravity.CENTER); root.addView(lines,new LinearLayout.LayoutParams(-1,-2));
        TextView note=tv(previousAnalysisText(),15,MUTED,Typeface.NORMAL); note.setGravity(Gravity.CENTER); root.addView(note);
        new Thread(()->{ JSONObject plan=makePlan(minutes); runOnUiThread(()->{currentPlan=plan; exIndex=0; showExercise();}); }).start(); }

    String previousAnalysisText(){ if(sessions.length()==0) return "Chưa có dữ liệu hôm trước. AI sẽ bắt đầu từ chỉ số cơ thể, thời lượng tập và mục tiêu toàn thân."; try{ JSONObject last=sessions.getJSONObject(sessions.length()-1); return "Đánh giá hôm trước: "+last.optString("aiNote","Bạn đã hoàn thành. Hôm nay AI sẽ điều chỉnh để tập vừa sức hơn."); }catch(Exception e){return "AI đang đọc dữ liệu tập trước...";} }

    JSONObject makePlan(int minutes){ String fallback="{\"note\":\"Bắt đầu kiểm soát cơ thể: tập toàn thân, không đốt sức mù quáng. Hôm nay ưu tiên kỹ thuật chuẩn.\",\"exercises\":["+
            "{\"name\":\"Jumping Jack\",\"target\":\"Khởi động toàn thân\",\"seconds\":120,\"guide\":\"Đứng thẳng, bật chân rộng, tay đưa qua đầu, tiếp đất nhẹ.\",\"video\":\"jumping jack proper form\"},"+
            "{\"name\":\"Squat\",\"target\":\"Chân mông\",\"seconds\":180,\"guide\":\"Hạ hông ra sau, gối theo hướng mũi chân, lưng thẳng.\",\"video\":\"squat proper form beginner\"},"+
            "{\"name\":\"Push-up\",\"target\":\"Ngực tay vai\",\"seconds\":180,\"guide\":\"Siết bụng, thân người thành một đường, khuỷu tay không xòe quá rộng.\",\"video\":\"push up proper form\"},"+
            "{\"name\":\"Plank\",\"target\":\"Bụng core\",\"seconds\":120,\"guide\":\"Khuỷu dưới vai, mông không võng, thở đều.\",\"video\":\"plank proper form\"},"+
            "{\"name\":\"Neck and Eye Control\",\"target\":\"Cổ mặt ánh mắt\",\"seconds\":120,\"guide\":\"Giữ cổ thẳng, đảo mắt chậm, không rướn cằm, không đau thì mới tiếp tục.\",\"video\":\"neck posture eye focus exercise\"}]}";
        try{
            if(apiKey.trim().length()<8) return scalePlan(new JSONObject(fallback),minutes);
            String prompt="Bạn là AI huấn luyện viên chuyên nghiệp nghiêm khắc nhưng an toàn. Tạo giáo án toàn thân cho hôm nay theo JSON duy nhất. Người dùng cân nặng "+sp.getString("weight","")+"kg, chiều cao "+sp.getString("height","")+"cm, tập "+minutes+" phút. Lịch sử gần nhất: "+sessions.toString()+". Bắt buộc gồm chân, tay/vai/ngực hoặc lưng, bụng/core, cổ/mặt/ánh mắt nhẹ. Trả JSON: {note:string, exercises:[{name,target,seconds,guide,video}]}. Tổng seconds gần bằng "+(minutes*60)+". Không dùng markdown.";
            JSONObject body=new JSONObject().put("contents",new JSONArray().put(new JSONObject().put("parts",new JSONArray().put(new JSONObject().put("text",prompt)))));
            String res=postGemini(body); String text=extractGeminiText(res); JSONObject p=new JSONObject(cleanJson(text)); return scalePlan(p,minutes);
        }catch(Exception e){ return tryObj(fallback,minutes); }
    }
    JSONObject tryObj(String s,int m){ try{return scalePlan(new JSONObject(s),m);}catch(Exception e){return new JSONObject();} }
    JSONObject scalePlan(JSONObject p,int minutes)throws JSONException{ JSONArray arr=p.optJSONArray("exercises"); if(arr==null||arr.length()==0)return p; int total=minutes*60, sum=0; for(int i=0;i<arr.length();i++)sum+=Math.max(30,arr.getJSONObject(i).optInt("seconds",60)); for(int i=0;i<arr.length();i++){ JSONObject o=arr.getJSONObject(i); int sec=Math.max(30,o.optInt("seconds",60)*total/Math.max(1,sum)); o.put("seconds",sec);} return p; }

    void showExercise(){ base(); root.setGravity(Gravity.TOP); root.setPadding(dp(18),dp(28),dp(18),dp(14)); JSONArray arr=currentPlan.optJSONArray("exercises"); if(arr==null||exIndex>=arr.length()){ showCheckinCamera(); return; }
        JSONObject ex=arr.optJSONObject(exIndex); root.addView(tv("Bài "+(exIndex+1)+"/"+arr.length(),16,MUTED,Typeface.BOLD)); root.addView(tv(ex.optString("name","Bài tập"),31,TXT,Typeface.BOLD));
        root.addView(card("Nhóm cơ: "+ex.optString("target","Toàn thân")+"\n\nCách tập chuẩn:\n"+ex.optString("guide","Tập chậm, đúng kỹ thuật, nếu đau nhói thì dừng.")));
        TextView time=tv(formatSec(ex.optInt("seconds",60)),52,GREEN,Typeface.BOLD); time.setGravity(Gravity.CENTER); root.addView(time,new LinearLayout.LayoutParams(-1,-2));
        Button start=btn("Bắt đầu",GREEN); root.addView(start,new LinearLayout.LayoutParams(-1,dp(56))); addGap(8); Button video=btn("Video hướng dẫn cụ thể",Color.rgb(35,90,180)); root.addView(video,new LinearLayout.LayoutParams(-1,dp(54)));
        if(exIndex==arr.length()-1){ Button finish=btn("Hoàn thành",Color.rgb(255,106,0)); finish.setVisibility(View.GONE); root.addView(finish,new LinearLayout.LayoutParams(-1,dp(56))); finish.setOnClickListener(v->showCheckinCamera()); start.setOnClickListener(v->runTimer(ex.optInt("seconds",60),time,finish)); }
        else start.setOnClickListener(v->runTimer(ex.optInt("seconds",60),time,null));
        video.setOnClickListener(v->openYoutube(ex.optString("video",ex.optString("name","exercise proper form")))); }
    void runTimer(int sec,TextView time,Button finish){ if(timer!=null)timer.cancel(); timer=new CountDownTimer(sec*1000L,1000){ public void onTick(long ms){remaining=ms/1000; time.setText(formatSec((int)remaining));} public void onFinish(){ time.setText("00:00"); if(finish!=null){finish.setVisibility(View.VISIBLE); toast("Xong bài cuối. Bấm Hoàn thành.");} else {exIndex++; showExercise();}}}; timer.start(); }

    TextView card(String t){ TextView c=tv(t,16,TXT,Typeface.NORMAL); c.setBackgroundColor(CARD); c.setPadding(dp(16),dp(16),dp(16),dp(16)); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,dp(10),0,dp(12)); c.setLayoutParams(lp); return c; }
    void openYoutube(String q){ try{ Intent i=new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query="+URLEncoder.encode(q,"UTF-8"))); startActivity(i);}catch(Exception e){toast("Không mở được video");} }

    void showCheckinCamera(){ base(); root.setGravity(Gravity.CENTER); root.setPadding(dp(24),dp(24),dp(24),dp(24)); root.addView(tv("Check-in body sau buổi tập",27,TXT,Typeface.BOLD)); TextView s=tv("Chụp ảnh lưu trên máy để AI dùng làm dữ liệu phân tích cho lần sau.",15,MUTED,Typeface.NORMAL); s.setGravity(Gravity.CENTER); root.addView(s); addGap(16); Button cam=btn("Mở máy ảnh Check-in",GREEN); root.addView(cam,new LinearLayout.LayoutParams(-1,dp(58))); cam.setOnClickListener(v->capture()); }
    void capture(){ if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){ ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},9); return;} try{ File dir=new File(getFilesDir(),"checkins"); dir.mkdirs(); File f=new File(dir,"checkin_"+System.currentTimeMillis()+".jpg"); pendingPhotoPath=f.getAbsolutePath(); pendingPhotoUri=FileProvider.getUriForFile(this,getPackageName()+".provider",f); Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE); i.putExtra(MediaStore.EXTRA_OUTPUT,pendingPhotoUri); i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION); startActivityForResult(i,44);}catch(Exception e){toast("Không mở được máy ảnh: "+e.getMessage());} }
    @Override protected void onActivityResult(int r,int c,Intent d){ super.onActivityResult(r,c,d); if(r==44 && c==RESULT_OK) completeSession(); }

    void completeSession(){ int minutes=planMinutes(); String note=currentPlan!=null?currentPlan.optString("note","Hoàn thành buổi tập. Dữ liệu đã lưu để buổi sau AI điều chỉnh."):"Hoàn thành."; try{ JSONObject s=new JSONObject(); s.put("time",System.currentTimeMillis()); s.put("minutes",minutes); s.put("plus","+"+String.format(Locale.US,"%02d:00",minutes)); s.put("aiNote",note); s.put("photo",pendingPhotoPath); s.put("plan",currentPlan==null?new JSONObject():currentPlan); sessions.put(s); dayCount++; save(); }catch(Exception e){} showTearAnimation(); }
    int planMinutes(){ int sum=0; if(currentPlan!=null){ JSONArray a=currentPlan.optJSONArray("exercises"); if(a!=null)for(int i=0;i<a.length();i++)sum+=a.optJSONObject(i).optInt("seconds",0);} return Math.max(1,sum/60); }
    void showTearAnimation(){ base(); root.setGravity(Gravity.CENTER); TextView v=tv("▦",100,TXT,Typeface.BOLD); v.setGravity(Gravity.CENTER); root.addView(v); TextView plus=tv("+1 Day",34,GREEN,Typeface.BOLD); plus.setGravity(Gravity.CENTER); root.addView(plus); RotateAnimation ra=new RotateAnimation(0,18,Animation.RELATIVE_TO_SELF,.5f,Animation.RELATIVE_TO_SELF,.5f); ra.setDuration(700); ra.setRepeatMode(Animation.REVERSE); ra.setRepeatCount(1); AlphaAnimation aa=new AlphaAnimation(1f,0.15f); aa.setDuration(1200); v.startAnimation(ra); plus.startAnimation(aa); h.postDelayed(()->showMainTabs(0),1400); }

    void showMainTabs(int tab){ base(); root.setGravity(Gravity.TOP); root.setPadding(0,dp(18),0,0); LinearLayout content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(14),0,dp(14),0); root.addView(content,new LinearLayout.LayoutParams(-1,0,1)); if(tab==0)renderReport(content); else if(tab==1)renderChat(content); else renderData(content); LinearLayout nav=new LinearLayout(this); nav.setOrientation(LinearLayout.HORIZONTAL); nav.setBackgroundColor(CARD); String[] ns={"Báo cáo","Chat AI","Dữ liệu"}; for(int i=0;i<3;i++){ final int k=i; Button b=btn(ns[i],i==tab?GREEN:CARD); nav.addView(b,new LinearLayout.LayoutParams(0,dp(62),1)); b.setOnClickListener(v->showMainTabs(k)); } root.addView(nav,new LinearLayout.LayoutParams(-1,dp(68))); }
    void renderReport(LinearLayout c){ c.addView(tv(dayCount+" Day",36,TXT,Typeface.BOLD)); c.addView(tv("Lịch sử luyện tập",21,TXT,Typeface.BOLD)); ScrollView sv=new ScrollView(this); LinearLayout list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); sv.addView(list); c.addView(sv,new LinearLayout.LayoutParams(-1,0,1)); for(int i=sessions.length()-1;i>=0;i--){ try{ JSONObject s=sessions.getJSONObject(i); TextView item=card(date(s.optLong("time"))+"\n"+s.optString("plus","+00:00")+"\nAI đánh giá: "+s.optString("aiNote","")); final JSONObject fs=s; item.setOnClickListener(v->showSessionDetail(fs)); list.addView(item);}catch(Exception e){} } if(sessions.length()==0)c.addView(tv("Chưa có giao dịch tập luyện nào.",16,MUTED,Typeface.NORMAL)); }
    void showSessionDetail(JSONObject s){ base(); root.setGravity(Gravity.TOP); root.setPadding(dp(16),dp(24),dp(16),dp(16)); Button back=btn("← Quay lại",CARD); root.addView(back,new LinearLayout.LayoutParams(-1,dp(52))); back.setOnClickListener(v->showMainTabs(0)); root.addView(tv(date(s.optLong("time")),22,TXT,Typeface.BOLD)); root.addView(card(s.optString("plus","+00:00")+"\n\nĐánh giá AI:\n"+s.optString("aiNote",""))); String path=s.optString("photo",""); if(new File(path).exists()){ ImageView img=new ImageView(this); img.setScaleType(ImageView.ScaleType.CENTER_CROP); img.setImageURI(Uri.fromFile(new File(path))); root.addView(img,new LinearLayout.LayoutParams(-1,0,1)); } }
    void renderChat(LinearLayout c){ c.addView(tv("Chat AI Coach",28,TXT,Typeface.BOLD)); TextView box=card("Hỏi AI về bài tập, đau mỏi, kỹ thuật, hoặc yêu cầu phân tích buổi trước."); c.addView(box,new LinearLayout.LayoutParams(-1,0,1)); EditText msg=input("Nhập tin nhắn cho AI"); msg.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE); c.addView(msg,new LinearLayout.LayoutParams(-1,-2)); Button send=btn("Gửi",GREEN); c.addView(send,new LinearLayout.LayoutParams(-1,dp(54))); send.setOnClickListener(v->{ String m=msg.getText().toString(); if(m.trim().isEmpty())return; box.setText("Bạn: "+m+"\n\nAI đang trả lời..."); new Thread(()->{String ans=chat(m); runOnUiThread(()->box.setText("Bạn: "+m+"\n\nAI Coach:\n"+ans));}).start(); }); }
    void renderData(LinearLayout c){ c.addView(tv("Dữ liệu / API",28,TXT,Typeface.BOLD)); c.addView(tv("API key dùng cho Gemini. Dữ liệu tập và ảnh check-in lưu trên máy.",15,MUTED,Typeface.NORMAL)); EditText key=input("Dán Gemini API key"); key.setInputType(InputType.TYPE_CLASS_TEXT); key.setText(apiKey); c.addView(key,new LinearLayout.LayoutParams(-1,-2)); EditText mod=input("Model, ví dụ gemini-2.5-flash"); mod.setInputType(InputType.TYPE_CLASS_TEXT); mod.setText(model); c.addView(mod,new LinearLayout.LayoutParams(-1,-2)); Button saveB=btn("Lưu API",GREEN); c.addView(saveB,new LinearLayout.LayoutParams(-1,dp(54))); saveB.setOnClickListener(v->{apiKey=key.getText().toString().trim(); model=mod.getText().toString().trim(); save(); toast("Đã lưu API");}); Button del=btn("Xóa toàn bộ dữ liệu",Color.rgb(180,50,50)); c.addView(del,new LinearLayout.LayoutParams(-1,dp(54))); del.setOnClickListener(v->{sessions=new JSONArray(); dayCount=0; save(); showLanding();}); }

    String chat(String m){ try{ if(apiKey.length()<8)return "Chưa có Gemini API key. Vào Dữ liệu để thêm key."; String prompt="Bạn là AI coach nghiêm khắc, chuyên nghiệp, an toàn. Trả lời ngắn, thực tế. Dữ liệu: "+sessions.toString()+". Người dùng hỏi: "+m; JSONObject body=new JSONObject().put("contents",new JSONArray().put(new JSONObject().put("parts",new JSONArray().put(new JSONObject().put("text",prompt))))); return extractGeminiText(postGemini(body)); }catch(Exception e){return "Lỗi AI: "+e.getMessage();} }
    String postGemini(JSONObject body)throws Exception{ URL url=new URL("https://generativelanguage.googleapis.com/v1beta/models/"+URLEncoder.encode(model,"UTF-8")+":generateContent?key="+URLEncoder.encode(apiKey,"UTF-8")); HttpURLConnection con=(HttpURLConnection)url.openConnection(); con.setRequestMethod("POST"); con.setRequestProperty("Content-Type","application/json"); con.setConnectTimeout(30000); con.setReadTimeout(60000); con.setDoOutput(true); OutputStream os=con.getOutputStream(); os.write(body.toString().getBytes("UTF-8")); os.close(); InputStream is=con.getResponseCode()<400?con.getInputStream():con.getErrorStream(); return readAll(is); }
    String extractGeminiText(String res)throws JSONException{ JSONObject o=new JSONObject(res); return o.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).optString("text",""); }
    String cleanJson(String s){ int a=s.indexOf('{'), b=s.lastIndexOf('}'); if(a>=0&&b>a)return s.substring(a,b+1); return s; }
    String readAll(InputStream is)throws IOException{ ByteArrayOutputStream bo=new ByteArrayOutputStream(); byte[] buf=new byte[4096]; int n; while((n=is.read(buf))>0)bo.write(buf,0,n); return bo.toString("UTF-8"); }
    int parseInt(String s,int d){ try{return Integer.parseInt(s.trim());}catch(Exception e){return d;} }
    String formatSec(int s){ return String.format(Locale.US,"%02d:%02d",s/60,s%60); }
    String date(long t){ return new SimpleDateFormat("HH:mm  dd/MM/yyyy",Locale.getDefault()).format(new Date(t)); }
    void addGap(int d){ Space s=new Space(this); root.addView(s,new LinearLayout.LayoutParams(1,dp(d))); }
    void toast(String s){ Toast.makeText(this,s,Toast.LENGTH_SHORT).show(); }
}
