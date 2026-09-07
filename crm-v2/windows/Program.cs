using System;
using System.Diagnostics;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Reflection;
using System.Threading;
using System.Windows.Forms;
namespace NamiWinCRM {
 static class Program {
  static HttpListener listener;
  [STAThread] static void Main(){ Application.EnableVisualStyles(); Application.SetCompatibleTextRenderingDefault(false); try { var root=ExtractWeb(); int port=FreePort(); listener=new HttpListener(); listener.Prefixes.Add("http://127.0.0.1:"+port+"/"); listener.Start(); var th=new Thread(()=>Serve(root)){IsBackground=true}; th.Start(); Process.Start(new ProcessStartInfo("http://127.0.0.1:"+port+"/"){UseShellExecute=true}); Application.Run(new TrayContext()); } catch(Exception ex){ MessageBox.Show("NamiWin CRM اجرا نشد:\n"+ex.Message,"NamiWin CRM",MessageBoxButtons.OK,MessageBoxIcon.Error); } }
  static string ExtractWeb(){ var root=Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),"NamiWinCRM","v2"); Directory.CreateDirectory(root); WriteResource("web.index.html",Path.Combine(root,"index.html")); WriteResource("web.app.css",Path.Combine(root,"app.css")); WriteResource("web.app.js",Path.Combine(root,"app.js")); return root; }
  static void WriteResource(string name,string dst){using(var s=Assembly.GetExecutingAssembly().GetManifestResourceStream(name)){if(s==null)throw new Exception("منبع داخلی "+name+" پیدا نشد.");using(var f=File.Create(dst)){s.CopyTo(f);}}}
  static int FreePort(){var l=new TcpListener(IPAddress.Loopback,0);l.Start();int p=((IPEndPoint)l.LocalEndpoint).Port;l.Stop();return p;}
  static void Serve(string root){while(listener.IsListening){try{var c=listener.GetContext();var path=Uri.UnescapeDataString(c.Request.Url.AbsolutePath.TrimStart('/'));if(string.IsNullOrWhiteSpace(path))path="index.html";path=path.Replace('/',Path.DirectorySeparatorChar);var full=Path.GetFullPath(Path.Combine(root,path));if(!full.StartsWith(Path.GetFullPath(root),StringComparison.OrdinalIgnoreCase)||!File.Exists(full)){c.Response.StatusCode=404;c.Response.Close();continue;}var ext=Path.GetExtension(full).ToLowerInvariant();c.Response.ContentType=ext==".html"?"text/html; charset=utf-8":ext==".css"?"text/css; charset=utf-8":ext==".js"?"application/javascript; charset=utf-8":"application/octet-stream";var b=File.ReadAllBytes(full);c.Response.ContentLength64=b.Length;c.Response.OutputStream.Write(b,0,b.Length);c.Response.Close();}catch{}}}
  sealed class TrayContext:ApplicationContext { NotifyIcon tray; public TrayContext(){var menu=new ContextMenuStrip();menu.Items.Add("باز کردن NamiWin CRM",null,(s,e)=>Open());menu.Items.Add("خروج",null,(s,e)=>ExitThread());tray=new NotifyIcon{Text="NamiWin CRM v2",Icon=System.Drawing.SystemIcons.Application,Visible=true,ContextMenuStrip=menu};tray.DoubleClick+=(s,e)=>Open();} void Open(){try{var prefix=new System.Collections.Generic.List<string>(listener.Prefixes)[0];Process.Start(new ProcessStartInfo(prefix){UseShellExecute=true});}catch{}} protected override void ExitThreadCore(){try{tray.Visible=false;tray.Dispose();listener.Stop();}catch{}base.ExitThreadCore();}}
 }
}