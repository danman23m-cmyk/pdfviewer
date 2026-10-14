package com.docuview.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.BitmapFactory;
import android.graphics.pdf.PdfDocument;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.NodeList;

public class MainActivity extends Activity {
    private final int PICK_DOC=101, PICK_IMAGES=102, CREATE_PDF=103;
    private LinearLayout root, content;
    private Uri pendingUri;
    private String pendingText="";
    private PdfRenderer currentRenderer;
    private android.os.ParcelFileDescriptor currentPdfFd;
    private final int ink=Color.rgb(29,35,57), muted=Color.rgb(105,112,137), purple=Color.rgb(83,85,220), bg=Color.rgb(247,248,252);

    @Override public void onCreate(Bundle b){ super.onCreate(b); showHome(); }
    private GradientDrawable shape(int color, int radius){ GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); return d; }
    private TextView text(String s,int size,int color,boolean bold){ TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); t.setGravity(Gravity.CENTER_VERTICAL); return t; }
    private LinearLayout column(){ LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout row(){ LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private void pad(View v,int a){ v.setPadding(a,a,a,a); }
    private void base(String title,String subtitle){
        root=column(); root.setBackgroundColor(bg); root.setPadding(20,22,20,12); setContentView(root);
        TextView brand=text("DOCUVIEW  ·  DOCUMENTS",12,purple,true); root.addView(brand,new LinearLayout.LayoutParams(-1,30));
        TextView h=text(title,29,ink,true); root.addView(h,new LinearLayout.LayoutParams(-1,44));
        TextView sub=text(subtitle,14,muted,false); root.addView(sub,new LinearLayout.LayoutParams(-1,-2));
        content=column(); LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,0,1); cp.topMargin=18; root.addView(content,cp);
    }
    private Button button(String label, boolean primary, Runnable action){ Button b=new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(14); b.setTypeface(Typeface.DEFAULT,Typeface.BOLD); b.setTextColor(primary?Color.WHITE:ink); b.setBackground(shape(primary?purple:Color.WHITE,24)); b.setPadding(18,7,18,7); b.setOnClickListener(v->action.run()); return b; }
    private void card(String title,String desc,String icon,Runnable action){
        LinearLayout c=column(); c.setPadding(16,13,16,13); c.setBackground(shape(Color.WHITE,20)); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.bottomMargin=11; content.addView(c,p);
        LinearLayout r=row(); TextView i=text(icon,23,purple,true); i.setGravity(Gravity.CENTER); i.setBackground(shape(0xFFF0F0FF,15)); r.addView(i,new LinearLayout.LayoutParams(52,52)); LinearLayout tx=column(); tx.setPadding(13,0,0,0); tx.addView(text(title,16,ink,true)); TextView d=text(desc,12,muted,false); d.setPadding(0,4,0,0); tx.addView(d); r.addView(tx,new LinearLayout.LayoutParams(0,-2,1)); c.addView(r); c.setOnClickListener(v->action.run());
    }
    private void showHome(){
        base("Your files, simplified.","Read documents and convert files from one place.");
        LinearLayout hero=column(); hero.setPadding(18,18,18,18); hero.setBackground(shape(0xFFE9EAFE,22)); content.addView(hero,new LinearLayout.LayoutParams(-1,-2));
        hero.addView(text("OPEN A DOCUMENT",11,purple,true)); TextView big=text("PDF, Word, Excel & more",21,ink,true); big.setPadding(0,7,0,7); hero.addView(big); hero.addView(text("Choose a file from your phone. Your files stay on your device.",13,muted,false)); Button open=button("Browse files",true,()->pickDocument()); LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,48); bp.topMargin=14; hero.addView(open,bp);
        TextView section=text("TOOLS",12,muted,true); section.setPadding(2,17,0,9); content.addView(section);
        ScrollView sc=new ScrollView(this); LinearLayout tools=column(); sc.addView(tools); content.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout old=content; content=tools;
        card("PDF viewer","Read, zoom and move between pages","▤",()->pickDocument());
        card("Office document viewer","DOCX, XLSX, PPTX, CSV and TXT","▣",()->pickDocument());
        card("Images → PDF","Combine photos into a PDF file","▧",()->pickImages());
        card("Text → PDF","Write or paste text and export it","T",()->textToPdf());
        card("Convert to PDF","Open a supported file and export when available","⇄",()->pickDocument()); content=old;
        TextView footer=text("DOCUVIEW  •  Version 1.0",11,muted,false); footer.setGravity(Gravity.CENTER); root.addView(footer,new LinearLayout.LayoutParams(-1,28));
    }
    private void pickDocument(){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("*/*"); i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/pdf","application/msword","application/vnd.openxmlformats-officedocument.wordprocessingml.document","application/vnd.ms-excel","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","application/vnd.ms-powerpoint","application/vnd.openxmlformats-officedocument.presentationml.presentation","text/plain","text/csv","image/*"}); startActivityForResult(i,PICK_DOC); }
    private void pickImages(){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("image/*"); i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true); startActivityForResult(i,PICK_IMAGES); }
    private String nameOf(Uri u){ String n="document"; try(android.database.Cursor c=getContentResolver().query(u,null,null,null,null)){ if(c!=null&&c.moveToFirst()){int x=c.getColumnIndex(OpenableColumns.DISPLAY_NAME); if(x>=0)n=c.getString(x);} }catch(Exception ignored){} return n; }
    @Override protected void onActivityResult(int req,int res,Intent data){ super.onActivityResult(req,res,data); if(res!=RESULT_OK||data==null)return; try{
        if(req==CREATE_PDF){ Uri dest=data.getData(); if(dest!=null)writeTextPdf(dest,pendingText); return; }
        if(req==CREATE_PDF+10){ Uri dest=data.getData(); if(dest!=null)writeImagesPdf(dest,pendingImages); return; }
        if(req==PICK_IMAGES){ ArrayList<Uri> list=new ArrayList<>(); if(data.getClipData()!=null){for(int k=0;k<data.getClipData().getItemCount();k++)list.add(data.getClipData().getItemAt(k).getUri());}else if(data.getData()!=null)list.add(data.getData()); if(list.size()>0)makePdfFromImages(list); return; }
        Uri u=data.getData(); if(u==null)return; getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION); String n=nameOf(u).toLowerCase(); if(n.endsWith(".pdf"))showPdf(u); else if(n.endsWith(".docx"))showText(n,readOffice(u,"word/document.xml",false)); else if(n.endsWith(".xlsx"))showText(n,readXlsx(u)); else if(n.endsWith(".pptx"))showText(n,readSlides(u)); else if(n.endsWith(".txt")||n.endsWith(".csv")||n.endsWith(".md"))showText(n,readPlain(u)); else if(n.endsWith(".doc")||n.endsWith(".xls")||n.endsWith(".ppt"))showText(n,"This older Office format is not supported for local text extraction. Try opening it in an Office-compatible app, or save it as DOCX, XLSX or PPTX first."); else if(n.endsWith(".png")||n.endsWith(".jpg")||n.endsWith(".jpeg")||n.endsWith(".webp"))makePdfFromImages(java.util.Collections.singletonList(u)); else showText(n,"This file type is not supported by the built-in viewer yet.\n\nSupported: PDF, DOCX, XLSX, PPTX, TXT, CSV and common images.");
    }catch(Exception e){ toast("Could not open file: "+e.getMessage()); } }
    private void showText(String title,String body){ base(title,"Document preview"); ScrollView sc=new ScrollView(this); TextView t=text(body,14,ink,false); t.setGravity(Gravity.TOP); t.setPadding(16,16,16,16); t.setBackground(shape(Color.WHITE,18)); sc.addView(t); content.addView(sc,new LinearLayout.LayoutParams(-1,0,1)); content.addView(button("Back to home",false,()->showHome())); }
    private String readPlain(Uri u)throws Exception{ StringBuilder b=new StringBuilder(); try(InputStream in=getContentResolver().openInputStream(u)){byte[] buf=new byte[4096]; int n; while((n=in.read(buf))>0)b.append(new String(buf,0,n,"UTF-8"));} return b.toString(); }
    private org.w3c.dom.Document parseCurrentEntry(ZipInputStream z)throws Exception{ ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] buf=new byte[4096]; int n; while((n=z.read(buf))!=-1)out.write(buf,0,n); return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(out.toByteArray())); }
    private String readOffice(Uri u,String wanted,boolean ignored)throws Exception{ StringBuilder out=new StringBuilder(); try(ZipInputStream z=new ZipInputStream(getContentResolver().openInputStream(u))){ZipEntry e; while((e=z.getNextEntry())!=null){if(e.getName().equals(wanted)){org.w3c.dom.Document d=parseCurrentEntry(z); NodeList ps=d.getElementsByTagName("w:p"); for(int i=0;i<ps.getLength();i++){NodeList ts=((org.w3c.dom.Element)ps.item(i)).getElementsByTagName("w:t"); for(int j=0;j<ts.getLength();j++)out.append(ts.item(j).getTextContent()); out.append("\n");} break;}}} return out.length()==0?"No readable text found in this document.":out.toString(); }
    private String readSlides(Uri u)throws Exception{ StringBuilder out=new StringBuilder(); try(ZipInputStream z=new ZipInputStream(getContentResolver().openInputStream(u))){ZipEntry e; while((e=z.getNextEntry())!=null){if(e.getName().matches("ppt/slides/slide[0-9]+\\.xml")){org.w3c.dom.Document d=parseCurrentEntry(z); NodeList ts=d.getElementsByTagName("a:t"); for(int i=0;i<ts.getLength();i++)out.append(ts.item(i).getTextContent()).append(' '); out.append("\n\n");}}} return out.length()==0?"No slide text found.":out.toString(); }
    private String readXlsx(Uri u)throws Exception{ String shared=""; ArrayList<String> vals=new ArrayList<>(); StringBuilder sheets=new StringBuilder(); try(ZipInputStream z=new ZipInputStream(getContentResolver().openInputStream(u))){ZipEntry e; while((e=z.getNextEntry())!=null){if(e.getName().equals("xl/sharedStrings.xml")){org.w3c.dom.Document d=parseCurrentEntry(z); NodeList si=d.getElementsByTagName("si"); for(int i=0;i<si.getLength();i++){NodeList ts=((org.w3c.dom.Element)si.item(i)).getElementsByTagName("t"); StringBuilder s=new StringBuilder(); for(int j=0;j<ts.getLength();j++)s.append(ts.item(j).getTextContent()); vals.add(s.toString());}} else if(e.getName().matches("xl/worksheets/sheet[0-9]+\\.xml")){org.w3c.dom.Document d=parseCurrentEntry(z); NodeList rows=d.getElementsByTagName("row"); for(int i=0;i<rows.getLength();i++){NodeList cells=((org.w3c.dom.Element)rows.item(i)).getElementsByTagName("c"); for(int j=0;j<cells.getLength();j++){org.w3c.dom.Element c=(org.w3c.dom.Element)cells.item(j); NodeList vs=c.getElementsByTagName("v"); if(vs.getLength()>0){String v=vs.item(0).getTextContent(); if("s".equals(c.getAttribute("t"))){try{v=vals.get(Integer.parseInt(v));}catch(Exception ignored){}} sheets.append(v).append("\t");}} sheets.append('\n');} sheets.append("\n");}}} return sheets.length()==0?"No spreadsheet data found.":sheets.toString(); }
    private void showPdf(Uri u)throws Exception{ if(currentRenderer!=null){try{currentRenderer.close();}catch(Exception ignored){}} if(currentPdfFd!=null){try{currentPdfFd.close();}catch(Exception ignored){}} currentPdfFd=getContentResolver().openFileDescriptor(u,"r"); if(currentPdfFd==null)throw new Exception("Cannot read this PDF"); currentRenderer=new PdfRenderer(currentPdfFd); final PdfRenderer renderer=currentRenderer; final int[] page={0}; base(nameOf(u),"PDF document"); ImageView image=new ImageView(this); image.setAdjustViewBounds(true); image.setScaleType(ImageView.ScaleType.FIT_CENTER); ScrollView sc=new ScrollView(this); sc.setBackground(shape(Color.WHITE,18)); sc.addView(image); content.addView(sc,new LinearLayout.LayoutParams(-1,0,1)); LinearLayout nav=row(); nav.setPadding(0,12,0,0); Button prev=button("← Previous",false,()->{if(page[0]>0){page[0]--;renderPage(renderer,page[0],image);}}); Button next=button("Next →",true,()->{if(page[0]<renderer.getPageCount()-1){page[0]++;renderPage(renderer,page[0],image);}}); TextView count=text("",13,muted,true); count.setGravity(Gravity.CENTER); nav.addView(prev,new LinearLayout.LayoutParams(0,48,1)); nav.addView(count,new LinearLayout.LayoutParams(0,48,1)); nav.addView(next,new LinearLayout.LayoutParams(0,48,1)); content.addView(nav); content.addView(button("Back to home",false,()->showHome())); renderPage(renderer,0,image); count.setText("1 / "+renderer.getPageCount()); prev.setOnClickListener(v->{if(page[0]>0){page[0]--;renderPage(renderer,page[0],image);count.setText((page[0]+1)+" / "+renderer.getPageCount());}}); next.setOnClickListener(v->{if(page[0]<renderer.getPageCount()-1){page[0]++;renderPage(renderer,page[0],image);count.setText((page[0]+1)+" / "+renderer.getPageCount());}}); }
    private void writeTextPdf(Uri dest,String text){ PdfDocument doc=new PdfDocument(); try{ PdfDocument.PageInfo info=new PdfDocument.PageInfo.Builder(595,842,1).create(); PdfDocument.Page page=doc.startPage(info); Canvas c=page.getCanvas(); Paint p=new Paint(3); p.setColor(ink); p.setTextSize(12); int y=48; int max=85; for(String line:text.split("\\n",-1)){String remain=line; if(remain.isEmpty()){y+=18;continue;} while(remain.length()>max){c.drawText(remain.substring(0,max),40,y,p);remain=remain.substring(max);y+=18;if(y>790){doc.finishPage(page);info=new PdfDocument.PageInfo.Builder(595,842,doc.getPages().size()+1).create();page=doc.startPage(info);c=page.getCanvas();y=48;}} c.drawText(remain,40,y,p); y+=18; if(y>790){doc.finishPage(page);info=new PdfDocument.PageInfo.Builder(595,842,doc.getPages().size()+1).create();page=doc.startPage(info);c=page.getCanvas();y=48;}} doc.finishPage(page); try(OutputStream out=getContentResolver().openOutputStream(dest)){doc.writeTo(out);} toast("PDF saved successfully."); showHome(); }catch(Exception e){toast("PDF export failed: "+e.getMessage());}finally{doc.close();} }
    private void writeImagesPdf(Uri dest,List<Uri> images){ PdfDocument doc=new PdfDocument(); try{int num=1; for(Uri u:images){Bitmap b; try(InputStream in=getContentResolver().openInputStream(u)){b=BitmapFactory.decodeStream(in);} if(b==null)continue; PdfDocument.PageInfo info=new PdfDocument.PageInfo.Builder(595,842,num++).create(); PdfDocument.Page page=doc.startPage(info); Canvas c=page.getCanvas(); c.drawColor(Color.WHITE); float scale=Math.min(555f/b.getWidth(),790f/b.getHeight()); float w=b.getWidth()*scale,h=b.getHeight()*scale; float left=(595-w)/2,top=(842-h)/2; c.drawBitmap(b,null,new RectF(left,top,left+w,top+h),new Paint(3)); doc.finishPage(page); b.recycle(); } try(OutputStream out=getContentResolver().openOutputStream(dest)){doc.writeTo(out);} toast("PDF created from images."); showHome(); }catch(Exception e){toast("Image-to-PDF failed: "+e.getMessage());}finally{doc.close();} }
    private void textToPdf(){ base("Text to PDF","Create a PDF from your notes or text."); EditText input=new EditText(this); input.setHint("Type or paste your text here..."); input.setGravity(Gravity.TOP); input.setMinLines(8); input.setPadding(14,14,14,14); input.setBackground(shape(Color.WHITE,18)); content.addView(input,new LinearLayout.LayoutParams(-1,0,1)); content.addView(button("Save PDF",true,()->{String s=input.getText().toString().trim(); if(s.isEmpty()){toast("Enter some text first.");return;} pendingText=s; Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("application/pdf"); i.putExtra(Intent.EXTRA_TITLE,"DocuView-Text.pdf"); startActivityForResult(i,CREATE_PDF);})); content.addView(button("Cancel",false,()->showHome())); }
    private void makePdfFromImages(List<Uri> uris){ try{ Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("application/pdf"); i.putExtra(Intent.EXTRA_TITLE,"DocuView-Images.pdf"); pendingImages=new ArrayList<>(uris); startActivityForResult(i,CREATE_PDF+10); }catch(Exception e){toast("Could not start PDF export.");} }
    private ArrayList<Uri> pendingImages=new ArrayList<>();
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
